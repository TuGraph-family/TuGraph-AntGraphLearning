#ifndef AGL_OPENSOURCE_FEATURE_SPEC_H
#define AGL_OPENSOURCE_FEATURE_SPEC_H

#include <string>

#include "base_data_structure/dtype.h"

namespace agl {
enum FeatureCategory { kDense = 1, kSpKV = 2, kSpK = 3 };

/*class FeatureSpec {
 public:
  virtual ~FeatureSpec() = default;
  virtual FeatureCategory GetFeatureCategory() = 0;
  virtual std::string GetFeatureName() const { return feature_name_; };

 protected:
  std::string feature_name_;
};*/

class DenseFeatureSpec {
 public:
  DenseFeatureSpec(const std::string& name, int feature_dim,
                   AGLDType dtype);
/*  ~DenseFeatureSpec() override = default;
  FeatureCategory GetFeatureCategory() override;*/

  AGLDType GetFeatureDtype();

  int GetDim();
  std::string GetFeatureName() const { return feature_name_; };

 private:
  int feature_dim_;
  AGLDType f_dtype_;
  std::string feature_name_;
};

class SparseKVSpec  {
 public:
  SparseKVSpec(const std::string& name, int max_dim, AGLDType key_dytpe,
               AGLDType val_dtype);
/*  ~SparseKVSpec() override = default;
  FeatureCategory GetFeatureCategory() override;*/
  AGLDType GetKeyDtype();
  AGLDType GetValDtype();
  int GetMaxDim();
  std::string GetFeatureName() const { return feature_name_; };

 private:
  int max_dim_;
  AGLDType key_dytpe_;
  AGLDType val_dtype_;
  std::string feature_name_;
};

class SparseKSpec  {
 public:
  SparseKSpec(const std::string& name, int max_dim, AGLDType key_dytpe);
/*  ~SparseKSpec() override = default;
  FeatureCategory GetFeatureCategory() override;*/
  AGLDType GetKeyDtype();
  int GetMaxDim();
  std::string GetFeatureName() const { return feature_name_; };

 private:
  int max_dim_;
  AGLDType key_dytpe_;
  std::string feature_name_;
};

}  // namespace agl

#endif  // AGL_OPENSOURCE_FEATURE_SPEC_H