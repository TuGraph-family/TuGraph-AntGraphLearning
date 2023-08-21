/**
 * Copyright 2023 AntGroup CO., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
#ifndef AGL_FEATURE_INITER_H
#define AGL_FEATURE_INITER_H

#include <memory>
#include <unordered_map>
#include <vector>

#include "base_data_structure/csr.h"
#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
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
                          int element_count, int feature_dim, AGLDType f_dtype);

  ~DenseFeatureArrayIniter() override = default;

  void Init() override;

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
class SparseKVFeatureArrayIniter : public FeatureArrayIniter {
 public:
  SparseKVFeatureArrayIniter(
      std::shared_ptr<SparseKVFeatureArray>& array_to_init, int element_count,
      int total_feat_count, AGLDType key_type, AGLDType val_type);

  ~SparseKVFeatureArrayIniter() override = default;
  void Init() override;

 private:
  std::shared_ptr<SparseKVFeatureArray> array;
  int element_count;
  int total_feat_count;
  AGLDType key_type;
  AGLDType val_type;
};

class SparseKFeatureArrayIniter : public FeatureArrayIniter {
 public:
  SparseKFeatureArrayIniter(std::shared_ptr<SparseKFeatureArray>& array_to_init,
                            int element_count, int total_feat_count,
                            AGLDType key_type);
  ~SparseKFeatureArrayIniter() override = default;
  void Init() override;

 private:
  std::shared_ptr<SparseKFeatureArray> array;
  int element_count;
  int total_feat_count;
  AGLDType key_type;
};

class CSRIniter : public FeatureArrayIniter {
 public:
  CSRIniter(int row_num, int col_num, int nnz_num,
            std::shared_ptr<CSR>& csr_to_init);
  ~CSRIniter() override = default;
  void Init() override;

 private:
  int rows;     // dst node (n1) num
  int cols;     // src node (n1) num
  int nnz_num;  // edge num
  std::shared_ptr<CSR> adj;
};

}  // namespace agl

#endif  // AGL_FEATURE_INITER_H
