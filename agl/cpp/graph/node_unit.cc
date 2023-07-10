#include "node_unit.h"

#include "common/safe_check.h"

namespace agl {
void NodeUint::Init(
    std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>&
        dense_arrays,
    std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>&
        spkv_arrays,
    std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>&
        spk_arrays) {
  AGL_CHECK_EQUAL(d_f_array_.size(), 0);
  AGL_CHECK_EQUAL(spkv_f_array_.size(), 0);
  AGL_CHECK_EQUAL(spk_f_array_.size(), 0);
  d_f_array_ = dense_arrays;
  spkv_f_array_ = spkv_arrays;
  spk_f_array_ = spk_arrays;
}

std::shared_ptr<DenseFeatureArray> NodeUint::GetDenseFeatureArray(
    const std::string& name) const {
  auto find = d_f_array_.find(name);
  AGL_CHECK(find != d_f_array_.end());
  return find->second;
}

std::shared_ptr<SparseKVFeatureArray> NodeUint::GetSparseKVArray(
    const std::string& name) const {
  auto find = spkv_f_array_.find(name);
  AGL_CHECK(find != spkv_f_array_.end());
  return find->second;
}

std::shared_ptr<SparseKFeatureArray> NodeUint::GetSparseKArray(
    const std::string& name) const {
  auto find = spk_f_array_.find(name);
  AGL_CHECK(find != spk_f_array_.end());
  return find->second;
}

}  // namespace agl