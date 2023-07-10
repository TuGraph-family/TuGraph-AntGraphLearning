#include "base_data_structure/frame.h"

#include <algorithm>
#include <atomic>
#include <memory>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "base_data_structure/coo.h"
#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "common/safe_check.h"
#include "common/thread_pool.h"

using namespace std;

namespace {
using namespace agl;
void CountEdges(vector<int>& count_res, const vector<int64_t>& seed_nodes,
                const shared_ptr<CSR>& edge_whole, int64_t row_whole,
                int64_t col_whole) {
  atomic<int> index(0);
  std::atomic<bool> status_flag(true);
  auto& offset_nd = edge_whole->ind_;
  auto* offset_ptr = offset_nd->Flat<IdDType>();

  auto countEdge = [&index, &count_res, &seed_nodes, &status_flag, offset_ptr,
                    row_whole](int64_t start, int64_t end) {
    int seed_num = seed_nodes.size();
    try {
      while (status_flag) {
        int i_begin = index.fetch_add(10);  // 每个线程计算10个偏移量
        if (i_begin > seed_num) break;
        int tmp_end = i_begin + 10;
        int i_end = tmp_end > seed_num ? seed_num : tmp_end;
        for (size_t i = i_begin; i < i_end; ++i) {
          int n_index = seed_nodes[i]; // 第一位是0
          AGL_CHECK_GREAT_THAN(row_whole, n_index);
          int offset_begin = offset_ptr[n_index];
          int offset = offset_ptr[n_index + 1] - offset_begin;
          count_res[i] = offset;
        }
      }
    } catch (std::exception& e) {
      status_flag = false;
      // todo add log
      // LOG(INFO) << e.what();
    } catch (...) {
      status_flag = false;
    }
  };
  agl::Shard(countEdge, std::max(row_whole, int64_t(1)),
             std::max(long(1), long(10)), 1);
  AGL_CHECK(status_flag) << "Error occur in countEdge func, see error above";
}

void FillCOOWithEdgeInfo(shared_ptr<NDArray>& n1_nd, shared_ptr<NDArray>& n2_nd,
                         shared_ptr<NDArray>& e_nd,
                         const vector<int64_t>& seed_nodes,
                         const shared_ptr<CSR>& edge_whole,
                         const vector<int>& dst_offset, int64_t row_whole) {
  atomic<int> index(0);
  std::atomic<bool> status_flag(true);
  // copy from  edge whole
  // indptr
  auto& offset_src_nd = edge_whole->ind_;
  auto* offset_src_ptr = offset_src_nd->Flat<IdDType>();
  // n2 indices
  auto& n2_src_nd = edge_whole->indices_;
  auto* n2_src_ptr = n2_src_nd->Flat<IdDType>();
  // todo edge index in edge_whole is nullptr, [0, nnz]

  auto* n1_nd_dst = n1_nd->Flat<IdDType>();
  auto* n2_nd_dst = n2_nd->Flat<IdDType>();
  auto* e_nd_dst = e_nd->Flat<IdDType>();
  auto fillCOO = [&index, &status_flag, &seed_nodes, &dst_offset,
                  offset_src_ptr, n2_src_ptr, n1_nd_dst, n2_nd_dst, e_nd_dst,
                  row_whole](int64_t start, int64_t end) {
    int seed_num = seed_nodes.size();
    try {
      while (status_flag) {
        int i_begin = index.fetch_add(10);  // 每个线程计算10个偏移量
        if (i_begin > seed_num) break;
        int end_tmp = i_begin + 10;
        int i_end = end_tmp > seed_num ? seed_num : end_tmp;
        for (size_t i = i_begin; i < i_end; ++i) {
          // prepare from src and seed nodes
          auto n1_id = seed_nodes[i] ; // 偏移量第一位是0
          AGL_CHECK_LESS_THAN(n1_id, row_whole);
          int64_t n2_src_begin = offset_src_ptr[n1_id];
          int64_t n2_src_end = offset_src_ptr[n1_id + 1];
          int dst_offset_begin = i == 0 ? 0 : dst_offset[i - 1];
          int dst_offset_end = dst_offset[i];
          AGL_CHECK_EQUAL(n2_src_end - n2_src_begin,
                          dst_offset_end - dst_offset_begin);
          // copy from src to dst
          for (size_t j = 0; j < n2_src_end - n2_src_begin; ++j) {
            int64_t src_index = n2_src_begin + j;  // 也是 edge index
            int dst_index = dst_offset_begin + j;
            n1_nd_dst[dst_index] = n1_id;
            n2_nd_dst[dst_index] = n2_src_ptr[src_index];
            e_nd_dst[dst_index] = src_index;
          }
        }
      }
    } catch (std::exception& e) {
      status_flag = false;
      // todo add log
      // LOG(INFO) << e.what();
    } catch (...) {
      status_flag = false;
    }
  };
  agl::Shard(fillCOO, std::max(row_whole, int64_t(1)),
             std::max(long(1), long(10)), 1);
  AGL_CHECK(status_flag) << "Error occur in countEdge func, see error above";
}

void ComputeVectorOffset(std::vector<int>& src) {
  for (size_t i = 1; i < src.size(); ++i) {
    src[i] = src[i] + src[i - 1];
  }
}

void AddUniqueElement(const vector<IdDType>& src, vector<IdDType>& dst,
                      unordered_set<IdDType>& dst_unique) {
  for (auto t : src) {
    if (dst_unique.find(t) == dst_unique.end()) {
      dst_unique.insert(t);
      dst.push_back(t);
    }
  }
}
}  // namespace

