#include <google/protobuf/arena.h>

#include <atomic>
#include <memory>
#include <string>
#include <vector>

#include "base_data_structure/dtype.h"
#include "common/base64.h"
#include "common/thread_pool.h"
#include "common/uncompress.h"
#include "internal/feature_filler.h"
#include "internal/feature_initer.h"
#include "internal/non_merge_container.h"
#include "proto/graph_feature.pb.h"
#include "sub_graph.h"

using namespace agl::proto::graph_feature;
using google::protobuf::Arena;
using google::protobuf::ArenaOptions;
using std::shared_ptr;
using std::unique_ptr;
using std::unordered_map;
using std::vector;

namespace {
#define RUNNUMBER 5
using namespace agl;
using std::string;

void CheckFeatureValid(
    const unordered_map<string, shared_ptr<DenseFeatureSpec>>& dense_spec,
    const unordered_map<string, shared_ptr<SparseKVSpec>>& sp_kv_spec,
    const unordered_map<string, shared_ptr<SparseKSpec>>& sp_k_spec,
    const Features& features, const string& unit_name) {
  // todo (zdl) 目前约定如果缺失 node/edge unit, proto 里面会有一个空的 Nodes,
  // Edges 因此特征的 check 遵循以下原则：proto 中存在的必须做check, proto
  // 不存在的则作为数据缺失处理
  // 1. check dense
  for (const auto& df_pair : features.dfs()) {
    // 1.2 proto 中每种 dense 特征都必须在 spec 中找到
    auto find_df = dense_spec.find(df_pair.first);
    AGL_CHECK(find_df != dense_spec.end())
        << "dense feature:" << df_pair.first << " of unit:" << unit_name
        << " in porto but not in spec";
    // 1.3 proto 中 对应dense 的 dim 和 dtype 需要和 spec 中的match
    auto& df = df_pair.second;
    auto& spec = find_df->second;
    AGL_CHECK_EQUAL(df.dim(), spec->GetDim());
    auto df_dtype = spec->GetFeatureDtype();
    switch (df_dtype) {
      case AGLDType::FLOAT: {
        AGL_CHECK(df.has_f32s())
            << "dense feature:" << df_pair.first << " of unit:" << unit_name
            << " spec dytpe:" << df_dtype
            << " proto has_f32s should be True, but"
            << " proto has_f32s:" << df.has_f32s()
            << " has_f64s:" << df.has_f64s() << " has_i64s:" << df.has_i64s()
            << " has_raws:" << df.has_raws();
        break;
      }
      case AGLDType::DOUBLE: {
        AGL_CHECK(df.has_f64s())
            << "dense feature:" << df_pair.first << " of unit:" << unit_name
            << " spec dytpe:" << df_dtype
            << " proto has_f64s should be True, but"
            << " proto has_f32s:" << df.has_f32s()
            << " has_f64s:" << df.has_f64s() << " has_i64s:" << df.has_i64s()
            << " has_raws:" << df.has_raws();
        break;
      }
      case AGLDType::INT64: {
        AGL_CHECK(df.has_i64s())
            << "dense feature:" << df_pair.first << " of unit:" << unit_name
            << " spec dytpe:" << df_dtype
            << " proto has_i64s should be True, but"
            << " proto has_f32s:" << df.has_f32s()
            << " has_f64s:" << df.has_f64s() << " has_i64s:" << df.has_i64s()
            << " has_raws:" << df.has_raws();
        break;
      }
      case AGLDType::STR: {
        AGL_CHECK(false) << "raw feature for dense feature is not support now!";
        break;
      }
      default: {
        AGL_CHECK(false) << "Not supported dtype:" << df_dtype
                         << " for dense feature:" << df_pair.first
                         << " of unit:" << unit_name;
      }
    }
  }
  // 2 check sp kv
  // proto 中的 spkv 在 spec 中必须存在
  for (const auto& sp_kv_pair : features.sp_kvs()) {
    auto find_sp_kv = sp_kv_spec.find(sp_kv_pair.first);
    AGL_CHECK(find_sp_kv != sp_kv_spec.end())
        << "sparse kv feature:" << sp_kv_pair.first << " of unit:" << unit_name
        << " in proto but not in spec";
    // todo should check dtype like dense feature
  }
  // 3 check sp k
  for (const auto& sp_k_pair : features.sp_ks()) {
    // proto 中的 spk 在 spec 中必须存在
    auto find_sp_k = sp_k_spec.find(sp_k_pair.first);
    AGL_CHECK(find_sp_k != sp_k_spec.end())
        << "sparse key feature:" << sp_k_pair.first << " of unit:" << unit_name
        << " in proto but not in spec";
    // todo should check dtype with spec like dense feature
  }
}

void CheckUnitValid(
    const unordered_map<string, shared_ptr<NodeSpec>>& node_specs,
    const unordered_map<string, shared_ptr<EdgeSpec>>& edge_specs,
    const GraphFeature& graph_features) {
  // 检查 pb 和 spec 是否能够对应上
  // 1: pb 中的点的检查
  // 1.1： 点的类型的数目 <= node_spec.size()，
  const auto& node_map = graph_features.nodes();
  AGL_CHECK_LESS_EQUAL(node_map.size(), node_specs.size());
  for (const auto& pair : node_map) {
    // 1.2 pb中存在的点的类型在 node spec 中都能找到, 如果找不到，则 raise error
    auto find_n_k = node_specs.find(pair.first);
    AGL_CHECK(find_n_k != node_specs.end())
        << "node name:" << pair.first << " in proto but not in node spec";
    // 1.3 check 点的 id dtype 和 spec 中的一致，否则报错
    const auto& n_spec_ptr = find_n_k->second;
    const auto& nodes_k = pair.second;
    if (nodes_k.has_nids()) {
      // Note: 目前允许不存原始id
      if (n_spec_ptr->GetNodeIdDtype() == AGLDType::INT64) {
        AGL_CHECK(!nodes_k.nids().has_str())
            << "node name:" << pair.first
            << " id dtype in spec is: AGLDType::INT64, but pb has str ids";
      } else if (n_spec_ptr->GetNodeIdDtype() == AGLDType::STR) {
        AGL_CHECK(!nodes_k.nids().has_u64())
            << "node name:" << pair.first
            << " id dtype in spec is: AGLDType::STR, but pb has u64 ids";
      }
    }

    // 1.4:  pb中每种点（pb 含有的）的特征 和 node_spec 完全一致
    // const auto& n_spec_ptr = find_n_k->second;
    if (pair.second.has_features()) {
      // 如果有 feature 则需要验证
      CheckFeatureValid(
          n_spec_ptr->GetDenseFeatureSpec(), n_spec_ptr->GetSparseKVSpec(),
          n_spec_ptr->GetSparseKSpec(), pair.second.features(), pair.first);
    }
  }
  // 2: pb 中 边的检查
  const auto& edge_map = graph_features.edges();
  AGL_CHECK_LESS_EQUAL(edge_map.size(), edge_specs.size());
  for (const auto& pair : edge_map) {
    // 2.1 pb中存在的点的类型在 node spec 中都能找到, 如果找不到，则raise error
    auto find_n_k = edge_specs.find(pair.first);
    AGL_CHECK(find_n_k != edge_specs.end())
        << "edge name:" << pair.first << " in proto but not in edge spec";
    // 2.2 edge id dtype 需要和 spec 中配置的一致， 否则raise error
    const auto& n_spec_ptr = find_n_k->second;
    const auto& edges_k = pair.second;
    if (edges_k.has_eids()) {
      if (n_spec_ptr->GetEidDtype() == AGLDType::INT64) {
        AGL_CHECK(!edges_k.eids().has_str())
            << "edge name:" << pair.first
            << " id dtype in spce is AGLDType::INT64, but pb has str ids";
      } else if (n_spec_ptr->GetEidDtype() == AGLDType::STR) {
        AGL_CHECK(!edges_k.eids().has_u64())
            << "edge name:" << pair.first
            << " id dtype in spce is AGLDType::STR, but pb  has u64 ids";
      }
    }
    // 2.3 proto 中 edge 对应的两个边信息需要和spec中的一致
    AGL_CHECK_EQUAL(edges_k.n1_name(), n_spec_ptr->GetN1Name())
        << "edge name:" << pair.first
        << " n1 name in proto is:" << edges_k.n1_name()
        << " != n1 name in spec:" << n_spec_ptr->GetN1Name();
    AGL_CHECK_EQUAL(edges_k.n2_name(), n_spec_ptr->GetN2Name())
        << "edge name:" << pair.first
        << " n2 name in proto is:" << edges_k.n2_name()
        << " != n2 name in spec:" << n_spec_ptr->GetN2Name();
    // 1.3:  pb中每种边（pb 含有的）的特征 和 edge_spec 完全一致
    // const auto& n_spec_ptr = find_n_k->second;
    if (pair.second.has_features()) {
      CheckFeatureValid(
          n_spec_ptr->GetDenseFeatureSpec(), n_spec_ptr->GetSparseKVSpec(),
          n_spec_ptr->GetSparseKSpec(), pair.second.features(), pair.first);
    }
  }
}

void ParsePB(GraphFeature** out_pb, unique_ptr<Arena>& arena,
             const char* gf_src, const size_t length, bool uncompress_param) {
  // step 1: base64 decode
  string base64_decode_data = base64_decode(gf_src, length);
  AGL_CHECK(!base64_decode_data.empty())
      << "base64 decode failed:" << string(gf_src, length);
  string uncompress_pb;
  // 暂时把base64_decode_data 的指针给 gf
  string* gf = &base64_decode_data;
  // step 2: 如果需要解压，则进行解压操作
  if (uncompress_param) {
    AGL_CHECK(uncompress(base64_decode_data, uncompress_pb))
        << "uncompressed failed:" << string(gf_src, length);
    // 如果需要解压，更新这个gf 指针指向的string
    gf = &uncompress_pb;
  }
  // step 3:
  ArenaOptions option;
  option.start_block_size = gf->size() * 2;
  arena.reset(new Arena(option));
  *out_pb = Arena::CreateMessage<GraphFeature>(arena.get());
  AGL_CHECK((*out_pb)->ParseFromString(*gf))
      << "PB parse failed:" << string(gf_src, length);
}

void CreateNodeAndEdgeInfoContainer(
    int pb_nums, const unordered_map<string, shared_ptr<NodeSpec>>& node_specs,
    const unordered_map<string, shared_ptr<EdgeSpec>>& edge_specs,
    unordered_map<string, shared_ptr<NonMergeNodeInfo>>& node_info,
    unordered_map<string, shared_ptr<NonMergeEdgeInfo>>& edge_info,
    unordered_map<std::string, vector<vector<IdDType>>>& root_info) {
  for (auto& node_pair : node_specs) {
    node_info.insert({node_pair.first, std::make_shared<NonMergeNodeInfo>(
                                           pb_nums, node_pair.second)});
    root_info[node_pair.first].resize(pb_nums);
  }
  for (auto& edge_pair : edge_specs) {
    edge_info.insert({edge_pair.first, std::make_shared<NonMergeEdgeInfo>(
                                           pb_nums, edge_pair.second)});
  }
}

void AddFeatureIniterToTheadPool(
    // ThreadPool& pool,
    const unordered_map<string, shared_ptr<DenseFeatureSpec>>& dense_spec,
    const unordered_map<string, shared_ptr<SparseKVSpec>>& sp_kv_spec,
    const unordered_map<string, shared_ptr<SparseKSpec>>& sp_k_spec,
    const unordered_map<string, vector<int>>& dense_count,
    const unordered_map<string, vector<int>>& sp_kv_count,
    const unordered_map<string, vector<int>>& sp_k_count,
    std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>&
        d_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>&
        spkv_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>&
        spk_f_array,
    int element_count, vector<shared_ptr<FeatureArrayIniter>>& initers) {
  // dense
  for (auto& d_t : dense_count) {
    auto& f_name = d_t.first;
    auto& f_count_vec = d_t.second;
    int total_f_count = f_count_vec[f_count_vec.size() - 1];
    auto df_array_ptr = d_f_array[f_name];
    auto df_spec_find = dense_spec.find(f_name);
    AGL_CHECK(df_spec_find != dense_spec.end());
    auto& curr_df_spec = df_spec_find->second;
    auto builder_ptr = std::make_shared<DenseFeatureArrayIniter>(
        df_array_ptr, element_count, curr_df_spec->GetDim(),
        curr_df_spec->GetFeatureDtype());
    initers.push_back(builder_ptr);
  }

  // sparse kv
  for (auto& spkv_t : sp_kv_count) {
    auto& f_name = spkv_t.first;
    auto& f_count_vec = spkv_t.second;
    int total_f_count = f_count_vec[f_count_vec.size() - 1];
    auto spkv_array_ptr = spkv_f_array[f_name];
    auto spkv_spec_find = sp_kv_spec.find(f_name);
    AGL_CHECK(spkv_spec_find != sp_kv_spec.end());
    auto& curr_spkv_spec = spkv_spec_find->second;
    auto builder_ptr = std::make_shared<SparseKVFeatureArrayIniter>(
        spkv_array_ptr, element_count, total_f_count,
        curr_spkv_spec->GetKeyDtype(), curr_spkv_spec->GetValDtype());
    initers.push_back(builder_ptr);
  }

  // sparse k
  for (auto& spk_t : sp_k_count) {
    auto& f_name = spk_t.first;
    auto& f_count_vec = spk_t.second;
    int total_f_count = f_count_vec[f_count_vec.size() - 1];
    auto spk_array_ptr = spk_f_array[f_name];
    auto spk_spec_find = sp_k_spec.find(f_name);
    AGL_CHECK(spk_spec_find != sp_k_spec.end());
    auto& curr_spk_spec = spk_spec_find->second;
    auto builder_ptr = std::make_shared<SparseKFeatureArrayIniter>(
        spk_array_ptr, element_count, total_f_count,
        curr_spk_spec->GetKeyDtype());
    initers.push_back(builder_ptr);
  }
}

void FillNodeEdgeNumPerSample(
    const unordered_map<std::string, shared_ptr<NonMergeNodeInfo>>& nodes,
    const unordered_map<std::string, shared_ptr<NonMergeEdgeInfo>>& edges,
    std::unordered_map<std::string, std::vector<int>>& n_num_per_sample,
    std::unordered_map<std::string, std::vector<int>>& e_num_per_sample) {
  for (auto& node_t : nodes) {
    auto& name = node_t.first;
    auto& node_info = node_t.second;
    n_num_per_sample[name] = node_info->element_num_;
  }
  for (auto& edge_t : edges) {
    auto& name = edge_t.first;
    auto& edge_info = edge_t.second;
    e_num_per_sample[name] = edge_info->element_num_;
  }
}

void ComputeOffsetAndAllocateMemory(
    unordered_map<std::string, shared_ptr<NonMergeNodeInfo>>& nodes,
    unordered_map<std::string, shared_ptr<NonMergeEdgeInfo>>& edges,
    const unordered_map<string, shared_ptr<NodeSpec>>& node_specs,
    const unordered_map<string, shared_ptr<EdgeSpec>>& edge_specs) {
  vector<shared_ptr<FeatureArrayIniter>>
      initers;  // hold the life cycle of initers
  for (auto& n_item : nodes) {
    auto& n_ptr = n_item.second;
    n_ptr->ComputeOffset();
    auto spec_find = node_specs.find(n_item.first);
    AGL_CHECK(spec_find != node_specs.end());
    auto& n_spec = spec_find->second;
    int total_element_count =
        n_ptr->element_num_[n_ptr->element_num_.size() - 1];
    AddFeatureIniterToTheadPool(
        n_spec->GetDenseFeatureSpec(), n_spec->GetSparseKVSpec(),
        n_spec->GetSparseKSpec(), n_ptr->dense_count_, n_ptr->sparse_kv_count_,
        n_ptr->sparse_k_count_, n_ptr->d_f_array_, n_ptr->spkv_f_array_,
        n_ptr->spk_f_array_, total_element_count, initers);
  }

  for (auto& e_item : edges) {
    auto e_ptr = e_item.second;
    e_ptr->ComputeOffset();
    auto spec_find = edge_specs.find(e_item.first);
    AGL_CHECK(spec_find != edge_specs.end());
    auto& e_spec = spec_find->second;
    // 处理特征
    int total_element_count =
        e_ptr->element_num_[e_ptr->element_num_.size() - 1];
    AddFeatureIniterToTheadPool(
        e_spec->GetDenseFeatureSpec(), e_spec->GetSparseKVSpec(),
        e_spec->GetSparseKSpec(), e_ptr->dense_count_, e_ptr->sparse_kv_count_,
        e_ptr->sparse_k_count_, e_ptr->d_f_array_, e_ptr->spkv_f_array_,
        e_ptr->spk_f_array_, total_element_count, initers);
  }
  ThreadPool pool(RUNNUMBER);  // todo hard code 10 thread
  for (auto& b_ptr : initers) {
    pool.AddTask([&b_ptr]() { b_ptr->Init(); });
  }
  pool.CloseAndJoin();
  // 处理adj, adj 的计算依赖 offset 都计算完，特别是需要 n1, n2 的 offset
  // 都计算完， 因此，单独开一个 thead pool 进行计算
  vector<shared_ptr<FeatureArrayIniter>> adj_initers;
  for (auto& e_item : edges) {
    auto e_ptr = e_item.second;
    auto spec_find = edge_specs.find(e_item.first);
    auto& e_spec = spec_find->second;
    // 全部边的数目
    int total_element_count =
        e_ptr->element_num_[e_ptr->element_num_.size() - 1];
    // n1 node total num
    auto& n1_name = e_spec->GetN1Name();
    auto find_n1_static_info = nodes.find(n1_name);
    AGL_CHECK(find_n1_static_info != nodes.end());
    auto& n1_info = find_n1_static_info->second;
    int n1_total_num = n1_info->element_num_[n1_info->element_num_.size() - 1];
    // n2 total num
    auto& n2_name = e_spec->GetN2Name();
    auto find_n2 = nodes.find(n2_name);
    AGL_CHECK(find_n2 != nodes.end());
    auto& n2_info = find_n2->second;
    int n2_total_num = n2_info->element_num_[n2_info->element_num_.size() - 1];
    auto adj_initer = std::make_shared<CSRIniter>(
        n1_total_num, n2_total_num, total_element_count, e_ptr->adj_);
    adj_initers.push_back(adj_initer);
  }
  ThreadPool adj_pool(RUNNUMBER);  // 每种类型一个adj， 因此线程数可以不用太多
  for (auto& adj_b_ptr : adj_initers) {
    adj_pool.AddTask([&adj_b_ptr]() { adj_b_ptr->Init(); });
  }
  adj_pool.CloseAndJoin();
}

void CreateAddFillerToPool(
    // ThreadPool& pool,
    const unordered_map<string, shared_ptr<DenseFeatureSpec>>& dense_spec,
    const unordered_map<string, shared_ptr<SparseKVSpec>>& sp_kv_spec,
    const unordered_map<string, shared_ptr<SparseKSpec>>& sp_k_spec,
    vector<int>& index_offset,
    const unordered_map<string, vector<int>>& dense_count,
    const unordered_map<string, vector<int>>& sp_kv_count,
    const unordered_map<string, vector<int>>& sp_k_count,
    std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>&
        d_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>&
        spkv_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>&
        spk_f_array,
    Features* f_pb, size_t pb_order, vector<shared_ptr<Filler>>& fillers) {
  if (f_pb->dfs_size() > 0) {
    for (auto& df_pair : *(f_pb->mutable_dfs())) {
      auto& df_name = df_pair.first;
      auto& df = df_pair.second;
      // find dense spec
      auto find_spec = dense_spec.find(df_name);
      AGL_CHECK(find_spec != dense_spec.end());
      auto spec = find_spec->second;
      // find dense count
      auto find_count = dense_count.find(df_name);
      AGL_CHECK(find_count != dense_count.end());
      auto count = find_count->second;
      int offset = pb_order == 0 ? 0 : count[pb_order - 1];  // count[pb_order];
      // find df_array
      auto find_array = d_f_array.find(df_name);
      AGL_CHECK(find_array != d_f_array.end());
      auto array = find_array->second;
      auto builder_ptr = std::make_shared<DenseFeatureFiller>(
          array, offset, &df, spec->GetFeatureDtype());
      fillers.push_back(builder_ptr);
    }
  }
  if (f_pb->sp_kvs_size() > 0) {
    for (auto& spkv_pair : *(f_pb->mutable_sp_kvs())) {
      auto& name = spkv_pair.first;
      auto& sp_kv = spkv_pair.second;
      // find sp kv spec
      auto find_spec = sp_kv_spec.find(name);
      AGL_CHECK(find_spec != sp_kv_spec.end());
      auto spec = find_spec->second;
      // find spkv count
      auto find_count = sp_kv_count.find(name);
      AGL_CHECK(find_count != sp_kv_count.end());
      auto count = find_count->second;
      int f_offset =
          pb_order == 0 ? 0 : count[pb_order - 1];  // count[pb_order];
      // find spkv array
      auto find_array = spkv_f_array.find(name);
      AGL_CHECK(find_array != spkv_f_array.end());
      auto array = find_array->second;
      auto index_offset_i = pb_order == 0 ? 0 : index_offset[pb_order - 1];
      auto builder_ptr = std::make_shared<SparseKVFeatureFiller>(
          array, index_offset_i, f_offset, spec->GetMaxDim(), &sp_kv,
          spec->GetKeyDtype(), spec->GetValDtype());
      fillers.push_back(builder_ptr);
    }
  }
  if (f_pb->sp_ks_size() > 0) {
    for (auto& spk_pair : *(f_pb->mutable_sp_ks())) {
      auto& name = spk_pair.first;
      auto& spk = spk_pair.second;
      // find sp k spec
      auto find_spk = sp_k_spec.find(name);
      AGL_CHECK(find_spk != sp_k_spec.end());
      auto& spec = find_spk->second;
      // find spk count
      auto find_count = sp_k_count.find(name);
      AGL_CHECK(find_count != sp_k_count.end());
      auto count = find_count->second;
      int offset = pb_order == 0 ? 0 : count[pb_order - 1];  // count[pb_order];
      // find spk array
      auto find_array = spk_f_array.find(name);
      AGL_CHECK(find_array != spk_f_array.end());
      auto array = find_array->second;
      auto index_offset_i = pb_order == 0 ? 0 : index_offset[pb_order - 1];
      auto builder_ptr = std::make_shared<SparseKFeatureFiller>(
          array, index_offset_i, offset, spec->GetMaxDim(), &spk,
          spec->GetKeyDtype());
      fillers.push_back(builder_ptr);
    }
  }
}

void CopyFeatureFromPBToFeatureArray(
    unordered_map<std::string, shared_ptr<NonMergeNodeInfo>>& nodes,
    unordered_map<std::string, shared_ptr<NonMergeEdgeInfo>>& edges,
    vector<GraphFeature*>& gfs,
    const unordered_map<string, shared_ptr<NodeSpec>>& node_specs,
    const unordered_map<string, shared_ptr<EdgeSpec>>& edge_specs) {
  vector<shared_ptr<Filler>> fillers;  // hold the lifecycle of fillers
  for (size_t i = 0; i < gfs.size(); ++i) {
    auto* gf = gfs[i];
    // step 1: node related features
    auto* nodes_pb = gf->mutable_nodes();
    for (auto& n_pair : *nodes_pb) {
      auto& n_name = n_pair.first;
      auto& node_content = n_pair.second;
      auto* feat_ptr = node_content.mutable_features();
      // 找到 name 对应的 NonMergeNodeInfo
      auto find_static_t = nodes.find(n_name);
      AGL_CHECK(find_static_t != nodes.end());
      auto node_static_t = find_static_t->second;
      // 找到 name 对应的 NodeSpec
      auto find_spec = node_specs.find(n_name);
      AGL_CHECK(find_spec != node_specs.end());
      auto node_spec_t = find_spec->second;
      CreateAddFillerToPool(
          node_spec_t->GetDenseFeatureSpec(), node_spec_t->GetSparseKVSpec(),
          node_spec_t->GetSparseKSpec(), node_static_t->element_num_,
          node_static_t->dense_count_, node_static_t->sparse_kv_count_,
          node_static_t->sparse_k_count_, node_static_t->d_f_array_,
          node_static_t->spkv_f_array_, node_static_t->spk_f_array_, feat_ptr,
          i, fillers);
    }
    // step 2: edge related features
    auto* edges_pb = gf->mutable_edges();
    for (auto& e_pair : *edges_pb) {
      auto& e_name = e_pair.first;
      auto& e_content = e_pair.second;
      auto* feat_ptr = e_content.mutable_features();
      // 找到 name 对应的 NonMergeedgeInfo
      auto find_static_t = edges.find(e_name);
      AGL_CHECK(find_static_t != edges.end());
      auto e_static_t = find_static_t->second;
      // 找到 name 对应的 NodeSpec
      auto find_spec = edge_specs.find(e_name);
      AGL_CHECK(find_spec != edge_specs.end());
      auto e_spec_t = find_spec->second;
      CreateAddFillerToPool(
          e_spec_t->GetDenseFeatureSpec(), e_spec_t->GetSparseKVSpec(),
          e_spec_t->GetSparseKSpec(), e_static_t->element_num_,
          e_static_t->dense_count_, e_static_t->sparse_kv_count_,
          e_static_t->sparse_k_count_, e_static_t->d_f_array_,
          e_static_t->spkv_f_array_, e_static_t->spk_f_array_, feat_ptr, i,
          fillers);
      // 在填充adj 的时候，并不存在依赖关系，因此可以直接填充即可
      auto& edge_offset_vec = e_static_t->element_num_;
      int edge_position_offset =
          i == 0 ? 0 : edge_offset_vec[i - 1];  // edge_offset_vec[i];
      // n1 indices offset
      auto& n1_name = e_spec_t->GetN1Name();
      auto find_n1_static_info = nodes.find(n1_name);
      AGL_CHECK(find_n1_static_info != nodes.end());
      auto& n1_info = find_n1_static_info->second;
      int n1_offset = i == 0 ? 0 : n1_info->element_num_[i - 1];
      // n2 indices offset
      auto& n2_name = e_spec_t->GetN2Name();
      auto find_n2 = nodes.find(n2_name);
      AGL_CHECK(find_n2 != nodes.end());
      auto& n2_info = find_n2->second;
      int n2_offset = i == 0 ? 0 : n2_info->element_num_[i - 1];
      AGL_CHECK(e_content.has_csr()) << "Only support csr struct now!";

      auto builder_ptr = std::make_shared<CSRFiller>(
          e_static_t->adj_, n1_offset, edge_position_offset, n1_offset,
          n2_offset, e_content.mutable_csr());
      fillers.push_back(builder_ptr);
    }
  }
  ThreadPool pool(RUNNUMBER);  // todo hard code 10 thread
  for (auto& build_ptr : fillers) {
    pool.AddTask([&build_ptr]() { build_ptr->Fill(); });
  }
  pool.CloseAndJoin();
}

void InitNodeAndEdgeUnitByNoMergeContainer(
    const unordered_map<std::string, shared_ptr<NonMergeNodeInfo>>& nodes,
    const unordered_map<std::string, shared_ptr<NonMergeEdgeInfo>>& edges,
    std::unordered_map<std::string, std::shared_ptr<NodeUint>>& node_dst,
    std::unordered_map<std::string, std::shared_ptr<EdgeUint>>& edge_dst) {
  // init node unit with non merge node info container
  // std::cout << "call InitNodeAndEdgeUnitByNoMergeContainer, node info size:"
  // << nodes.size() << " edge info size:" << edges.size() << "\n";
  for (auto& node_t : nodes) {
    auto& name = node_t.first;
    auto& node_info = node_t.second;
    node_dst[name]->Init(node_info->d_f_array_, node_info->spkv_f_array_,
                         node_info->spk_f_array_);
  }
  // init edge unit with non merge edge info container
  for (auto& edge_t : edges) {
    auto& name = edge_t.first;
    auto& edge_info = edge_t.second;
    auto adj = std::make_shared<CSRAdj>(edge_info->adj_);
    edge_dst[name]->Init(edge_info->d_f_array_, edge_info->spkv_f_array_,
                         edge_info->spk_f_array_, adj);
  }
}

void ParseRootInfo(
    const GraphFeature_Root& root_info,
    unordered_map<std::string, vector<vector<IdDType>>>& parsed_root,
    int index) {
  // std::cout << " ==== begin parse root info" << "\n";
  if (root_info.has_subgraph()) {
    // std::cout<< "has subgraph" << "\n";
    //  todo 目前暂时只支持 node index 作为root
    AGL_CHECK(root_info.subgraph().is_node())
        << " Now not support edge index as root";
    const auto& root_map = root_info.subgraph().indices();
    for (const auto& root_t : root_map) {
      auto& name = root_t.first;
      auto& root_ind = root_t.second;
      auto find = parsed_root.find(name);
      AGL_CHECK(find != parsed_root.end())
          << " root info with node name:" << name << " not in node spec!";
      auto& vec = find->second[index];
      for (size_t i = 0; i < root_ind.value_size(); ++i) {
        vec.emplace_back(root_ind.value(i));
      }
    }
  } else if (root_info.has_nidx()) {
    const auto& name = root_info.nidx().name();
    const auto& root_ind = root_info.nidx().idx();
    // std::cout << "parse root name:" << name << " root ind:" << root_ind
    // <<"\n";
    auto find = parsed_root.find(name);
    AGL_CHECK(find != parsed_root.end())
        << " root info with node name:" << name << " not in node spec!";
    auto& vec = find->second[index];
    vec.emplace_back(root_ind);
  } else if (root_info.has_eidx()) {
    AGL_CHECK(false) << " Now not support edge index as root";
  }
}

void AddOffsetToRoots(
    const unordered_map<std::string, shared_ptr<NonMergeNodeInfo>>& nodes,
    unordered_map<std::string, vector<vector<IdDType>>>& root) {
  for (auto& root_t : root) {
    auto& name = root_t.first;
    auto& root_all = root_t.second;
    auto find = nodes.find(name);
    AGL_CHECK(find != nodes.end());
    auto& node_info = find->second;
    AGL_CHECK_EQUAL(root_all.size(),
                    node_info->element_num_.size());  // 样本数目一致
    for (size_t i = 0; i < root_all.size(); ++i) {
      // 同一个样本中的 root id, 添加相同的偏移量
      auto& vec = root_all[i];
      int offset = i == 0 ? 0 : node_info->element_num_[i - 1];
      for (size_t j = 0; j < vec.size(); ++j) {
        vec[j] += offset;
      }
    }
  }
}

}  // anonymous namespace

