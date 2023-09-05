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
#ifndef AGL_SPARSEKV_FEATURE_ARRAY_H
#define AGL_SPARSEKV_FEATURE_ARRAY_H
#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"

namespace agl {
struct SparseKVNDArray {
  // refer:
  // https://pytorch.org/docs/stable/generated/torch.sparse_csr_tensor.html
  // https://pytorch.org/docs/stable/sparse.html#sparse-csr-tensor

  // ind_offset_ （element_num + 1）* int64
  // the first value is 0, and the last is the total length of key and value
  std::shared_ptr<NDArray> ind_offset_ = nullptr;
  // NNZ keys, nnz * int64
  std::shared_ptr<NDArray> keys_ = nullptr;
  // NNZ values, nnx * T (float, double)
  std::shared_ptr<NDArray> values_ = nullptr;
};

// Sparse kv feature array container for a certain sparse kv feature
class SparseKVFeatureArray {
 public:
  void Init(std::shared_ptr<NDArray> ind_offset, std::shared_ptr<NDArray> keys,
            std::shared_ptr<NDArray> values) {
    auto kv_nd = std::make_shared<SparseKVNDArray>();
    kv_nd->ind_offset_ = ind_offset;
    kv_nd->keys_ = keys;
    kv_nd->values_ = values;
    skv_ptr_ = kv_nd;
  }

  std::shared_ptr<SparseKVNDArray>& GetFeatureArray() { return skv_ptr_; }

 private:
  std::shared_ptr<SparseKVNDArray> skv_ptr_ = nullptr;
};
}  // namespace agl
#endif  // AGL_SPARSEKV_FEATURE_ARRAY_H
