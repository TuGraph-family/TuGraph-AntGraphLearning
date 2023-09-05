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
#ifndef AGL_OPENSOURCE_NDARRAY_H
#define AGL_OPENSOURCE_NDARRAY_H

#include <memory>

#include "base_data_structure/dtype.h"
#include "common/safe_check.h"

namespace agl {

// A simple yet fundamental container used to store graph information such as
// adjacency and feature data. It is also used for transferring data between C++
// and Python based on the buffer protocol in Pybind11.
class NDArray {
 public:
  NDArray(int rows, int cols, AGLDType dtype)
      : m_rows_(rows), m_cols_(cols), dtype_(dtype) {
    data_ = malloc(rows * cols * GetDtypeSize(dtype));
  };

  ~NDArray() { free(data_); }

  int GetRowNumber() { return m_rows_; }

  int GetColNumber() { return m_cols_; }

  AGLDType GetDType() { return dtype_; }

  void* data() { return data_; }

  template <class T>
  T* Flat() {
    AGL_CHECK(GetDTypeFromT<T>() == dtype_);
    return reinterpret_cast<T*>(data_);
  }

 private:
  NDArray(const NDArray&) = delete;
  NDArray(NDArray&&) = delete;
  NDArray& operator=(const NDArray&) = delete;

 private:
  // data type
  AGLDType dtype_;
  // a continues memory block with size
  // m_rows_*m_cols_*GetDtypeSize(dtype_)
  void* data_;
  // num of rows
  int m_rows_;
  // num of cols
  int m_cols_;
};

}  // namespace agl

#endif  // AGL_OPENSOURCE_NDARRAY_H