namespace agl {
///////////////////////// UnitFrame /////////////////////////
void UnitFrame::Init() {
  auto& seed_vec = *seed_nodes_;
  int total_n1_num = seed_vec.size();
  auto csr_nd_whole = edges_whole_->GetCSRNDArray();
  int64_t row_whole = csr_nd_whole->rows_nums_;
  int64_t col_whole = csr_nd_whole->col_nums_;
  vector<int> num_of_edges(total_n1_num, 0);
  // step 1: 计算和 seed nodes 相关的 edge 数目
  // todo 是否不用多线程也可以? 需要计算的内容非常简单
  CountEdges(num_of_edges, seed_vec, csr_nd_whole, row_whole, col_whole);
  // step 2: 根据num of edges 计算偏移量，方便生成 COO matrix
  ComputeVectorOffset(num_of_edges);
  // step 3: 根据 csr 进行一跳的遍历，填充当前Frame
  int total = num_of_edges[num_of_edges.size() - 1];
  auto nd_n1_ind =
      std::make_shared<NDArray>(total, 1, GetDTypeFromT<IdDType>());
  auto nd_n2_ind =
      std::make_shared<NDArray>(total, 1, GetDTypeFromT<IdDType>());
  auto nd_edge_ind =
      std::make_shared<NDArray>(total, 1, GetDTypeFromT<IdDType>());
  // step 3.1: 填充 COO matrix
  FillCOOWithEdgeInfo(nd_n1_ind, nd_n2_ind, nd_edge_ind, seed_vec, csr_nd_whole,
                      num_of_edges, row_whole);
  // step 3.2 创建COO对象
  auto coo_res = std::make_shared<COO>(row_whole, col_whole, nd_n1_ind,
                                       nd_n2_ind, nd_edge_ind);
  edges_for_seeds_ = coo_res;
  // step 3.2 对dst 节点进行去重操作
  unordered_set<int64_t> unique_set;
  n2_nodes_ = std::make_shared<std::vector<int64_t>>();
  auto& unique_vec = *n2_nodes_;
  auto* n2_index = nd_n2_ind->Flat<IdDType>();
  for (size_t i = 0; i < total; ++i) {
    auto n2_i = n2_index[i];
    if (unique_set.find(n2_i) == unique_set.end()) {
      unique_set.insert(n2_i);
      unique_vec.push_back(n2_i);
    }
  }
  // sort
  std::sort(unique_vec.begin(), unique_vec.end());
}

///////////////////////// Frame /////////////////////////
Frame::Frame(
    std::unordered_map<std::string, std::vector<IdDType>> seed_nodes,
    std::unordered_map<std::string, std::shared_ptr<UnitFrame>>& e_to_uf,
    std::unordered_map<std::string, std::vector<std::shared_ptr<UnitFrame>>>&
        n2_to_uf,
    bool add_seed_to_next) {
  seed_nodes_ = std::move(seed_nodes);
  for (auto& e_uf_pair : e_to_uf) {
    auto& e_name = e_uf_pair.first;
    auto& uf = e_uf_pair.second;
    auto& coo = uf->GetEdgeCOO();
    edges_[e_name] = std::shared_ptr<COOAdj>(new COOAdj(coo));
  }

  // todo: 生成 next_nodes 可能有性能问题：
  // (1) unique_set bucket, 用小锁 + 多线程进行加速
  unordered_set<std::string> condidate_next_name;
  for (auto& n2_uf_pair : n2_to_uf) {
    auto& n2_name = n2_uf_pair.first;
    condidate_next_name.insert(n2_name);
  }

  if (add_seed_to_next) {
    for (auto& seed_pair : seed_nodes_) {
      auto& name = seed_pair.first;
      condidate_next_name.insert(name);
    }
  }

  for (auto& n_name : condidate_next_name) {
    auto& unique_vec = next_nodes_[n_name];
    unordered_set<int64_t> unique_set;
    // 将 seed 里面的id 添加进去， 如果需要
    if (add_seed_to_next) {
      auto find_seed = seed_nodes_.find(n_name);
      if (find_seed != seed_nodes_.end()) {
        auto& s_vec = find_seed->second;
        AddUniqueElement(s_vec, unique_vec, unique_set);
      }
    }

    // 将 n2 node的id 添加进去
    auto find_n2 = n2_to_uf.find(n_name);
    if (find_n2 != n2_to_uf.end()) {
      auto& uf_ptr_vec = find_n2->second;
      for (auto& uf_ptr : uf_ptr_vec) {
        auto& n2_nodes = *(uf_ptr->GetN2Nodes());
        AddUniqueElement(n2_nodes, unique_vec, unique_set);
      }
    }
    std::sort(unique_vec.begin(), unique_vec.end());
  }
}

const std::unordered_map<std::string, std::vector<IdDType>>& Frame::GetSeeds()
    const {
  return seed_nodes_;
}

const std::unordered_map<std::string, std::vector<IdDType>>&
Frame::GetNextNodes() const {
  return next_nodes_;
}

const std::unordered_map<std::string, std::shared_ptr<COOAdj>>&
Frame::GetCOOEdges() const {
  return edges_;
}
}  // namespace agl