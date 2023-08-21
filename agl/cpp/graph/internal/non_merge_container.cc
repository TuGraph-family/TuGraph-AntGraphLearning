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
#include "internal/non_merge_container.h"

namespace {
using namespace agl;
void ResizeAccordingSpec(
    int nums,
    const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
        dense_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
        sp_kv_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
        sp_k_spec,
    std::vector<int>& element_num,
    std::unordered_map<std::string, std::vector<int>>& dense_count,
    std::unordered_map<std::string, std::vector<int>>& sp_kv_count,
    std::unordered_map<std::string, std::vector<int>>& sp_k_count,
    std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>&
        d_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>&
        spkv_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>&
        spk_f_array) {
  element_num.resize(nums);
  for (const auto& df_pair : dense_spec) {
    dense_count[df_pair.first].resize(nums);
    d_f_array[df_pair.first] = std::make_shared<DenseFeatureArray>();
  }
  for (const auto& sp_kv_pair : sp_kv_spec) {
    sp_kv_count[sp_kv_pair.first].resize(nums);
    spkv_f_array[sp_kv_pair.first] = std::make_shared<SparseKVFeatureArray>();
  }
  for (const auto& sp_k_pair : sp_k_spec) {
    sp_k_count[sp_k_pair.first].resize(nums);
    spk_f_array[sp_k_pair.first] = std::make_shared<SparseKFeatureArray>();
  }
}

int GetIdNumFromPB(const agl::proto::graph_feature::IDs& ids,
                   const AGLDType& id_dtype) {
  // Note: raw id may not be stored in protobuf, therefore, we use an optional
  // variable inum to record how many ids in this pb
  if (ids.has_inum()) {
    return ids.inum();
  } else {
    if (id_dtype == AGLDType::STR) {
      AGL_CHECK(ids.has_str());
      return ids.str().value_size();
    } else if (id_dtype == AGLDType::UINT64) {
      AGL_CHECK(ids.has_u64());
      return ids.u64().value_size();
    } else {
      AGL_CHECK(false) << "Only support STR or UINT64 id type, while now is:"
                       << id_dtype;
    }
  }
}

int GetDenseFeatureListLength(
    const agl::proto::graph_feature::Features_DenseFeatures& df,
    const std::shared_ptr<DenseFeatureSpec>& df_spec) {
  switch (df_spec->GetFeatureDtype()) {
    case AGLDType::FLOAT: {
      AGL_CHECK(df.has_f32s());
      return df.f32s().value_size();
    }
    case AGLDType::DOUBLE: {
      AGL_CHECK(df.has_f64s());
      return df.f64s().value_size();
    }
    case AGLDType::INT64: {
      AGL_CHECK(df.has_i64s());
      return df.i64s().value_size();
    }
    case AGLDType::STR: {
      AGL_CHECK(false) << "Not support dense feature with STR dtype";
    }
    default:
      AGL_CHECK(false) << "Not supported dtype:" << df_spec->GetFeatureDtype();
  }
}

void SetFeatureCountWithPBInfo(
    const agl::proto::graph_feature::Features& features,
    const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
        dense_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
        sp_kv_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
        sp_k_spec,
    int index, std::unordered_map<std::string, std::vector<int>>& dense_count,
    std::unordered_map<std::string, std::vector<int>>& sp_kv_count,
    std::unordered_map<std::string, std::vector<int>>& sp_k_count) {
  // step 1: dense
  for (auto& df_pair : features.dfs()) {
    auto& f_name = df_pair.first;
    auto& feat = df_pair.second;
    auto find_df_spec = dense_spec.find(f_name);
    auto& spec = find_df_spec->second;
    auto find_df = dense_count.find(f_name);
    auto& count = find_df->second;
    count[index] = GetDenseFeatureListLength(feat, spec);
  }
  // step 2: sparse kv
  for (auto& sp_kv_pair : features.sp_kvs()) {
    auto& f_name = sp_kv_pair.first;
    auto& feat = sp_kv_pair.second;
    auto find_sp_kv = sp_kv_count.find(f_name);
    auto& count = find_sp_kv->second;
    auto& offset = feat.lens();
    // the last element of offset means the total length
    count[index] = offset.value(offset.value_size() - 1);
  }
  // step 3: sparse k
  for (auto& sp_k_pair : features.sp_ks()) {
    auto& f_name = sp_k_pair.first;
    auto& feat = sp_k_pair.second;
    auto find_sp_k = sp_k_count.find(f_name);
    auto& count = find_sp_k->second;
    auto& offset = feat.lens();
    // the last element of offset means the total length
    count[index] = offset.value(offset.value_size() - 1);
  }
}

/**
 * transform length of each element as offset of each element
 * @param src : vector<int> length for each element
 */
void ComputeVectorOffset(std::vector<int>& src) {
  for (size_t i = 1; i < src.size(); ++i) {
    src[i] = src[i] + src[i - 1];
  }
}

/**
 * for every key(string) val(vector<int>) pair
 * transform length of each element as offset of each element
 * @param map_src : string -> vector<int>,  length for each element
 */
void CountMapOffset(
    std::unordered_map<std::string, std::vector<int>>& map_src) {
  for (auto& pair : map_src) {
    ComputeVectorOffset(pair.second);
  }
}
}  // namespace

