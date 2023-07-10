#ifndef AGL_OPENSOURCE_NDARRAY_H
#define AGL_OPENSOURCE_NDARRAY_H

#include <memory>

#include "base_data_structure/dtype.h"
#include "common/safe_check.h"

namespace agl {

// todo， 利用继承关系搞定 NDArrayView 和
// NDArray里面指针的生命周期，目前看起来应该同生命周期。NDArrayView ->
// NDArrayImpl
template <typename T>
class NDArrayView {
 public:
  NDArrayView(T* data, int rows, int cols)
      : m_data_(data), m_rows_(rows), m_cols_(cols) {}

  T* operator[](int i) const { return &m_data_[i * m_cols_]; }

  const T* operator[](int i) { return &m_data_[i * m_cols_]; }

  T& operator()(int i, int j) const { return m_data_[i * m_cols_ + j]; }

 private:
  T* m_data_;
  int m_rows_;
  int m_cols_;
};

class NDArray {
 public:
  NDArray(int rows, int cols, AGLDType dtype)
      : m_rows_(rows), m_cols_(cols), dtype_(dtype) {
    if (rows * cols > 0) {
      data_ = malloc(rows * cols * GetDtypeSize(dtype)); //new char[rows * cols * GetDtypeSize(dtype)];
    }
  };
  ~NDArray() {
    free(data_);
    //delete[] static_cast<char*>(data_);
  }

  int GetRowNumber() { return m_rows_; }

  int GetColNumber() { return m_cols_; }

  AGLDType GetDType() { return dtype_; }

  void* data() {return data_;}

  template <class T>
  T* Flat() {
    AGL_CHECK(GetDTypeFromT<T>() == dtype_);
    return reinterpret_cast<T*>(data_);
  }

  template <class T>
  std::unique_ptr<NDArrayView<T>> ToView() {
    AGL_CHECK(GetDTypeFromT<T>() == dtype_);
    return std::unique_ptr<NDArrayView<T>>(
        new NDArrayView<T>(reinterpret_cast<T*>(data_), m_rows_, m_cols_));
  }

 private:
  AGLDType dtype_;
  void* data_;
  int m_rows_;
  int m_cols_;
};

}  // namespace agl

#endif  // AGL_OPENSOURCE_NDARRAY_H
