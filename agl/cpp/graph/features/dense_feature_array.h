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
#ifndef AGL_DENSE_FEATURE_ARRAY_H
#define AGL_DENSE_FEATURE_ARRAY_H

#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"

namespace agl {

// Dense feature array container for a certain DenseFeature
// for example, a certain kind of node (e.g. "user") has DenseFeature "dense",
// all "dense" feature of this kind node would be stored together in this
// container
class DenseFeatureArray {
 public:
  void Init(std::shared_ptr<NDArray>& df_array) {
    dense_feature_array_ = df_array;
  }

  std::shared_ptr<NDArray>& GetFeatureArray() { return dense_feature_array_; }

 private:
  // element_num * feature_dim
  std::shared_ptr<NDArray> dense_feature_array_ = nullptr;
};

}  // namespace agl

#endif  // AGL_DENSE_FEATURE_ARRAY_H
