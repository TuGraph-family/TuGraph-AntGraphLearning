#ifndef AGL_UNIT_SPEC_H
#define AGL_UNIT_SPEC_H

#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#include "base_data_structure/dtype.h"
#include "spec/feature_spec.h"

namespace agl {

class NodeSpec {
 public:
  NodeSpec(const std::string& name, AGLDType id_dtype);

  void AddDenseSpec(const std::string& f_name,
                    std::shared_ptr<DenseFeatureSpec>& df_spec);
  void AddSparseKVSpec(const std::string& f_name,
                       std::shared_ptr<SparseKVSpec>& sf_spec);
  void AddSparseKSpec(const std::string& f_name,
                      std::shared_ptr<SparseKSpec>& sf_spec);

  const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
  GetDenseFeatureSpec();

  const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
  GetSparseKVSpec();

  const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
  GetSparseKSpec();

  const AGLDType& GetNodeIdDtype();

  const std::string& GetNodeName();

 private:
  std::string node_name_;
  AGLDType node_id_dtype_;
  std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>
      dense_specs_;
  std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>> sp_kv_specs_;
  std::unordered_map<std::string, std::shared_ptr<SparseKSpec>> sp_k_specs_;
};

class EdgeSpec {
 public:
  EdgeSpec(const std::string& name, const std::shared_ptr<NodeSpec>& node1_spec,
           const std::shared_ptr<NodeSpec>& node2_spec, AGLDType id_dtype);

  void AddDenseSpec(const std::string& f_name,
                    std::shared_ptr<DenseFeatureSpec>& df_spec);
  void AddSparseKVSpec(const std::string& f_name,
                       std::shared_ptr<SparseKVSpec>& sf_spec);
  void AddSparseKSpec(const std::string& f_name,
                      std::shared_ptr<SparseKSpec>& sf_spec);

  const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
  GetDenseFeatureSpec();

  const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
  GetSparseKVSpec();

  const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
  GetSparseKSpec();

  const AGLDType& GetEidDtype();

  const std::string& GetN1Name();
  const std::string& GetN2Name();
  const std::string& GetEdgeName();

 private:
  std::string edge_name_;
  std::shared_ptr<NodeSpec> node1_spec_;  // dst
  std::shared_ptr<NodeSpec> node2_spec_;  // src
  AGLDType edge_id_dtype_;                // optional
  std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>
      dense_specs_;
  std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>> sp_kv_specs_;
  std::unordered_map<std::string, std::shared_ptr<SparseKSpec>> sp_k_specs_;
};

}  // namespace agl

#endif  // AGL_UNIT_SPEC_H