namespace agl {

NonMergeNodeInfo::NonMergeNodeInfo(
    int pb_nums, const std::shared_ptr<NodeSpec>& my_node_spec) {
  auto dense_spec = my_node_spec->GetDenseFeatureSpec();
  auto sp_kv_spec = my_node_spec->GetSparseKVSpec();
  auto sp_k_spec = my_node_spec->GetSparseKSpec();
  // To avoid using locks, resize the container initially.
  ResizeAccordingSpec(pb_nums, dense_spec, sp_kv_spec, sp_k_spec, element_num_,
                      dense_count_, sparse_kv_count_, sparse_k_count_,
                      d_f_array_, spkv_f_array_, spk_f_array_);
}

void NonMergeNodeInfo::SetElementNum(const agl::proto::graph_feature::IDs& ids,
                                     const AGLDType& id_dtype, int index) {
  // As the container is initially resized, there is no need to use locks.
  element_num_[index] = GetIdNumFromPB(ids, id_dtype);
}

void NonMergeNodeInfo::SetFeatureCount(
    const agl::proto::graph_feature::Features& features,
    const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
        dense_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
        sp_kv_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
        sp_k_spec,
    int index) {
  SetFeatureCountWithPBInfo(features, dense_spec, sp_kv_spec, sp_k_spec, index,
                            dense_count_, sparse_kv_count_, sparse_k_count_);
}

void NonMergeNodeInfo::ComputeOffset() {
  ComputeVectorOffset(element_num_);
  CountMapOffset(dense_count_);
  CountMapOffset(sparse_kv_count_);
  CountMapOffset(sparse_k_count_);
}

NonMergeEdgeInfo::NonMergeEdgeInfo(
    int pb_nums, const std::shared_ptr<EdgeSpec>& my_edge_spec) {
  auto& dense_spec = my_edge_spec->GetDenseFeatureSpec();
  auto& sp_kv_spec = my_edge_spec->GetSparseKVSpec();
  auto& sp_k_spec = my_edge_spec->GetSparseKSpec();
  ResizeAccordingSpec(pb_nums, dense_spec, sp_kv_spec, sp_k_spec, element_num_,
                      dense_count_, sparse_kv_count_, sparse_k_count_,
                      d_f_array_, spkv_f_array_, spk_f_array_);
  adj_ = std::make_shared<CSR>();
}

void NonMergeEdgeInfo::SetElementNum(
    const agl::proto::graph_feature::Edges& edges, const AGLDType& id_dtype,
    int index) {
  if (edges.has_eids()) {
    // if protobuf has eids, directly use its length as edge num
    element_num_[index] = GetIdNumFromPB(edges.eids(), id_dtype);
  } else if (edges.has_coo()) {
    // if protobuf don't have eids but have coo edges
    // compute edge num according to n1_indices' size
    element_num_[index] = edges.coo().n1_indices().value_size();
  } else if (edges.has_csr()) {
    // if protobuf don't have eids and the edge format is CSR,
    // compute edge num according to nbrs_indices' size
    element_num_[index] = edges.csr().nbrs_indices().value_size();
  }
}

void NonMergeEdgeInfo::SetFeatureCount(
    const agl::proto::graph_feature::Features& features,
    const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
        dense_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
        sp_kv_spec,
    const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
        sp_k_spec,
    int index) {
  SetFeatureCountWithPBInfo(features, dense_spec, sp_kv_spec, sp_k_spec, index,
                            dense_count_, sparse_kv_count_, sparse_k_count_);
}

void NonMergeEdgeInfo::ComputeOffset() {
  ComputeVectorOffset(element_num_);
  CountMapOffset(dense_count_);
  CountMapOffset(sparse_kv_count_);
  CountMapOffset(sparse_k_count_);
}

}  // namespace agl