#ifndef AGL_DENSE_FEATURE_ARRAY_H
#define AGL_DENSE_FEATURE_ARRAY_H

#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "features/feature_array.h"

namespace agl {

/**
 * 解析后，全部变成连续内存块表示的FeatureArray
 */
class DenseFeatureArray {
 public:
/*  ~DenseFeatureArray() override = default;*/

  void Init(std::shared_ptr<NDArray>& df_array) {
      dense_feature_array_ = df_array;
  }

  std::shared_ptr<NDArray>& GetFeatureArray() { return dense_feature_array_;}

  //FeatureArrayCategory GetArrayType() override {
  //  return FeatureArrayCategory::kDenseArray;
  //}

  // todo get 操作
  // 取一个元素的特征 （是否必要？）
  NDArray GetFeature(int index);
  NDArray GetFeature(int start, int end);
  NDArray GetFeature(); // all feature


  // 取特征dim
  int GetFeatureDim();

  // 取特征类型
  AGLDType GetFeatureDtype();

  // put 操作能力现在非常有限，相当于是一个 un-mutable的容器

 private:
  // element_num * feature_dim
  std::shared_ptr<NDArray> dense_feature_array_;
};

}  // namespace agl

#endif  // AGL_DENSE_FEATURE_ARRAY_H
