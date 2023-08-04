#ifndef AGL_EDGE_UNIT_H
#define AGL_EDGE_UNIT_H
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#include "base_data_structure/coo.h"
#include "base_data_structure/csr.h"
#include "features/dense_feature_array.h"
#include "features/sparsek_feature_array.h"
#include "features/sparsekv_feature_array.h"

namespace agl {

/**
 * 一种类型（异构类型）边的特征容器，取特征的index就是mapping 后边的id
 * @tparam T
 */
class EdgeUint {
 public:
  void Init(
      std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>&
          dense_arrays,
      std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>&
          spkv_arrays,
      std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>&
          spk_arrays,
      std::shared_ptr<CSRAdj>& adj);

  std::shared_ptr<DenseFeatureArray> GetDenseFeatureArray(
      const std::string& f_name) const;
  std::shared_ptr<SparseKVFeatureArray> GetSparseKVArray(
      const std::string& f_name) const;
  std::shared_ptr<SparseKFeatureArray> GetSparseKArray(
      const std::string& f_name) const;

  std::shared_ptr<CSRAdj> GetCSRAdj();

 private:
  // 如果 csr是排序好的，那么由csr 生成的 coo
  // 一定是排好序的，因此，考虑只支持csr格式的外部数据
  std::shared_ptr<CSRAdj> csr_ptr_;
  // coo_ptr 进行 lazy init
  std::shared_ptr<COOAdj> coo_ptr_;

  // 特征们按照 CSR中的顺序排布
  // csr 第一维 是从 0 ~ dst_nodes_num 数目个偏移量
  // 第二维是 nnz 数目个src nodes, 第一，二维展开后的结果的顺序就是边的编号
  // 然后边的特征顺序排布按照这个顺序
  std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>
      d_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>
      spkv_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>
      spk_f_array_;
};

}  // namespace agl

#endif  // AGL_EDGE_UNIT_H
