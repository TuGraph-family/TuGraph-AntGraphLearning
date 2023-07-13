#ifndef AGL_NON_MERGE_CONTAINER_H
#define AGL_NON_MERGE_CONTAINER_H

#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "base_data_structure/csr.h"
#include "common/safe_check.h"
#include "features/dense_feature_array.h"
#include "features/sparsek_feature_array.h"
#include "features/sparsekv_feature_array.h"
#include "proto/graph_feature.pb.h"
#include "spec/feature_spec.h"
#include "spec/unit_spec.h"

namespace agl {

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
    std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>
        &d_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>
        &spkv_f_array,
    std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>> &spk_f_array) {
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
  // Note: 可能会不存储 raw id，因此使用 optional inum 记录一个batch 有多少 ids
  if (ids.has_inum()) {
    //std::cout << ">>>>>>>> use inum \n";
    return ids.inum();
  } else {
    if (id_dtype == AGLDType::STR) {
      AGL_CHECK(ids.has_str());
      //std::cout << ">>>>>>>> use str id \n";
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
    // todo check feat dim 和 dense spec 中的是否一致 ?
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
    // todo 是否需要spec 进行check
    auto find_sp_kv = sp_kv_count.find(f_name);
    auto& count = find_sp_kv->second;
    auto& offset = feat.lens();
    count[index] =
        offset.value(offset.value_size() - 1);  // 最后一个元素表示整体的长度
  }
  // step 3: sparse k
  for (auto& sp_k_pair : features.sp_ks()) {
    auto& f_name = sp_k_pair.first;
    auto& feat = sp_k_pair.second;
    // todo 是否需要spec 进行check
    auto find_sp_k = sp_k_count.find(f_name);
    auto& count = find_sp_k->second;
    auto& offset = feat.lens();
    count[index] =
        offset.value(offset.value_size() - 1);  // 最后一个元素表示整体的长度
  }
}

void ComputeVectorOffset(std::vector<int>& src) {
  for (size_t i = 1; i < src.size(); ++i) {
    src[i] = src[i] + src[i - 1];
  }
}

void CountMapOffset(
    std::unordered_map<std::string, std::vector<int>>& map_src) {
  for (auto& pair : map_src) {
    ComputeVectorOffset(pair.second);
  }
}

struct NonMergeNodeInfo {
  NonMergeNodeInfo(int pb_nums, const std::shared_ptr<NodeSpec>& my_node_spec) {
    auto dense_spec = my_node_spec->GetDenseFeatureSpec();
    auto sp_kv_spec = my_node_spec->GetSparseKVSpec();
    auto sp_k_spec = my_node_spec->GetSparseKSpec();
    ResizeAccordingSpec(pb_nums, dense_spec, sp_kv_spec, sp_k_spec,
                        element_num_, dense_count_, sparse_kv_count_,
                        sparse_k_count_, d_f_array_, spkv_f_array_, spk_f_array_);
  }

  void SetElementNum(const agl::proto::graph_feature::IDs& ids,
                     const AGLDType& id_dtype, int index) {
    // 根据 index 区分，无需加锁
    element_num_[index] = GetIdNumFromPB(ids, id_dtype);
  }

  void SetFeatureCount(
      const agl::proto::graph_feature::Features& features,
      const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
          dense_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
          sp_kv_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
          sp_k_spec,
      int index) {
    SetFeatureCountWithPBInfo(features, dense_spec, sp_kv_spec, sp_k_spec,
                              index, dense_count_, sparse_kv_count_,
                              sparse_k_count_);
  }

  void ComputeOffset() {
    ComputeVectorOffset(element_num_);
    CountMapOffset(dense_count_);
    CountMapOffset(sparse_kv_count_);
    CountMapOffset(sparse_k_count_);
  }

  std::vector<int> element_num_;
  std::unordered_map<std::string, std::vector<int>> dense_count_;
  std::unordered_map<std::string, std::vector<int>> sparse_kv_count_;
  std::unordered_map<std::string, std::vector<int>> sparse_k_count_;
  std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>
      d_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>
      spkv_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>
      spk_f_array_;
};

struct NonMergeEdgeInfo {
  NonMergeEdgeInfo(int pb_nums, const std::shared_ptr<EdgeSpec>& my_edge_spec) {
    auto& dense_spec = my_edge_spec->GetDenseFeatureSpec();
    auto& sp_kv_spec = my_edge_spec->GetSparseKVSpec();
    auto& sp_k_spec = my_edge_spec->GetSparseKSpec();
    ResizeAccordingSpec(pb_nums, dense_spec, sp_kv_spec, sp_k_spec,
                        element_num_, dense_count_, sparse_kv_count_,
                        sparse_k_count_, d_f_array_, spkv_f_array_, spk_f_array_);
    adj_ = std::make_shared<CSR>();
  }

  void SetElementNum(const agl::proto::graph_feature::Edges& edges,
                     const AGLDType& id_dtype, int index) {
    // 根据 index 区分，无需加锁
    if (edges.has_eids()) {
      // 如果有 eids, 直接根据 eids 的长度进行确认
      element_num_[index] = GetIdNumFromPB(edges.eids(), id_dtype);
    } else {
      if (edges.has_coo()) {
        // 如果是 coo 格式，n1_indices 和 n2_indices 都是一样的
        element_num_[index] = edges.coo().n1_indices().value_size();
      } else if (edges.has_csr()) {
        // 如果是 csr 格式，根据 nbrs_indices的长度确定，也可以根据 indptr
        // 最后一位确认
        element_num_[index] = edges.csr().nbrs_indices().value_size();
      }
    }
  }

  void SetFeatureCount(
      const agl::proto::graph_feature::Features& features,
      const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
          dense_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
          sp_kv_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
          sp_k_spec,
      int index) {
    SetFeatureCountWithPBInfo(features, dense_spec, sp_kv_spec, sp_k_spec,
                              index, dense_count_, sparse_kv_count_,
                              sparse_k_count_);
  }

  void ComputeOffset() {
    ComputeVectorOffset(element_num_);
    CountMapOffset(dense_count_);
    CountMapOffset(sparse_kv_count_);
    CountMapOffset(sparse_k_count_);
  }

  std::vector<int> element_num_;
  std::unordered_map<std::string, std::vector<int>> dense_count_;
  std::unordered_map<std::string, std::vector<int>> sparse_kv_count_;
  std::unordered_map<std::string, std::vector<int>> sparse_k_count_;

  std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>
      d_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>
      spkv_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>
      spk_f_array_;

  std::shared_ptr<CSR> adj_;
};
}  // namespace agl

#endif  // AGL_NON_MERGE_CONTAINER_H
