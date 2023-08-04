#ifndef AGL_SPARSEK_FEATURE_ARRAY_H
#define AGL_SPARSEK_FEATURE_ARRAY_H

#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"

namespace agl {
struct SparseKeyNDArray {
  // element_num * int64
  std::shared_ptr<NDArray> ind_offset_;
  // NNZ keys, nnz * int64
  std::shared_ptr<NDArray> keys_;
};

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
  std::shared_ptr<SparseKeyNDArray> sk_array_ptr_;
};
}  // namespace agl

#endif  // AGL_SPARSEK_FEATURE_ARRAY_H