namespace agl {

void SubGraph::CreateFromPBNonMerge(const std::vector<const char*>& pbs,
                                    const std::vector<size_t>& pb_length,
                                    bool uncompress) {
  int pb_nums = pbs.size();  // 一般也认为是 sample num，方便与 label 对应
  // 解析后，graph feature 指针
  vector<GraphFeature*> gfs(pb_nums);
  // 管理这些 graph feature 指针的生命周期
  vector<unique_ptr<Arena>> batch_arena_(pb_nums);
  // 不融合情况下，每个pb的统计信息
  unordered_map<std::string, shared_ptr<NonMergeNodeInfo>> nodes_statistic_info;
  unordered_map<std::string, shared_ptr<NonMergeEdgeInfo>> edges_statistic_info;
  // unordered_map<std::string, vector<vector<IdDType>>> root_id_info;
  // 根据 spec 信息进行初始化上述统计信息的容器
  CreateNodeAndEdgeInfoContainer(pb_nums, node_specs_, edge_specs_,
                                 nodes_statistic_info, edges_statistic_info,
                                 this->root_ids_);
  std::atomic<int32_t> sample_index(0);
  std::atomic<bool> status_flag(true);
  // step 1: 解析 pb, 并且将每个pb的统计信息放在 临时容器中
  auto countNodeAndEdge = [this, &pbs, &pb_length, &gfs, &batch_arena_,
                           &nodes_statistic_info, &edges_statistic_info,
                           &uncompress, &pb_nums, &sample_index,
                           &status_flag](int64_t start, int64_t end) {
    const auto& node_specs = this->node_specs_;
    const auto& edge_specs = this->edge_specs_;
    try {
      while (status_flag) {
        int i = sample_index.fetch_add(1);
        if (i >= pb_nums) break;

        // step 1: parse pb from string to pb structure
        // auto* gf_i = gfs[i];.
        GraphFeature* gf_i = nullptr;
        ParsePB(&gf_i, batch_arena_[i], pbs[i], pb_length[i], uncompress);
        CheckUnitValid(node_specs, edge_specs, *gf_i);
        gfs[i] = gf_i;

        // step 2: 更新 nodes_statistic_info 中的信息，由于预先 resize 成
        // pb_nums 大小了，因此不用加锁
        for (const auto& node_t : gf_i->nodes()) {
          const auto& n_name_t = node_t.first;
          const auto& node_info_t = node_t.second;
          const auto& node_s_info_t = nodes_statistic_info[n_name_t];
          auto find_spec = node_specs.find(n_name_t);
          const auto& n_spec_t = find_spec->second;
          // step 2.1: node num
          node_s_info_t->SetElementNum(node_info_t.nids(),
                                       n_spec_t->GetNodeIdDtype(), i);
          // step 2.2: feature total length
          node_s_info_t->SetFeatureCount(
              node_info_t.features(), n_spec_t->GetDenseFeatureSpec(),
              n_spec_t->GetSparseKVSpec(), n_spec_t->GetSparseKSpec(), i);
        }

        // step 3: 更新 edge_static_info 中的信息
        for (const auto& edge_t : gf_i->edges()) {
          const auto& e_name_t = edge_t.first;
          const auto& e_info_t = edge_t.second;
          const auto& e_statistic_t = edges_statistic_info[e_name_t];
          auto find_spec = edge_specs.find(e_name_t);
          const auto& e_spec_t = find_spec->second;
          // step 3.1: edge_num
          // step 2.1: node num
          e_statistic_t->SetElementNum(e_info_t, e_spec_t->GetEidDtype(), i);
          // step 2.2: feature total length
          e_statistic_t->SetFeatureCount(
              e_info_t.features(), e_spec_t->GetDenseFeatureSpec(),
              e_spec_t->GetSparseKVSpec(), e_spec_t->GetSparseKSpec(), i);
        }
        // step 4: root node 信息
        ParseRootInfo(gf_i->root(), this->root_ids_, i);
      }
    } catch (std::exception& e) {
      status_flag = false;
      // todo add log
      // LOG(INFO) << e.what();
    } catch (...) {
      status_flag = false;
    }
  };
  // todo cpu cors
  agl::Shard(countNodeAndEdge, std::max(pb_nums, 1),
             std::max(long(1), long(pb_nums)), 1);
  AGL_CHECK(status_flag)
      << "Error occur in countNodeAndEdge func, see error above";

  // step 2: 根据统计信息计算偏移量以及最终容器的申请
  // 计算每个pb 中节点id的 offset, 边 id 的 offset, 方便进行 index
  // 的映射，以及将信息填充在临界矩阵中 计算 各个特征的offset,
  // 以及总长度，方便进行相应容器的申请
  // step 2.1: 在计算偏移量之前，填充 node/edge num per sample
  FillNodeEdgeNumPerSample(nodes_statistic_info, edges_statistic_info,
                           n_num_per_sample_, e_num_per_sample_);

  ComputeOffsetAndAllocateMemory(nodes_statistic_info, edges_statistic_info,
                                 this->node_specs_, this->edge_specs_);
  // step 3:
  // 根据偏移量信息以及最终容器的指针，将pb中的特征copy到对应的位置上
  CopyFeatureFromPBToFeatureArray(nodes_statistic_info, edges_statistic_info,
                                  gfs, this->node_specs_, this->edge_specs_);

  // step 4: 从 nodes_static_info, 以及 edges_statistic_info 中填充 node 以及
  // edge units
  InitNodeAndEdgeUnitByNoMergeContainer(
      nodes_statistic_info, edges_statistic_info, this->nodes_, this->edges_);

  // step 5: 根据nodes_statistic_info 的偏移量，对root id index 进行偏移计算
  AddOffsetToRoots(nodes_statistic_info, this->root_ids_);
}

void SubGraph::CreateFromPB(const std::vector<const char*>& pbs,
                            const std::vector<size_t>& pb_length, bool merge,
                            bool uncompress) {
  merge_ = merge;
  AGL_CHECK(!merge_) << " merge not support now!";
  if (!merge_) {
    CreateFromPBNonMerge(pbs, pb_length, uncompress);
  }
}
}  // namespace agl