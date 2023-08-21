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
#include "spec/feature_spec.h"

namespace agl {

/////////////////////////////// DenseFeatureSpec ///////////////////////////////
DenseFeatureSpec::DenseFeatureSpec(const std::string& name, int feature_dim,
                                   AGLDType dtype) {
  this->feature_name_ = name;
  feature_dim_ = feature_dim;
  f_dtype_ = dtype;
}

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

AGLDType SparseKSpec::GetKeyDtype() { return key_dytpe_; }
int SparseKSpec::GetMaxDim() { return max_dim_; }

}  // namespace agl