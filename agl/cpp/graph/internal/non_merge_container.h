#ifndef AGL_NON_MERGE_CONTAINER_H
#define AGL_NON_MERGE_CONTAINER_H

#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#include "base_data_structure/csr.h"
#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "common/safe_check.h"
#include "features/dense_feature_array.h"
#include "features/sparsek_feature_array.h"
#include "features/sparsekv_feature_array.h"
#include "proto/graph_feature.pb.h"
#include "spec/feature_spec.h"
#include "spec/unit_spec.h"

namespace agl {

struct NonMergeNodeInfo {
  NonMergeNodeInfo(int pb_nums, const std::shared_ptr<NodeSpec>& my_node_spec);

  void SetElementNum(const agl::proto::graph_feature::IDs& ids,
                     const AGLDType& id_dtype, int index);

  void SetFeatureCount(
      const agl::proto::graph_feature::Features& features,
      const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
          dense_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
          sp_kv_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
          sp_k_spec,
      int index);

  void ComputeOffset();

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
  NonMergeEdgeInfo(int pb_nums, const std::shared_ptr<EdgeSpec>& my_edge_spec);

  void SetElementNum(const agl::proto::graph_feature::Edges& edges,
                     const AGLDType& id_dtype, int index);

  void SetFeatureCount(
      const agl::proto::graph_feature::Features& features,
      const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
          dense_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
          sp_kv_spec,
      const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
          sp_k_spec,
      int index);

  void ComputeOffset();

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
