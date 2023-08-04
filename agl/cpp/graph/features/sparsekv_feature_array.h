#ifndef AGL_SPARSEKV_FEATURE_ARRAY_H
#define AGL_SPARSEKV_FEATURE_ARRAY_H
#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"

namespace agl {
struct SparseKVNDArray {
  // element_num * int64
  // ind_offset_ 保持和 torch 一致， 需要 （element_num + 1）* int64
  // 其中第一位是0， 最后一位是 key 和 value 的长度
  // refer:
  // https://pytorch.org/docs/stable/generated/torch.sparse_csr_tensor.html
  // https://pytorch.org/docs/stable/sparse.html#sparse-csr-tensor

  std::shared_ptr<NDArray> ind_offset_;
  // NNZ keys, nnz * int64
  std::shared_ptr<NDArray> keys_;
  // NNZ values, nnx * T (float, double)
  std::shared_ptr<NDArray> values_;
};

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
  std::shared_ptr<SparseKVNDArray> skv_ptr_;
};
}  // namespace agl
#endif  // AGL_SPARSEKV_FEATURE_ARRAY_H
