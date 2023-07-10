#ifndef AGL_FEATURE_INITER_H
#define AGL_FEATURE_INITER_H

#include <memory>
#include <unordered_map>
#include <vector>

#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "base_data_structure/csr.h"
#include "common/safe_check.h"
#include "features/dense_feature_array.h"
#include "features/sparsek_feature_array.h"
#include "features/sparsekv_feature_array.h"
#include "proto/graph_feature.pb.h"

namespace agl {

class FeatureArrayIniter {
 public:
  virtual void Init() = 0;
  virtual ~FeatureArrayIniter() = default;
};

class DenseFeatureArrayIniter : public FeatureArrayIniter {
 public:
  DenseFeatureArrayIniter(std::shared_ptr<DenseFeatureArray>& array_to_init,
                          int element_count, int feature_dim, AGLDType f_dtype)
      : array(array_to_init),
        element_count(element_count),
        feature_dim(feature_dim),
        f_dtype(f_dtype) {}

  ~DenseFeatureArrayIniter() override = default;

  void Init() override {
    //std::cout<< "dense init begin. element_count:" << element_count << ", dim:" << feature_dim <<"\n";
    auto nd = std::make_shared<NDArray>(element_count, feature_dim, f_dtype);
    array->Init(nd);
    //auto nd_res = array->GetFeatureArray();
    //std::cout << "nd_res is null ptr? " << (nd_res == nullptr) << "\n";
    //std::cout << "nd_res dim:" << nd_res->GetRowNumber() << ", " << nd_res->GetColNumber() << "\n";
  }

 private:
  std::shared_ptr<DenseFeatureArray> array;
  int element_count;
  int feature_dim;
  AGLDType f_dtype;
};

/**
 *  SparseKVFeatureArrayIniter, 创建 CSR格式的 sparse kv nd array
 *  格式和torch 格式一致
 */
class SparseKVFeatureArrayIniter : public FeatureArrayIniter{
 public:
  SparseKVFeatureArrayIniter(
      std::shared_ptr<SparseKVFeatureArray>& array_to_init, int element_count,
      int total_feat_count, AGLDType key_type, AGLDType val_type)
      : array(array_to_init),
        element_count(element_count),
        total_feat_count(total_feat_count),
        key_type(key_type),
        val_type(val_type) {}
  ~SparseKVFeatureArrayIniter() override = default;
  void Init() override {
    //std::cout<< "sparse kv init begin: element_count:" << element_count << ", total_feat_count:" << total_feat_count << ",keyt:" << key_type << "vt:" << val_type<<"\n";

    // note index offset should keep the same as torch/numpy
    auto indptr =
        std::make_shared<NDArray>(element_count + 1, 1, GetDTypeFromT<IdDType>());
    auto* indptr_data = indptr->Flat<IdDType>();
    indptr_data[0] = 0;

    if (total_feat_count == 0){
      total_feat_count = 1; // 特殊处理，如果一个key val 也没有，填充0，0的元素
    }

    auto keys = std::make_shared<NDArray>(total_feat_count, 1, key_type);
    auto vals = std::make_shared<NDArray>(total_feat_count, 1, val_type);
    if(total_feat_count == 1){
      auto* k_p = keys->data();
      memset(k_p, 0, total_feat_count*1* GetDtypeSize(key_type));
      auto* v_p = vals->data();
      memset(v_p, 0, total_feat_count*1* GetDtypeSize(val_type));
    }

    array->Init(indptr, keys, vals);
  }

 private:
  std::shared_ptr<SparseKVFeatureArray> array;
  int element_count;
  int total_feat_count;
  AGLDType key_type;
  AGLDType val_type;
};

class SparseKFeatureArrayIniter : public FeatureArrayIniter{
 public:
  SparseKFeatureArrayIniter(std::shared_ptr<SparseKFeatureArray>& array_to_init,
                            int element_count, int total_feat_count,
                            AGLDType key_type)
      : array(array_to_init),
        element_count(element_count),
        total_feat_count(total_feat_count),
        key_type(key_type) {}
  ~SparseKFeatureArrayIniter() override =default;
  void Init() override {
    //std::cout<< "sparse k init begin" << "\n";
    // todo, 如果key是非数值类型暂时不支持
    AGL_CHECK(key_type != AGLDType::STR);
    // note index offset should keep the same as torch/numpy
    auto indptr =
        std::make_shared<NDArray>(element_count + 1, 1, GetDTypeFromT<IdDType>());
    auto* indptr_data = indptr->Flat<IdDType>();
    indptr_data[0] = 0;

    auto keys = std::make_shared<NDArray>(total_feat_count, 1, key_type);
    array->Init(indptr, keys);
  }

 private:
  std::shared_ptr<SparseKFeatureArray> array;
  int element_count;
  int total_feat_count;
  AGLDType key_type;
};

class CSRIniter : public FeatureArrayIniter{
 public:
  CSRIniter(int row_num, int col_num, int nnz_num,
            std::shared_ptr<CSR>& csr_to_init)
      : rows(row_num), cols(col_num), nnz_num(nnz_num), adj(csr_to_init) {}
  ~CSRIniter() override = default;
  void Init() override {
    //std::cout<< "csr init begin" << "\n";
    // todo: NDArray 需要提供 fill zero 初始化的功能
    // note index offset should keep the same as torch/numpy
    auto ind_ptr = std::make_shared<NDArray>(rows+1, 1, GetDTypeFromT<IdDType>());
    auto* indptr_data = ind_ptr->Flat<IdDType>();
    indptr_data[0] = 0;
    auto indices = std::make_shared<NDArray>(nnz_num, 1, GetDTypeFromT<IdDType>());
    auto data = std::shared_ptr<NDArray> (nullptr); // 暂时不需要提供
    adj->Init(rows, cols, ind_ptr, indices, data);
  }

 private:
  int rows;     // dst node (n1) num
  int cols;     // src node (n1) num
  int nnz_num;  // 边的数目
  std::shared_ptr<CSR> adj;
};

}  // namespace agl

#endif  // AGL_FEATURE_INITER_H
