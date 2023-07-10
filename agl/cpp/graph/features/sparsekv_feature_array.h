#ifndef AGL_SPARSEKV_FEATURE_ARRAY_H
#define AGL_SPARSEKV_FEATURE_ARRAY_H
#include <vector>
#include <memory>

#include "features/feature_array.h"
#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"

namespace agl {
struct SparseKVNDArray {
  // element_num * int64
  // ind_offset_ 保持和 torch 一致， 需要 （element_num + 1）* int64
  // 其中第一位是0， 最后一位是 key 和 value 的长度
  // refer: https://pytorch.org/docs/stable/generated/torch.sparse_csr_tensor.html
  //        https://pytorch.org/docs/stable/sparse.html#sparse-csr-tensor

  std::shared_ptr<NDArray> ind_offset_;
  // NNZ keys, nnz * int64
  std::shared_ptr<NDArray> keys_;
  // NNZ values, nnx * T (float, double)
  std::shared_ptr<NDArray> values_;
};

class SparseKVFeatureArray {
 public:
/*  ~SparseKVFeatureArray() override = default;*/

  void Init(std::shared_ptr<NDArray> ind_offset,
            std::shared_ptr<NDArray> keys, std::shared_ptr<NDArray> values) {
    auto kv_nd= std::make_shared<SparseKVNDArray>();
    kv_nd->ind_offset_ =  ind_offset;
    kv_nd->keys_ = keys;
    kv_nd->values_ = values;
    skv_ptr_ = kv_nd;
    //skv_ptr_->ind_offset_ = ind_offset;
    //skv_ptr_->keys_ = keys;
    //skv_ptr_->values_ = values;
  }

  std::shared_ptr<SparseKVNDArray>& GetFeatureArray() {
    return skv_ptr_;
  }

  // get function
  // 取一个元素的特征 （是否必要？）
  std::shared_ptr<SparseKVNDArray> GetFeature(int index);
  std::shared_ptr<SparseKVNDArray> GetFeature(int start, int end);
  std::shared_ptr<SparseKVNDArray> GetFeature(); // all feature

  // 取特征dim, max_feature_dim 目前存在 spec中
  //int GetFeatureDim();

  // 取特征类型
  AGLDType GetFeatureKeyDtype();
  AGLDType GetFeatureValueDtype();

  // todo 同样 set 功能，特别是 add 功能比较受限，是 un-mutable 的容器


 private:
  // 采用csr的方式存储, 方便取出每个元素对应的特征
  // element_num * int64
  //std::shared_ptr<NDArray> ind_offset_;
  // NNZ keys, nnz * int64
  //std::shared_ptr<NDArray> keys_;
  // NNZ values, nnx * T (float, double)
  //std::shared_ptr<NDArray> values_;
  std::shared_ptr<SparseKVNDArray> skv_ptr_;


};
}
#endif  // AGL_SPARSEKV_FEATURE_ARRAY_H
