//
// Created by zdl on 2023/5/5.
//
#include <fstream>
#include <iostream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>
#include <unordered_set>
#include <unordered_map>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "common/safe_check.h"
#include "gtest/gtest.h"
#include "spec/feature_spec.h"
#include "spec/unit_spec.h"
#include "sub_graph.h"

using namespace std;
using namespace agl;

namespace {
std::vector<std::string> split(std::string s, std::string delimiter) {
  size_t pos_start = 0, pos_end, delim_len = delimiter.length();
  std::string token;
  std::vector<std::string> res;

  while ((pos_end = s.find(delimiter, pos_start)) != std::string::npos) {
    token = s.substr(pos_start, pos_end - pos_start);
    pos_start = pos_end + delim_len;
    res.push_back(token);
  }

  res.push_back(s.substr(pos_start));
  return res;
}

void ReadPbV2s(std::string file_name, std::vector<std::string>& root_id,
               std::vector<std::string>& graph_features, int repeated) {
  ifstream infile(file_name);
  string line;
  while (std::getline(infile, line)) {
    cout << "get line"
         << "\n";
    auto res = split(line, "\t");
    AGL_CHECK_EQUAL(res.size(), 2);
    root_id.push_back(res[0]);
    graph_features.push_back(res[1]);
  }
  infile.close();

  int size = graph_features.size();
  for (int i = 0; i < repeated; i++) {
    for (int j = 0; j < size; j++) {
      graph_features.push_back(graph_features[j]);
    }
  }
}

// just for this ut
// only support float and int64
void PrintNDArray(std::shared_ptr<NDArray>& nd) {
  int row_num = nd->GetRowNumber();
  int col_num = nd->GetColNumber();
  if (nd->GetDType() == AGLDType::FLOAT) {
    auto* f32 = nd->Flat<float>();
    for (size_t i = 0; i < row_num; ++i) {
      for (size_t j = 0; j < col_num; ++j) {
        std::cout << " " << f32[i * col_num + j];
      }
      std::cout << "\n";
    }
  } else if (nd->GetDType() == AGLDType::INT64) {
    auto* i64 = nd->Flat<int64_t>();
    for (size_t i = 0; i < row_num; ++i) {
      for (size_t j = 0; j < col_num; ++j) {
        std::cout << " " << i64[i * col_num + j];
      }
      std::cout << "\n";
    }
  }
}

void CheckValid(std::shared_ptr<NDArray>& nd, vector<float>& gt_float,
                vector<int64_t>& gt_int64, bool print_only = false) {
  int row_num = nd->GetRowNumber();
  int col_num = nd->GetColNumber();
  if (nd->GetDType() == AGLDType::FLOAT) {
    auto* f32 = nd->Flat<float>();
    for (size_t i = 0; i < row_num; ++i) {
      for (size_t j = 0; j < col_num; ++j) {
        std::cout << " our:" << f32[i * col_num + j];
        if(!print_only) AGL_CHECK_EQUAL(f32[i * col_num + j], gt_float[i * col_num + j]);
        std::cout << " gt:" << gt_float[i * col_num + j];
      }
      std::cout << "\n";
    }
  } else if (nd->GetDType() == AGLDType::INT64) {
    auto* i64 = nd->Flat<int64_t>();
    for (size_t i = 0; i < row_num; ++i) {
      for (size_t j = 0; j < col_num; ++j) {
        std::cout << " our:" << i64[i * col_num + j];
        if(!print_only) AGL_CHECK_EQUAL(i64[i * col_num + j], gt_int64[i * col_num + j]);
        std::cout << " gt:" << gt_int64[i * col_num + j];
      }
      std::cout << "\n";
    }
  }
}

void DuplicateFloatVector(vector<float>& src, vector<float>& dst) {
  dst.resize(src.size() * 2);
  std::copy(src.begin(), src.end(), dst.begin());
  std::copy(src.begin(), src.end(), dst.begin() + src.size());
}

void DuplicateInt64Vector(vector<int64_t>& src, vector<int64_t>& dst) {
  dst.resize(src.size() * 2);
  std::copy(src.begin(), src.end(), dst.begin());
  std::copy(src.begin(), src.end(), dst.begin() + src.size());
}

void CheckFrame(const shared_ptr<Frame>& fr_ptr,
                const string& e_name,
                const unordered_set<IdDType>& expected_seeds,
                const unordered_set<IdDType>& expected_next_nodes,
                const unordered_map<IdDType, vector<IdDType>>& expected_edge,
                bool print_only = false) {

  // 先 print 看一下
  // res seed nodes
  const auto& res_seeds = fr_ptr->GetSeeds();
  //std::unordered_set<IdDType> expected_res_seeds = {0, 3};
  auto& expected_res_seeds = expected_seeds;
  std::cout << "============== print seeds result ==============\n";
  for(auto& rseed: res_seeds){
    auto& rs_name = rseed.first;
    std::cout << "name:" << rs_name << "\n";
    if(!print_only) AGL_CHECK_EQUAL(expected_res_seeds.size(), rseed.second.size());
    for(auto seed_index: rseed.second) {
      auto find_seed = expected_res_seeds.find(seed_index);
      if(!print_only) AGL_CHECK(find_seed != expected_res_seeds.end());
      std::cout << " " << seed_index;
    }
    std::cout << "\n";
  }
  // res next seed nodes
  std::cout << "============== print next_nodes result ==============\n";
  const auto& res_next_nodes = fr_ptr->GetNextNodes();
  //std::unordered_set<IdDType> expected_next_seeds = {1, 4};
  auto& expected_next_seeds = expected_next_nodes;
  for(auto& rnseed: res_next_nodes){
    auto& rns_name = rnseed.first;
    std::cout << "name:" << rns_name << "\n";
    if(!print_only) AGL_CHECK_EQUAL(expected_next_seeds.size(), rnseed.second.size());
    for(auto nseed_index: rnseed.second) {
      auto find_next_nodes = expected_next_seeds.find(nseed_index);
      if(!print_only) AGL_CHECK(find_next_nodes != expected_next_seeds.end());
      std::cout << " " << nseed_index;
    }
    std::cout << "\n";
  }
  // res adj
  std::cout << "============== print coo result ==============\n";
  const auto& coo_adj = fr_ptr->GetCOOEdges();
  //unordered_map<IdDType, vector<IdDType>> expected_edges = {{0, {0, 1}},
  //                                                          {2, {3, 4}}};
  auto& expected_edges = expected_edge;
  if(!print_only)  AGL_CHECK_EQUAL(coo_adj.size(), 1); // 只有一种边
  auto find_coo = coo_adj.find(e_name);
  if(!print_only)  AGL_CHECK(find_coo != coo_adj.end());
  auto coo_adj_ptr = find_coo->second;
  auto& coo_struct_ptr = coo_adj_ptr->GetCOONDArray();
  std::cout<< "dense shape: (" << coo_struct_ptr->rows_nums_ << "," << coo_struct_ptr->col_nums_ << ")\n";
  auto& n1_indices = coo_struct_ptr->row_;
  auto& n2_indices = coo_struct_ptr->col_;
  auto& e_indices = coo_struct_ptr->data_;
  // now row number == edge num nnz
  if(!print_only)  AGL_CHECK_EQUAL(n1_indices->GetRowNumber(), n2_indices->GetRowNumber());
  if(!print_only)  AGL_CHECK_EQUAL(n1_indices->GetRowNumber(), e_indices->GetRowNumber());
  if(!print_only)  AGL_CHECK_EQUAL(n1_indices->GetRowNumber(), expected_edges.size());
  auto* n1_ptr = n1_indices->Flat<IdDType>();
  auto* n2_ptr = n2_indices->Flat<IdDType>();
  auto* e_ptr = e_indices->Flat<IdDType>();

  for(size_t iter =0; iter<n1_indices->GetRowNumber(); ++iter) {
    std::cout << "(" << n1_ptr[iter] << ", " << n2_ptr[iter] << ", "
              << e_ptr[iter] << ")\n";
    auto e_index = e_ptr[iter] ;
    auto n1_index = n1_ptr[iter];
    auto n2_index = n2_ptr[iter];
    auto find_e = expected_edges.find(e_index);
    if(!print_only)  AGL_CHECK(find_e != expected_edges.end());
    auto& exp_vec = find_e->second;
    if(!print_only)  AGL_CHECK_EQUAL(exp_vec[0], n1_index);
    if(!print_only)  AGL_CHECK_EQUAL(exp_vec[1], n2_index);
  }
}

}  // namespace

