/**
 * Copyright 2023 AntGroup CO., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
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
  // Check Rule:
  // if a feature exists in proto, we should check it with spec information.
  // if a feature does not exist in proto but appears in spec, we treat is as
  // data missing case.
  // 1. check dense feature
  for (const auto& df_pair : features.dfs()) {
    // 1.1 every dense feature in proto should be found in spec
    auto find_df = dense_spec.find(df_pair.first);
    AGL_CHECK(find_df != dense_spec.end())
        << "dense feature:" << df_pair.first << " of unit:" << unit_name
        << " in porto but not in spec";
    // 1.2 the dimension and data type of dense feature in proto should match
    // those information in spec
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
  // 2. check sparse kv feature
  // every sparse kv feature in proto should be found in spec
  for (const auto& sp_kv_pair : features.sp_kvs()) {
    auto find_sp_kv = sp_kv_spec.find(sp_kv_pair.first);
    AGL_CHECK(find_sp_kv != sp_kv_spec.end())
        << "sparse kv feature:" << sp_kv_pair.first << " of unit:" << unit_name
        << " in proto but not in spec";
    // todo should check dtype like dense feature
  }
  // 3. check sparse key feature
  // every sparse key feature in proto should be found in spec
  for (const auto& sp_k_pair : features.sp_ks()) {
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
  // Check whether the PB and spec can be matched up.
  // 1: Check Node information
  // 1.1: The number of node types in PB should be equal to or less than that in
  // node specs.
  const auto& node_map = graph_features.nodes();
  AGL_CHECK_LESS_EQUAL(node_map.size(), node_specs.size());
  for (const auto& pair : node_map) {
    // 1.2 if a kind of node (hetero type) exists in PB but can not be found in
    // specs, then we raise error.
    auto find_n_k = node_specs.find(pair.first);
    AGL_CHECK(find_n_k != node_specs.end())
        << "node name:" << pair.first << " in proto but not in node spec";
    // 1.3 Ensure that the id data type in PB matches that in spec. Otherwise,
    // raise error.
    const auto& n_spec_ptr = find_n_k->second;
    const auto& nodes_k = pair.second;
    if (nodes_k.has_nids()) {
      // Note: Currently, it is allowed to not store the original ID.
      // However, if the original ID exists in PB, we should check the id data
      // type.
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

    // 1.4:  Check node features in PB with node spec
    if (pair.second.has_features()) {
      // if node has features, then we check its feature with specs
      CheckFeatureValid(
          n_spec_ptr->GetDenseFeatureSpec(), n_spec_ptr->GetSparseKVSpec(),
          n_spec_ptr->GetSparseKSpec(), pair.second.features(), pair.first);
    }
  }
  // 2: Check edge information (in PB and edge specs)
  const auto& edge_map = graph_features.edges();
  AGL_CHECK_LESS_EQUAL(edge_map.size(), edge_specs.size());
  for (const auto& pair : edge_map) {
    // 2.1 The types of the nodes present in the PB should be found in node
    // specs. Otherwise, raise error
    auto find_n_k = edge_specs.find(pair.first);
    AGL_CHECK(find_n_k != edge_specs.end())
        << "edge name:" << pair.first << " in proto but not in edge spec";
    // 2.2 If edge id exists in PB, its data type should match that in edge
    // spec. Otherwise, raise error
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
    // 2.3 the node types for certain kind of edge should match that in edge
    // spec.
    AGL_CHECK_EQUAL(edges_k.n1_name(), n_spec_ptr->GetN1Name())
        << "edge name:" << pair.first
        << " n1 name in proto is:" << edges_k.n1_name()
        << " != n1 name in spec:" << n_spec_ptr->GetN1Name();
    AGL_CHECK_EQUAL(edges_k.n2_name(), n_spec_ptr->GetN2Name())
        << "edge name:" << pair.first
        << " n2 name in proto is:" << edges_k.n2_name()
        << " != n2 name in spec:" << n_spec_ptr->GetN2Name();
    // 1.3: Check edge features in PB with edge spec. if node has features, then
    // we check its feature with specs
    if (pair.second.has_features()) {
      CheckFeatureValid(
          n_spec_ptr->GetDenseFeatureSpec(), n_spec_ptr->GetSparseKVSpec(),
          n_spec_ptr->GetSparseKSpec(), pair.second.features(), pair.first);
    }
  }
}

void ParsePB(GraphFeature** out_pb, unique_ptr<Arena>& arena,
             const char* graph_feature_src, const size_t length,
             bool uncompress_param) {
  AGL_CHECK(graph_feature_src != nullptr)
      << "graph_feature_src should not be nullptr";
  AGL_CHECK(length > 0) << "graph_feature_src length should > 0";
  // step 1: base64 decode
  string base64_decode_data = base64_decode(graph_feature_src, length);
  AGL_CHECK(!base64_decode_data.empty())
      << "base64 decode failed:" << string(graph_feature_src, length);
  string uncompress_pb;
  // graph_feature_ptr point to base64_decode_data
  string* graph_feature_ptr = &base64_decode_data;
  // step 2: uncompress the graph feature (after base64 decode) if needed
  if (uncompress_param) {
    AGL_CHECK(uncompress(base64_decode_data, uncompress_pb))
        << "uncompressed failed:" << string(graph_feature_src, length);
    // update the pointer graph_feature_ptr
    graph_feature_ptr = &uncompress_pb;
  }
  // step 3: parse the graph feature
  ArenaOptions option;
  option.start_block_size = graph_feature_ptr->size() * 2;
  arena.reset(new Arena(option));
  *out_pb = Arena::CreateMessage<GraphFeature>(arena.get());
  AGL_CHECK((*out_pb)->ParseFromString(*graph_feature_ptr))
      << "PB parse failed:" << string(graph_feature_src, length);
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
  // dense feature initializer
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

  // sparse kv feature initializer
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

  // sparse k feature initializer
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
  // step 1: node/edge feature initializer
  vector<shared_ptr<FeatureArrayIniter>>
      initers;  // hold the life cycle of initializer
  for (auto& n_item : nodes) {
    auto& n_ptr = n_item.second;
    // compute node offset, include node id, dense feature, sparse kv/k feature
    // offset.
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
    // compute edge offset, include edge id, dense feature, sparse kv/k feature
    // offset.
    e_ptr->ComputeOffset();
    auto spec_find = edge_specs.find(e_item.first);
    AGL_CHECK(spec_find != edge_specs.end());
    auto& e_spec = spec_find->second;
    int total_element_count =
        e_ptr->element_num_[e_ptr->element_num_.size() - 1];
    AddFeatureIniterToTheadPool(
        e_spec->GetDenseFeatureSpec(), e_spec->GetSparseKVSpec(),
        e_spec->GetSparseKSpec(), e_ptr->dense_count_, e_ptr->sparse_kv_count_,
        e_ptr->sparse_k_count_, e_ptr->d_f_array_, e_ptr->spkv_f_array_,
        e_ptr->spk_f_array_, total_element_count, initers);
  }
  ThreadPool pool(RUNNUMBER);  // todo hard code $RUNNUMBER thread
  for (auto& b_ptr : initers) {
    pool.AddTask([&b_ptr]() { b_ptr->Init(); });
  }
  pool.CloseAndJoin();

  // step 2: adjacency matrix initializer
  // The calculation of adjacency matrix depends on the completion of offset
  // calculations, especially the offsets for n1 and n2.
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
  ThreadPool adj_pool(RUNNUMBER);
  for (auto& adj_b_ptr : adj_initers) {
    adj_pool.AddTask([&adj_b_ptr]() { adj_b_ptr->Init(); });
  }
  adj_pool.CloseAndJoin();
}

void CreateAddFillerToPool(
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
  // Dense feature filler
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
  // sparse kv feature filler
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
  // sparse key feature filler
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
    vector<GraphFeature*>& graph_features,
    const unordered_map<string, shared_ptr<NodeSpec>>& node_specs,
    const unordered_map<string, shared_ptr<EdgeSpec>>& edge_specs) {
  vector<shared_ptr<Filler>> fillers;  // hold the lifecycle of fillers
  for (size_t i = 0; i < graph_features.size(); ++i) {
    auto* gf = graph_features[i];
    // step 1: node related features
    auto* nodes_pb = gf->mutable_nodes();
    for (auto& n_pair : *nodes_pb) {
      auto& n_name = n_pair.first;
      auto& node_content = n_pair.second;
      auto* feat_ptr = node_content.mutable_features();
      // find NonMergeNodeInfo container with corresponding name
      auto find_static_t = nodes.find(n_name);
      AGL_CHECK(find_static_t != nodes.end());
      auto node_static_t = find_static_t->second;
      // find NodeSpec with corresponding name
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
      // find NonMergeEdgeInfo with corresponding name
      auto find_static_t = edges.find(e_name);
      AGL_CHECK(find_static_t != edges.end());
      auto e_static_t = find_static_t->second;
      // find EdgeSpec with corresponding name
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
      // There is no dependency when filling adj.
      auto& edge_offset_vec = e_static_t->element_num_;
      int edge_position_offset = i == 0 ? 0 : edge_offset_vec[i - 1];
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
  ThreadPool pool(RUNNUMBER);  // todo hard code RUNNUMBER thread
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
  if (root_info.has_subgraph()) {
    //  todo(zdl) only support node as root now.
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
    // ensure number of sample should be equal
    AGL_CHECK_EQUAL(root_all.size(), node_info->element_num_.size());
    for (size_t i = 0; i < root_all.size(); ++i) {
      // root id within a same sample, apply the same offsets (disjoint merge)
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
  // number of pbs, also can be treated as sample num
  int pb_nums = pbs.size();
  // graph feature pointers after parsing pb strings into memory
  vector<GraphFeature*> graph_features(pb_nums);
  // use unique_ptr of Arena to manage the lifetime of those graph feature
  // pointers.
  vector<unique_ptr<Arena>> batch_arena(pb_nums);
  // statistic information (container) for node and edge units in disjoint merge
  // case
  unordered_map<std::string, shared_ptr<NonMergeNodeInfo>> nodes_statistic_info;
  unordered_map<std::string, shared_ptr<NonMergeEdgeInfo>> edges_statistic_info;
  // initialize containers above according to node/edge specs information
  CreateNodeAndEdgeInfoContainer(pb_nums, node_specs_, edge_specs_,
                                 nodes_statistic_info, edges_statistic_info,
                                 this->root_ids_);
  std::atomic<int32_t> sample_index(0);
  std::atomic<bool> status_flag(true);
  // step 1: parse pb strings and put statistic information into containers
  // above the statistic information include: node/edge nums, feature length
  // (dense, sparse kv, sparse key) for each node/edge
  auto countNodeAndEdge = [this, &pbs, &pb_length, &graph_features,
                           &batch_arena, &nodes_statistic_info,
                           &edges_statistic_info, &uncompress, &pb_nums,
                           &sample_index,
                           &status_flag](int64_t start, int64_t end) {
    const auto& node_specs = this->node_specs_;
    const auto& edge_specs = this->edge_specs_;
    try {
      while (status_flag) {
        int i = sample_index.fetch_add(1);
        if (i >= pb_nums) {
          break;
        }
        // step 1.1: parse pb from string to pb structure
        GraphFeature* graph_feature_i = nullptr;
        ParsePB(&graph_feature_i, batch_arena[i], pbs[i], pb_length[i],
                uncompress);
        CheckUnitValid(node_specs, edge_specs, *graph_feature_i);
        graph_features[i] = graph_feature_i;

        // step 1.2: put node related information into nodes_statistic_info
        for (const auto& node_t : graph_feature_i->nodes()) {
          const auto& node_name_t = node_t.first;
          const auto& node_info_in_pb_t = node_t.second;
          const auto& node_statistic_info_t = nodes_statistic_info[node_name_t];
          auto find_spec = node_specs.find(node_name_t);
          const auto& node_spec_t = find_spec->second;
          // step 1.2.1: node num
          node_statistic_info_t->SetElementNum(
              node_info_in_pb_t.nids(), node_spec_t->GetNodeIdDtype(), i);
          // step 1.2.2: feature total length
          node_statistic_info_t->SetFeatureCount(
              node_info_in_pb_t.features(), node_spec_t->GetDenseFeatureSpec(),
              node_spec_t->GetSparseKVSpec(), node_spec_t->GetSparseKSpec(), i);
        }

        // step 1.3: put edge related information into edges_statistic_info
        for (const auto& edge_t : graph_feature_i->edges()) {
          const auto& edge_name_t = edge_t.first;
          const auto& e_info_in_pb_t = edge_t.second;
          const auto& edge_statistic_info_t = edges_statistic_info[edge_name_t];
          auto find_spec = edge_specs.find(edge_name_t);
          const auto& edge_spec_t = find_spec->second;
          // step 1.3.1: edge_num
          edge_statistic_info_t->SetElementNum(e_info_in_pb_t,
                                               edge_spec_t->GetEidDtype(), i);
          // step 1.3.2: feature total length
          edge_statistic_info_t->SetFeatureCount(
              e_info_in_pb_t.features(), edge_spec_t->GetDenseFeatureSpec(),
              edge_spec_t->GetSparseKVSpec(), edge_spec_t->GetSparseKSpec(), i);
        }
        // step 1.4: root node information
        ParseRootInfo(graph_feature_i->root(), this->root_ids_, i);
      }
    } catch (std::exception& e) {
      status_flag = false;
      // todo log
    } catch (...) {
      status_flag = false;
    }
  };
  // todo cpu cors
  agl::Shard(countNodeAndEdge, std::max(pb_nums, 1),
             std::max(long(1), long(pb_nums)), 1);
  AGL_CHECK(status_flag)
      << "Error occur in countNodeAndEdge func, see error above";

  // step 2: Calculate the offsets and conduct memory allocation based on
  // statistical information step 2.1: Before calculating the offsets. fill
  // node/edge num per sample into container.
  FillNodeEdgeNumPerSample(nodes_statistic_info, edges_statistic_info,
                           n_num_per_sample_, e_num_per_sample_);
  // step 2.2: Compute the offsets for each node ID and edge ID in the protobuf
  // (PB). Implicitly perform ID mapping to fill the adjacency matrix.
  // Additionally, calculate the offsets and total length of each feature for
  // convenient allocation of the corresponding containers.
  ComputeOffsetAndAllocateMemory(nodes_statistic_info, edges_statistic_info,
                                 this->node_specs_, this->edge_specs_);
  // step 3:
  // copy feature from pb to corresponding containers according to the offset
  // information
  CopyFeatureFromPBToFeatureArray(nodes_statistic_info, edges_statistic_info,
                                  graph_features, this->node_specs_,
                                  this->edge_specs_);

  // step 4: fill node/edge units according to
  // nodes_static_info/edges_statistic_info
  InitNodeAndEdgeUnitByNoMergeContainer(
      nodes_statistic_info, edges_statistic_info, this->nodes_, this->edges_);

  // step 5: apply offset to root id, to get real root id index
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