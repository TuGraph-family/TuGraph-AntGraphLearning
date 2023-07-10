#ifndef AGL_SPARSEK_FEATURE_ARRAY_H
#define AGL_SPARSEK_FEATURE_ARRAY_H

#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "features/feature_array.h"

namespace agl {
struct SparseKeyNDArray {
  // element_num * int64
  std::shared_ptr<NDArray> ind_offset_;
  // NNZ keys, nnz * int64
  std::shared_ptr<NDArray> keys_;
};

class SparseKFeatureArray {
 public:
/*  ~SparseKFeatureArray() override = default;*/

  void Init(std::shared_ptr<NDArray>& ind_offset,
            std::shared_ptr<NDArray>& keys) {
    sk_array_ptr_ = std::make_shared<SparseKeyNDArray>();
    sk_array_ptr_->ind_offset_ = ind_offset;
    sk_array_ptr_->keys_ = keys;
  }

  std::shared_ptr<SparseKeyNDArray>& GetFeatureArray() { return sk_array_ptr_; }

  // 取一个元素的特征 （是否必要？）
  std::shared_ptr<SparseKeyNDArray> GetFeature(int index);
  std::shared_ptr<SparseKeyNDArray> GetFeature(int start, int end);
  std::shared_ptr<SparseKeyNDArray> GetFeature();  // all feature

  // 取特征dim, max_feature_dim 目前存在 spec中
  // int GetFeatureDim();

  // 取特征类型
  AGLDType GetFeatureKeyDtype();

 private:
  std::shared_ptr<SparseKeyNDArray> sk_array_ptr_;
};
}  // namespace agl

#endif  // AGL_SPARSEK_FEATURE_ARRAY_H
