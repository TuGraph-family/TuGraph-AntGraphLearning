#ifndef AGL_FEATURE_ARRAY_H
#define AGL_FEATURE_ARRAY_H

namespace agl {

enum FeatureArrayCategory {
  kDenseArray = 1,
  kSparseKVArray = 2,
  kSparseKArray = 3,
  kUnknown = 101
};

class BaseFeatureArray {
 public:
  virtual ~BaseFeatureArray() = default;
  //virtual FeatureArrayCategory GetArrayType() = 0;
};

}  // namespace agl

#endif  // AGL_FEATURE_ARRAY_H
