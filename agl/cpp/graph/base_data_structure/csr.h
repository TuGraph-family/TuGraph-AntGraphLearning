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
#ifndef AGL_CSR_H
#define AGL_CSR_H
#include <memory>

#include "common/safe_check.h"
#include "dtype.h"
#include "nd_array.h"

namespace agl {
struct CSR {
  void Init(int64_t rows_nums, int64_t col_nums,
            std::shared_ptr<NDArray>& index_offset,
            std::shared_ptr<NDArray>& indices, std::shared_ptr<NDArray>& data,
            bool sorted = false) {
    rows_nums_ = rows_nums;
    col_nums_ = col_nums;
    ind_ = index_offset;
    indices_ = indices;
    data_ = data;
    sorted_ = sorted;
  }

  // rows_nums_ and col_nums used to define the dense shape
  int64_t rows_nums_ = 0;                   // n1 num
  int64_t col_nums_ = 0;                    // n2 num
  std::shared_ptr<NDArray> ind_ = nullptr;  // ind_ptr
  std::shared_ptr<NDArray> indices_ = nullptr;
  std::shared_ptr<NDArray> data_ =
      nullptr;           // data index array, such as  edge indices
  bool sorted_ = false;  // whether indices is sorted or not
};

class CSRAdj {
 public:
  /**
   * for adj matrix, data_ refers to edge indices.
   * As a result, ind, indices and data should be IdDType
   * @param adj : adjacency information in CSR format
   */
  explicit CSRAdj(std::shared_ptr<CSR>& adj) : adj_(adj) {
    AGL_CHECK(adj != nullptr);
    AGL_CHECK(adj->ind_ != nullptr);
    AGL_CHECK(adj->indices_ != nullptr);
    AGL_CHECK_EQUAL(adj->ind_->GetDType(), GetDTypeFromT<IdDType>());
    AGL_CHECK_EQUAL(adj->indices_->GetDType(), GetDTypeFromT<IdDType>());
    if (adj->data_ != nullptr) {
      AGL_CHECK_EQUAL(adj->data_->GetDType(), GetDTypeFromT<IdDType>());
    }
  }

  std::shared_ptr<CSR>& GetCSRNDArray() { return adj_; }

 private:
  // adj matrix in csr format
  std::shared_ptr<CSR> adj_ = nullptr;
};

}  // namespace agl

#endif  // AGL_CSR_H
