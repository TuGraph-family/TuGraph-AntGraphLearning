#ifndef AGL_COO_H
#define AGL_COO_H

#include <memory>

#include "common/safe_check.h"
#include "dtype.h"
#include "nd_array.h"

namespace agl {
struct COO {
  COO(int64_t rows_nums, int64_t col_nums, std::shared_ptr<NDArray>& row,
      std::shared_ptr<NDArray>& col, std::shared_ptr<NDArray>& data,
      bool row_sorted = false, bool col_sorted = false)
      : rows_nums_(rows_nums),
        col_nums_(col_nums),
        row_(row),
        col_(col),
        data_(data),
        row_sorted_(row_sorted),
        col_sorted_(col_sorted) {}

  int64_t rows_nums_;  // n1 的数目
  int64_t col_nums_;   // n2 的数目
  std::shared_ptr<NDArray> row_;
  std::shared_ptr<NDArray> col_;
  std::shared_ptr<NDArray> data_;  // data index array, that is edge index
  bool row_sorted_;
  bool col_sorted_;
};

class COOAdj {
 public:
  // 对于adj 来说，ind, indices, data 都需要是 IdDType 类型
  explicit COOAdj(std::shared_ptr<COO>& adj) : adj_(adj) {
    AGL_CHECK_EQUAL(adj->row_->GetDType(), GetDTypeFromT<IdDType>());
    AGL_CHECK_EQUAL(adj->col_->GetDType(), GetDTypeFromT<IdDType>());
    AGL_CHECK_EQUAL(adj->data_->GetDType(), GetDTypeFromT<IdDType>());
  };

  std::shared_ptr<COO>& GetCOONDArray() { return adj_; }

 private:
  // csr 格式的 adj matrix
  std::shared_ptr<COO> adj_;
};

}  // namespace agl

#endif  // AGL_COO_H
