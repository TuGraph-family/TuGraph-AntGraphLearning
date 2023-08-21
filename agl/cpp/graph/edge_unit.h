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

// The minimal unit for a specific category of edges.
// It mainly contains adjacency data and feature arrays for this kind of edges.
// IDs are re-mapped and serve as the subscript of features.
// Now we don't store either raw IDs or ID mappings.
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
  std::shared_ptr<CSRAdj> csr_ptr_;

  std::unordered_map<std::string, std::shared_ptr<DenseFeatureArray>>
      d_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKVFeatureArray>>
      spkv_f_array_;
  std::unordered_map<std::string, std::shared_ptr<SparseKFeatureArray>>
      spk_f_array_;
};

}  // namespace agl

#endif  // AGL_EDGE_UNIT_H
