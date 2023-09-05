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
#ifndef AGL_COO_H
#define AGL_COO_H
#include <memory>

#include "common/safe_check.h"
#include "dtype.h"
#include "nd_array.h"

namespace agl {
struct COO {
  COO(int64_t rows_nums, int64_t col_nums, std::shared_ptr<NDArray>& row,
      std::shared_ptr<NDArray>& col, std::shared_ptr<NDArray>& data,
      bool row_sorted = false, bool col_sorted = false)
      : rows_nums_(rows_nums),
        col_nums_(col_nums),
        row_(row),
        col_(col),
        data_(data),
        row_sorted_(row_sorted),
        col_sorted_(col_sorted) {}

  // rows_nums_ and col_nums used to define the dense shape
  int64_t rows_nums_ = 0;  // n1 num
  int64_t col_nums_ = 0;   // n2 num
  std::shared_ptr<NDArray> row_ = nullptr;
  std::shared_ptr<NDArray> col_ = nullptr;
  std::shared_ptr<NDArray> data_ =
      nullptr;  // data index array, usually means edge index
  bool row_sorted_ = false;
  bool col_sorted_ = false;
};

class COOAdj {
 public:
  // for adj matrix, data_ refers to edge indices.
  // As a result, ind, indices and data should be IdDType
  explicit COOAdj(std::shared_ptr<COO>& adj) : adj_(adj) {
    AGL_CHECK(adj != nullptr);
    AGL_CHECK(adj->row_ != nullptr);
    AGL_CHECK(adj->col_ != nullptr);
    AGL_CHECK(adj->data_ != nullptr);
    AGL_CHECK_EQUAL(adj->row_->GetDType(), GetDTypeFromT<IdDType>());
    AGL_CHECK_EQUAL(adj->col_->GetDType(), GetDTypeFromT<IdDType>());
    AGL_CHECK_EQUAL(adj->data_->GetDType(), GetDTypeFromT<IdDType>());
  };

  std::shared_ptr<COO>& GetCOONDArray() { return adj_; }

 private:
  // adj matrix in coo format
  std::shared_ptr<COO> adj_ = nullptr;
};

}  // namespace agl

#endif  // AGL_COO_H