TEST(SUBGRAPH_TEST, TEST_PARSE_PB) {
  // node related spec
  string n_name = "default";
  AGLDType n_id_dtype = AGLDType::STR;
  // node dense feature
  string n_df_name = "dense";
  int n_df_dim = 3;
  AGLDType n_df_dtype = AGLDType::FLOAT;
  // node sparse kv feature
  string n_spkv_name = "nf";
  int max_dim = 10;
  AGLDType key_dtype = AGLDType::INT64;
  AGLDType val_dtype = AGLDType::INT64;
  auto n_d_spec =
      make_shared<DenseFeatureSpec>(n_df_name, n_df_dim, n_df_dtype);
  auto n_sp_spec =
      make_shared<SparseKVSpec>(n_spkv_name, max_dim, key_dtype, val_dtype);
  auto node_spec = make_shared<NodeSpec>(n_name, n_id_dtype);
  node_spec->AddDenseSpec(n_df_name, n_d_spec);
  node_spec->AddSparseKVSpec(n_spkv_name, n_sp_spec);
  // edge related spec
  string e_name = "default";
  string n1_name = "default";
  string n2_name = "default";
  AGLDType e_id_dtype = AGLDType::STR;
  // edge spkv feature
  string e_kv_name = "ef";
  int e_max_dim = 10;
  AGLDType ek_dtype = AGLDType::INT64;
  AGLDType ev_dtype = AGLDType::FLOAT;
  auto e_sp_spec =
      make_shared<SparseKVSpec>(e_kv_name, e_max_dim, ek_dtype, ev_dtype);
  auto edge_spec =
      std::make_shared<EdgeSpec>(e_name, node_spec, node_spec, e_id_dtype);
  edge_spec->AddSparseKVSpec(e_kv_name, e_sp_spec);
  auto sg_ptr = unique_ptr<SubGraph>(new SubGraph());
  sg_ptr->AddNodeSpec(n_name, node_spec);
  sg_ptr->AddEdgeSpec(e_name, edge_spec);

  // read pbs
  // vector<string> root_ids;
  // vector<string> gfs;
  // string file_name =
  // "/graph_ml/agl_ops/AGL_OpenSource/output/bin/test_sg_pbs.txt";
  // ReadPbV2s(file_name, root_ids, gfs, 0);
  // graph feature ground truth
  /**
   *
   * graphFeaturex:nodes {
key: "default"
value {
  nids {
    str {
      value: "1"
      value: "2"
      value: "3"
    }
  }
  features {
    dfs {
      key: "dense"
      value {
        dim: 3
        f32s {
          value: 0.1
          value: 1.1
          value: 1.0
          value: 0.2
          value: 2.2
          value: 2.0
          value: 0.3
          value: 3.3
          value: 3.0
        }
      }
    }
    sp_kvs {
      key: "nf"
      value {
        lens {
          value: 2
          value: 3
          value: 6
        }
        keys {
          value: 1
          value: 10
          value: 2
          value: 3
          value: 4
          value: 10
        }
        i64s {
          value: 1
          value: 1
          value: 2
          value: 3
          value: 3
          value: 3
        }
      }
    }
  }
}
}
edges {
key: "default"
value {
  eids {
    str {
      value: "1-2"
      value: "2-3"
    }
  }
  csr {
    indptr {
      value: 1
      value: 2
      value: 2
    }
    nbrs_indices {
      value: 1
      value: 2
    }
  }
  n1_name: "default"
  n2_name: "default"
  features {
    sp_kvs {
      key: "ef"
      value {
        lens {
          value: 3
          value: 6
        }
        keys {
          value: 1
          value: 2
          value: 9
          value: 2
          value: 3
          value: 10
        }
        f32s {
          value: 1.1
          value: 2.2
          value: 10.1
          value: 2.2
          value: 3.3
          value: 2.1
        }
      }
    }
  }
}
}
root {
nidx {
  name: "default"
}
}
   */

  vector<string> gfs = {
      "CnIKB2RlZmF1bHQSZwoLCgkKATEKATIKATMSWAozCgVkZW5zZRIqCAMSJgokzczMPc3MjD8A"
      "AIA/"
      "zcxMPs3MDEAAAABAmpmZPjMzU0AAAEBAEiEKAm5mEhsKBQoDAgMGEggKBgEKAgMECioICgYB"
      "AQIDAwMScAoHZGVmYXVsdBJlCgwKCgoDMS0yCgMyLTMSDQoFCgMBAgISBAoCAQIiB2RlZmF1"
      "bHQqB2RlZmF1bHQyNBIyCgJlZhIsCgQKAgMGEggKBgECCQIDChoaChjNzIw/"
      "zcwMQJqZIUHNzAxAMzNTQGZmBkAaCwoJEgdkZWZhdWx0"};
  gfs.push_back(gfs[0]);
  vector<const char*> gfs_char;
  vector<size_t> gfs_size;
  gfs_char.push_back(gfs[0].c_str());
  gfs_size.push_back(gfs[0].size());
  gfs_char.push_back(gfs[1].c_str());
  gfs_size.push_back(gfs[1].size());

  sg_ptr->CreateFromPB(gfs_char,gfs_size, false, false);
  //======================= ground truth =====================
  int node_num_gt = 3;  // one gf
  int node_dense_dim_gt = 3;
  vector<float> node_dense_gt = {0.1, 1.1, 1,   0.2, 2.2,
                                 2,   0.3, 3.3, 3};                  // onde gf
  vector<int64_t> node_spkv_ind_gt = {2, 3, 6};                      // one gf
  vector<int64_t> node_spkv_ind_gt_two_gf = {0, 2, 3, 6, 8, 9, 12};     // two gf
  vector<int64_t> node_spkv_key_gt = {1, 10, 2, 3, 4, 10};           // one gf
  vector<int64_t> node_spkv_val_gt = {1, 1, 2, 3, 3, 3};             // one gf
  int edge_num_gt = 2;                                               // one gf
  vector<int64_t> edge_spkv_ind_gt = {3, 6};                         // onde gf
  vector<int64_t> edge_spkv_ind_gt_two_gf = {0, 3, 6, 9, 12};           // two gf
  vector<int64_t> edge_spkv_key_gt = {1, 2, 9, 2, 3, 10};            // one gf
  vector<float> edge_spkv_val_gt = {1.1, 2.2, 10.1, 2.2, 3.3, 2.1};  // one gf
  // adj
  vector<int64_t> edge_adj_ind_gt = {0, 1, 2, 2, 3, 4, 4};   // two gf
  vector<int64_t> edge_adj_n2_indices_gt = {1, 2, 4, 5};  // two gf

  // step 1: test node related info
  // step 1.1: node dense feature
  auto n_df_array = sg_ptr->GetNodeDenseFeatureArray(n_name, n_df_name);
  auto n_df_nd = n_df_array->GetFeatureArray();
  AGL_CHECK(n_df_nd != nullptr);
  // check node num
  AGL_CHECK_EQUAL((n_df_nd->GetRowNumber()), (node_num_gt + node_num_gt));
  // check node dense dim
  AGL_CHECK_EQUAL((n_df_nd->GetColNumber()), node_dense_dim_gt);

  auto* f32 = n_df_nd->Flat<float>();
  std::cout << "==========node dense==========="
            << "\n";
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    DuplicateFloatVector(node_dense_gt, float_real_gt);
    CheckValid(n_df_nd, float_real_gt, int64_real_gt);
  }

  // step 1.2: node sparse kv feature
  auto n_spkv_array = sg_ptr->GetNodeSparseKVArray(n_name, n_spkv_name);
  auto& n_kv_nd = n_spkv_array->GetFeatureArray();
  // check node spkv indptr
  std::cout << "==========node indptr==========="
            << "\n";
  auto n_ind_ptr = n_kv_nd->ind_offset_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    int64_real_gt = node_spkv_ind_gt_two_gf;
    CheckValid(n_ind_ptr, float_real_gt, int64_real_gt);
  }

  // check node spkv keys
  std::cout << "==========node keys==========="
            << "\n";
  auto n_keys = n_kv_nd->keys_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    DuplicateInt64Vector(node_spkv_key_gt, int64_real_gt);
    CheckValid(n_keys, float_real_gt, int64_real_gt);
  }

  // check node spkv vals
  std::cout << "==========node vals==========="
            << "\n";
  auto n_vals = n_kv_nd->values_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    DuplicateInt64Vector(node_spkv_val_gt, int64_real_gt);
    CheckValid(n_vals, float_real_gt, int64_real_gt);
  }
  // step 2: test edge related info
  // step 2.1: edge sp kv feature
  auto e_spkv_array = sg_ptr->GetEdgeSparseKVArray(e_name, e_kv_name);
  auto& e_kv_nd = e_spkv_array->GetFeatureArray();

  // check edges spkv indptr
  std::cout << "==========edge spkv indptr==========="
            << "\n";
  auto e_ind_ptr = e_kv_nd->ind_offset_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    CheckValid(e_ind_ptr, float_real_gt, edge_spkv_ind_gt_two_gf );
  }
  // check edge spkv keys
  std::cout << "==========edge spkv keys==========="
            << "\n";
  auto e_keys = e_kv_nd->keys_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    DuplicateInt64Vector(edge_spkv_key_gt, int64_real_gt);
    CheckValid(e_keys, float_real_gt, int64_real_gt);
  }

  // check edge spkv vals
  std::cout << "==========edge spkv vals==========="
            << "\n";
  auto e_vals = e_kv_nd->values_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    DuplicateFloatVector(edge_spkv_val_gt, float_real_gt);
    CheckValid(e_vals, float_real_gt, int64_real_gt);
  }

  // step 2.2 edge adj matrix
  auto csr_ptr = sg_ptr->GetEdgeIndexCSR(e_name);
  auto& csr_array = csr_ptr->GetCSRNDArray();
  // check edge adj indptr

  std::cout << "==========edge adj indptr==========="
            << "\n";
  auto& adj_ind_ptr = csr_array->ind_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    CheckValid(adj_ind_ptr, float_real_gt, edge_adj_ind_gt);
  }

  // check edge adj n2 indices
  std::cout << "==========edge adj n2 indices==========="
            << "\n";
  auto& adj_n2_ind = csr_array->indices_;
  {
    vector<float> float_real_gt;
    vector<int64_t> int64_real_gt;
    CheckValid(adj_n2_ind, float_real_gt, edge_adj_n2_indices_gt);
  }

  // check topo_
  const auto& topo = sg_ptr->GetTopo();
  {
    AGL_CHECK_EQUAL(topo.size(), 1);
    for (auto& pair : topo) {
      auto& res_n1_name = pair.first;
      AGL_CHECK(res_n1_name == n1_name);
      auto& n2_edge_vec = pair.second;
      AGL_CHECK_EQUAL(n2_edge_vec.size(), 1);
      auto& res_n2_name = n2_edge_vec[0].first;
      auto& res_e_name = n2_edge_vec[0].second;
      AGL_CHECK(res_n2_name == n2_name);
      AGL_CHECK(res_e_name == e_name);
    }
  }

  // check root id
  const auto& root = sg_ptr->GetRootIds();
  {
    AGL_CHECK(root.size() == 1);  // 只有一种类型的root
    auto find_root = root.find(n1_name);
    AGL_CHECK(find_root != root.end());
    auto& root_ids = find_root->second;
    vector<int64_t> expected = {0, 3};
    AGL_CHECK_EQUAL(root_ids.size(), 2);  // 两个pb
    AGL_CHECK_EQUAL(root_ids[0][0], expected[0]);
    AGL_CHECK_EQUAL(root_ids[1][0], expected[1]);
  }

  // check node num per sample
  std::cout << "==========node num per sample==========="
            << "\n";
  const auto& n_num_per_sample = sg_ptr->GetNodeNumPerSample();
  {
    AGL_CHECK(n_num_per_sample.size() == 1);  // 只有一种类型的点
    auto find_n_num = n_num_per_sample.find(n1_name);
    AGL_CHECK(find_n_num != n_num_per_sample.end());
    auto& n_num_vec = find_n_num->second;
    vector<int> expected = {3, 3};
    std::cout << "n_num_vec[0]:" << n_num_vec[0] << ", expected:" << expected[0]
              << "; n_num_vec[1]:" << n_num_vec[1]
              << ", expected:" << expected[1] << "\n";
    AGL_CHECK_EQUAL(n_num_vec[0], expected[0]);
    AGL_CHECK_EQUAL(n_num_vec[1], expected[1]);
  }

  // check edge num per sample
  std::cout << "==========edge num per sample==========="
            << "\n";
  const auto& e_num_per_sample = sg_ptr->GetEdgeNumPerSample();
  {
    AGL_CHECK(e_num_per_sample.size() == 1);  // 只有一种类型的边
    auto find_e_num = e_num_per_sample.find(e_name);
    AGL_CHECK(find_e_num != e_num_per_sample.end());
    auto& e_num_vec = find_e_num->second;
    vector<int> expected = {2, 2};
    std::cout << "e_num_vec[0]:" << e_num_vec[0] << ", expected:" << expected[0]
              << "; e_num_vec[1]:" << e_num_vec[1]
              << ", expected:" << expected[1] << "\n";
    AGL_CHECK_EQUAL(e_num_vec[0], expected[0]);
    AGL_CHECK_EQUAL(e_num_vec[1], expected[1]);
  }
  // check ego graph
  {
    auto find_root = root.find(n1_name);
    AGL_CHECK(find_root != root.end());
    auto& root_ids = find_root->second;
    unordered_map<std::string, std::vector<IdDType>> seeds;
    auto& seed_vec = seeds[n1_name];
    for (auto& roots_t : root_ids) {
      for (auto& r : roots_t) {
        seed_vec.push_back(r);
      }
    }

    {
      auto frame_ptr = sg_ptr->GetEdgeIndexOneHop(seeds, false);
      std::unordered_set<IdDType> expected_res_seeds = {0, 3};
      std::unordered_set<IdDType> expected_next_seeds = {1, 4};
      unordered_map<IdDType, vector<IdDType>> expected_edges = {{0, {0, 1}},
                                                                {2, {3, 4}}};
      CheckFrame(frame_ptr, e_name, expected_res_seeds, expected_next_seeds,
                 expected_edges);
    }

    {
      auto frame_add_seed_ptr = sg_ptr->GetEdgeIndexOneHop(seeds, true);
      std::unordered_set<IdDType> expected_res_seeds = {0, 3};
      std::unordered_set<IdDType> expected_next_seeds = {0,1,3,4};
      unordered_map<IdDType, vector<IdDType>> expected_edges = {{0, {0, 1}},
                                                                {2, {3, 4}}};
      CheckFrame(frame_add_seed_ptr, e_name, expected_res_seeds, expected_next_seeds,
                 expected_edges );
    }

    {
      unordered_map<std::string, std::vector<IdDType>> seeds_2_hop;
      auto& seed_vec_2_hop = seeds_2_hop[n1_name];
      seed_vec_2_hop = {0,1, 3,4};
      std::unordered_set<IdDType> expected_res_seeds = {0,1, 3,4};
      std::unordered_set<IdDType> expected_next_seeds = {0,1,2, 3, 4,5};
      unordered_map<IdDType, vector<IdDType>> expected_edges = {
          {0, {0, 1}},
          {1, {1, 2}},
          {2, {3, 4}},
          {3, {4, 5}}};
      auto frame_ptr = sg_ptr->GetEdgeIndexOneHop(seeds_2_hop, true);
      CheckFrame(frame_ptr, e_name, expected_res_seeds, expected_next_seeds,
                 expected_edges );
    }

    {
      std::unordered_set<IdDType> expected_res_seeds_1_hop = {0, 3};
      std::unordered_set<IdDType> expected_next_seeds_1_hop = {0,1,3,4};
      unordered_map<IdDType, vector<IdDType>> expected_edges_1_hop = {{0, {0, 1}},
                                                                {2, {3, 4}}};
      std::unordered_set<IdDType> expected_res_seeds_2_hop = {0,1, 3,4};
      std::unordered_set<IdDType> expected_next_seeds_2_hop = {0,1,2, 3, 4,5};
      unordered_map<IdDType, vector<IdDType>> expected_edges_2_hop = {
          {0, {0, 1}},
          {1, {1, 2}},
          {2, {3, 4}},
          {3, {4, 5}}};

      auto frames = sg_ptr->GetEgoFrames(2, true);
      AGL_CHECK_EQUAL(frames.size(), 2);
      std::cout << ">>>>>>>>>>>> frames size:" << frames.size() << "\n";
      CheckFrame(frames[0], e_name, expected_res_seeds_1_hop, expected_next_seeds_1_hop,
                 expected_edges_1_hop );
      CheckFrame(frames[1], e_name, expected_res_seeds_2_hop, expected_next_seeds_2_hop,
                 expected_edges_2_hop );
    }
  }
}