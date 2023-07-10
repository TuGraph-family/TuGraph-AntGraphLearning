#include "spec/feature_spec.h"

namespace agl {

/////////////////////////////// DenseFeatureSpec ///////////////////////////////
DenseFeatureSpec::DenseFeatureSpec(const std::string& name, int feature_dim,
                                   AGLDType dtype) {
  this->feature_name_ = name;
  feature_dim_ = feature_dim;
  f_dtype_ = dtype;
}

/*FeatureCategory DenseFeatureSpec::GetFeatureCategory() {
  return FeatureCategory::kDense;
}*/

AGLDType DenseFeatureSpec::GetFeatureDtype() { return f_dtype_; }

int DenseFeatureSpec::GetDim() { return feature_dim_; }

/////////////////////////////// SparseKVSpec ///////////////////////////////
SparseKVSpec::SparseKVSpec(const std::string& name, int max_dim,
                           AGLDType key_dytpe, AGLDType val_dtype) {
  this->feature_name_ = name;
  max_dim_ = max_dim;
  key_dytpe_ = key_dytpe;
  val_dtype_ = val_dtype;
}

/*FeatureCategory SparseKVSpec::GetFeatureCategory() {
  return FeatureCategory::kSpKV;
}*/

AGLDType SparseKVSpec::GetKeyDtype() { return key_dytpe_; }
AGLDType SparseKVSpec::GetValDtype() { return val_dtype_; }
int SparseKVSpec::GetMaxDim() { return max_dim_; }

/////////////////////////////// SparseKSpec ///////////////////////////////
SparseKSpec::SparseKSpec(const std::string& name, int max_dim,
                         AGLDType key_dytpe) {
  this->feature_name_ = name;
  max_dim_ = max_dim;
  key_dytpe_ = key_dytpe;
}

/*FeatureCategory SparseKSpec::GetFeatureCategory() {
  return FeatureCategory::kSpK;
}*/

AGLDType SparseKSpec::GetKeyDtype() { return key_dytpe_; }
int SparseKSpec::GetMaxDim() { return max_dim_; }

}  // namespace agl