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
#ifndef AGL_OPENSOURCE_FEATURE_SPEC_H
#define AGL_OPENSOURCE_FEATURE_SPEC_H

#include <string>

#include "base_data_structure/dtype.h"

namespace agl {
enum FeatureCategory { kDense = 1, kSpKV = 2, kSpK = 3 };

class DenseFeatureSpec {
 public:
  DenseFeatureSpec(const std::string& name, int feature_dim, AGLDType dtype);

  AGLDType GetFeatureDtype();

  int GetDim();
  std::string GetFeatureName() const { return feature_name_; };

 private:
  int feature_dim_;
  AGLDType f_dtype_;
  std::string feature_name_;
};

class SparseKVSpec {
 public:
  SparseKVSpec(const std::string& name, int max_dim, AGLDType key_dytpe,
               AGLDType val_dtype);
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

class SparseKSpec {
 public:
  SparseKSpec(const std::string& name, int max_dim, AGLDType key_dytpe);
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
