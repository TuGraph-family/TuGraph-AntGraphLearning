#ifndef AGL_NODE_UNIT_H
#define AGL_NODE_UNIT_H
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#include "features/dense_feature_array.h"
#include "features/sparsek_feature_array.h"
#include "features/sparsekv_feature_array.h"

namespace agl {
/**
 * 一种类型(异构类型)点 对应的特征容器，取特征的下标就是 点的 index
 * @tparam T
 */
class NodeUint {
 public:
  void Init(
      std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>&
          dense_arrays,
      std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>&
          spkv_arrays,
      std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>&
          spk_arrays);

  std::shared_ptr<DenseFeatureArray> GetDenseFeatureArray(
      const std::string& name) const;
  std::shared_ptr<SparseKVFeatureArray> GetSparseKVArray(
      const std::string& name) const;
  std::shared_ptr<SparseKFeatureArray> GetSparseKArray(
      const std::string& name) const;

 private:
  // std::string node_name_; // 是否必要？

  // 特征们，按照 id 的顺序进行排布
  std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>
      d_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>
      spkv_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>
      spk_f_array_;
};
}  // namespace agl

#endif  // AGL_NODE_UNIT_H
