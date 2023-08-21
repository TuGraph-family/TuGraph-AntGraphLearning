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
#ifndef AGL_SPARSEK_FEATURE_ARRAY_H
#define AGL_SPARSEK_FEATURE_ARRAY_H

#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"

namespace agl {
struct SparseKeyNDArray {
  // element_num * int64
  std::shared_ptr<NDArray> ind_offset_ = nullptr;
  // NNZ keys, nnz * int64
  std::shared_ptr<NDArray> keys_ = nullptr;
};

// Sparse key feature array container for a certain sparse key feature
class SparseKFeatureArray {
 public:
  void Init(std::shared_ptr<NDArray>& ind_offset,
            std::shared_ptr<NDArray>& keys) {
    sk_array_ptr_ = std::make_shared<SparseKeyNDArray>();
    sk_array_ptr_->ind_offset_ = ind_offset;
    sk_array_ptr_->keys_ = keys;
  }

  std::shared_ptr<SparseKeyNDArray>& GetFeatureArray() { return sk_array_ptr_; }

 private:
  std::shared_ptr<SparseKeyNDArray> sk_array_ptr_ = nullptr;
};
}  // namespace agl

#endif  // AGL_SPARSEK_FEATURE_ARRAY_H
