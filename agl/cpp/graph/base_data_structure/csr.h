#ifndef AGL_CSR_H
#define AGL_CSR_H
#include <memory>

#include "common/safe_check.h"
#include "dtype.h"
#include "nd_array.h"

namespace agl {
struct CSR {
  /*  CSR(int64_t rows_nums, int64_t col_nums,
                 std::shared_ptr<NDArray>& index_offset,
                 std::shared_ptr<NDArray>& indices,
                 std::shared_ptr<NDArray>& data,
                 bool sorted = false)
        : rows_nums_(rows_nums),
          col_nums_(col_nums),
          ind_(index_offset),
          indices_(indices),
          data_(data),
          sorted_(sorted) {}*/

  void Init(int64_t rows_nums, int64_t col_nums,
            std::shared_ptr<NDArray>& index_offset,
            std::shared_ptr<NDArray>& indices, std::shared_ptr<NDArray>& data,
            bool sorted = false) {
    rows_nums_ = rows_nums;
    col_nums_ = col_nums;
    ind_ = index_offset;
    indices_ = indices;
    data_ = data;
    sorted_ = sorted;
  }

  // todo 需要提供 to coo 功能，便于往外输出
  // 用于定义dense shape
  int64_t rows_nums_;  // n1 的数目
  int64_t col_nums_;   // n2 的数目
  std::shared_ptr<NDArray> ind_;
  std::shared_ptr<NDArray> indices_;
  std::shared_ptr<NDArray> data_;  // data index array, that is edge index
  bool sorted_;                    // 指 indices 是否 排序
};

class CSRAdj {
  // 对于adj 来说，ind, indices, data 都需要是 IdDType 类型
 public:
  explicit CSRAdj(std::shared_ptr<CSR>& adj) : adj_(adj) {
    AGL_CHECK_EQUAL(adj->ind_->GetDType(), GetDTypeFromT<IdDType>());
    AGL_CHECK_EQUAL(adj->indices_->GetDType(), GetDTypeFromT<IdDType>());
    if (adj->data_ != nullptr) {
      AGL_CHECK_EQUAL(adj->data_->GetDType(), GetDTypeFromT<IdDType>());
    }
  }

  std::shared_ptr<CSR>& GetCSRNDArray() { return adj_; }

 private:
  // csr 格式的 adj matrix
  std::shared_ptr<CSR> adj_;
};

}  // namespace agl

#endif  // AGL_CSR_H
