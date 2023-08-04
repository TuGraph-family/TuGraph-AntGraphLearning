#ifndef AGL_DENSE_FEATURE_ARRAY_H
#define AGL_DENSE_FEATURE_ARRAY_H

#include <memory>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"

namespace agl {

/**
 * 解析后，全部变成连续内存块表示的FeatureArray
 */
class DenseFeatureArray {
 public:
  void Init(std::shared_ptr<NDArray>& df_array) {
      dense_feature_array_ = df_array;
  }

  std::shared_ptr<NDArray>& GetFeatureArray() { return dense_feature_array_;}

 private:
  // element_num * feature_dim
  std::shared_ptr<NDArray> dense_feature_array_;
};

}  // namespace agl

#endif  // AGL_DENSE_FEATURE_ARRAY_H
