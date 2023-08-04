#ifndef AGL_OPENSOURCE_NDARRAY_H
#define AGL_OPENSOURCE_NDARRAY_H

#include <memory>

#include "base_data_structure/dtype.h"
#include "common/safe_check.h"

namespace agl {

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
  AGLDType dtype_;
  void* data_;
  int m_rows_;
  int m_cols_;
};

}  // namespace agl

#endif  // AGL_OPENSOURCE_NDARRAY_H
