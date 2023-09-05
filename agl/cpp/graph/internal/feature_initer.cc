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
#include "internal/feature_initer.h"

namespace agl {
DenseFeatureArrayIniter::DenseFeatureArrayIniter(
    std::shared_ptr<DenseFeatureArray>& array_to_init, int element_count,
    int feature_dim, AGLDType f_dtype)
    : array(array_to_init),
      element_count(element_count),
      feature_dim(feature_dim),
      f_dtype(f_dtype) {}

void DenseFeatureArrayIniter::Init() {
  auto nd = std::make_shared<NDArray>(element_count, feature_dim, f_dtype);
  array->Init(nd);
}

SparseKVFeatureArrayIniter::SparseKVFeatureArrayIniter(
    std::shared_ptr<SparseKVFeatureArray>& array_to_init, int element_count,
    int total_feat_count, AGLDType key_type, AGLDType val_type)
    : array(array_to_init),
      element_count(element_count),
      total_feat_count(total_feat_count),
      key_type(key_type),
      val_type(val_type) {}

void SparseKVFeatureArrayIniter::Init() {
  // note index offset should keep the same as torch/numpy
  auto indptr =
      std::make_shared<NDArray>(element_count + 1, 1, GetDTypeFromT<IdDType>());
  auto* indptr_data = indptr->Flat<IdDType>();
  indptr_data[0] = 0;

  if (total_feat_count == 0) {
    // bad case: if there's no any key val, fill 0,0
    total_feat_count = 1;
  }

  auto keys = std::make_shared<NDArray>(total_feat_count, 1, key_type);
  auto vals = std::make_shared<NDArray>(total_feat_count, 1, val_type);
  if (total_feat_count == 1) {
    auto* k_p = keys->data();
    memset(k_p, 0, total_feat_count * 1 * GetDtypeSize(key_type));
    auto* v_p = vals->data();
    memset(v_p, 0, total_feat_count * 1 * GetDtypeSize(val_type));
  }

  array->Init(indptr, keys, vals);
}

SparseKFeatureArrayIniter::SparseKFeatureArrayIniter(
    std::shared_ptr<SparseKFeatureArray>& array_to_init, int element_count,
    int total_feat_count, AGLDType key_type)
    : array(array_to_init),
      element_count(element_count),
      total_feat_count(total_feat_count),
      key_type(key_type) {}

void SparseKFeatureArrayIniter::Init() {
  AGL_CHECK(key_type != AGLDType::STR)
      << "do not support string key now";
  // note index offset should keep the same as torch/numpy
  auto indptr =
      std::make_shared<NDArray>(element_count + 1, 1, GetDTypeFromT<IdDType>());
  auto* indptr_data = indptr->Flat<IdDType>();
  indptr_data[0] = 0;

  auto keys = std::make_shared<NDArray>(total_feat_count, 1, key_type);
  array->Init(indptr, keys);
}

CSRIniter::CSRIniter(int row_num, int col_num, int nnz_num,
                     std::shared_ptr<CSR>& csr_to_init)
    : rows(row_num), cols(col_num), nnz_num(nnz_num), adj(csr_to_init) {}

void CSRIniter::Init() {
  // note index offset should keep the same as torch/numpy
  auto ind_ptr =
      std::make_shared<NDArray>(rows + 1, 1, GetDTypeFromT<IdDType>());
  auto* indptr_data = ind_ptr->Flat<IdDType>();
  indptr_data[0] = 0;
  auto indices =
      std::make_shared<NDArray>(nnz_num, 1, GetDTypeFromT<IdDType>());
  auto data = std::shared_ptr<NDArray>(nullptr);
  adj->Init(rows, cols, ind_ptr, indices, data);
}

}  // namespace agl