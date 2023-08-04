#include "unit_spec.h"

namespace agl {
/////////////////////////////// NodeSpec ///////////////////////////////
NodeSpec::NodeSpec(const std::string& name, AGLDType id_dtype)
    : node_name_(name), node_id_dtype_(id_dtype) {}

void NodeSpec::AddDenseSpec(const std::string& f_name,
                            std::shared_ptr<DenseFeatureSpec>& df_spec) {
  dense_specs_.insert({f_name, df_spec});
}
void NodeSpec::AddSparseKVSpec(const std::string& f_name,
                               std::shared_ptr<SparseKVSpec>& sf_spec) {
  sp_kv_specs_.insert({f_name, sf_spec});
}
void NodeSpec::AddSparseKSpec(const std::string& f_name,
                              std::shared_ptr<SparseKSpec>& sf_spec) {
  sp_k_specs_.insert({f_name, sf_spec});
}
const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
NodeSpec::GetDenseFeatureSpec() {
  return dense_specs_;
}

const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
NodeSpec::GetSparseKVSpec() {
  return sp_kv_specs_;
}

const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
NodeSpec::GetSparseKSpec() {
  return sp_k_specs_;
}

const AGLDType& NodeSpec::GetNodeIdDtype() { return node_id_dtype_; }
const std::string& NodeSpec::GetNodeName() { return node_name_; }

/////////////////////////////// EdgeSpec ///////////////////////////////
EdgeSpec::EdgeSpec(const std::string& name,
                   const std::shared_ptr<NodeSpec>& node1_spec,
                   const std::shared_ptr<NodeSpec>& node2_spec,
                   AGLDType id_dtype)
    : edge_name_(name),
      node1_spec_(node1_spec),
      node2_spec_(node2_spec),
      edge_id_dtype_(id_dtype) {}
void EdgeSpec::AddDenseSpec(const std::string& f_name,
                            std::shared_ptr<DenseFeatureSpec>& df_spec) {
  dense_specs_.insert({f_name, df_spec});
}

void EdgeSpec::AddSparseKVSpec(const std::string& f_name,
                               std::shared_ptr<SparseKVSpec>& sf_spec) {
  sp_kv_specs_.insert({f_name, sf_spec});
}

void EdgeSpec::AddSparseKSpec(const std::string& f_name,
                              std::shared_ptr<SparseKSpec>& sf_spec) {
  sp_k_specs_.insert({f_name, sf_spec});
}

const std::unordered_map<std::string, std::shared_ptr<DenseFeatureSpec>>&
EdgeSpec::GetDenseFeatureSpec() {
  return dense_specs_;
}

const std::unordered_map<std::string, std::shared_ptr<SparseKVSpec>>&
EdgeSpec::GetSparseKVSpec() {
  return sp_kv_specs_;
}

const std::unordered_map<std::string, std::shared_ptr<SparseKSpec>>&
EdgeSpec::GetSparseKSpec() {
  return sp_k_specs_;
}

const AGLDType& EdgeSpec::GetEidDtype() { return edge_id_dtype_; }

const std::string& EdgeSpec::GetN1Name() { return node1_spec_->GetNodeName(); }

const std::string& EdgeSpec::GetN2Name() { return node2_spec_->GetNodeName(); }

const std::string& EdgeSpec::GetEdgeName() { return edge_name_; }

}  // namespace agl