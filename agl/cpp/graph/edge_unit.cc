#include "edge_unit.h"

namespace agl {
void EdgeUint::Init(
    std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>&
        dense_arrays,
    std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>&
        spkv_arrays,
    std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>&
        spk_arrays,
    std::shared_ptr<CSRAdj>& adj) {
  AGL_CHECK_EQUAL(d_f_array_.size(), 0);
  AGL_CHECK_EQUAL(spkv_f_array_.size(), 0);
  AGL_CHECK_EQUAL(spk_f_array_.size(), 0);
  d_f_array_ = dense_arrays;
  spkv_f_array_ = spkv_arrays;
  spk_f_array_ = spk_arrays;
  csr_ptr_ = adj;
}

std::shared_ptr<DenseFeatureArray> EdgeUint::GetDenseFeatureArray(
    const std::string& f_name) const {
  auto find = d_f_array_.find(f_name);
  AGL_CHECK(find != d_f_array_.end());
  return find->second;
}
std::shared_ptr<SparseKVFeatureArray> EdgeUint::GetSparseKVArray(
    const std::string& f_name) const {
  auto find = spkv_f_array_.find(f_name);
  AGL_CHECK(find != spkv_f_array_.end());
  return find->second;
}
std::shared_ptr<SparseKFeatureArray> EdgeUint::GetSparseKArray(
    const std::string& f_name) const {
  auto find = spk_f_array_.find(f_name);
  AGL_CHECK(find != spk_f_array_.end());
  return find->second;
}

std::shared_ptr<CSRAdj> EdgeUint::GetCSRAdj() { return csr_ptr_; }
}  // namespace agl