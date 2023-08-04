#ifndef AGL_FEATURE_FILLER_H
#define AGL_FEATURE_FILLER_H

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

class Filler {
 public:
  virtual ~Filler() = default;
  virtual void Fill() = 0;
};

class DenseFeatureFiller : public Filler {
 public:
  DenseFeatureFiller(std::shared_ptr<DenseFeatureArray>& array_to_fill,
                     int offset,
                     agl::proto::graph_feature::Features_DenseFeatures* df_src,
                     AGLDType dtype);

  ~DenseFeatureFiller() override = default;
  void Fill() override;

 private:
  std::shared_ptr<DenseFeatureArray> array;
  int offset;
  agl::proto::graph_feature::Features_DenseFeatures* df_src;
  AGLDType dtype;
};

class SparseKVFeatureFiller : public Filler {
 public:
  SparseKVFeatureFiller(
      std::shared_ptr<SparseKVFeatureArray>& array_to_fill, int index_offset,
      int feat_offset, int max_dim,
      agl::proto::graph_feature::Features_SparseKVFeatures* spkv_src,
      AGLDType key_type, AGLDType val_type);

  ~SparseKVFeatureFiller() override = default;
  void Fill() override;

 private:
  std::shared_ptr<SparseKVFeatureArray> array;
  int index_offset;
  int feat_offset;
  int max_dim;
  agl::proto::graph_feature::Features_SparseKVFeatures* spkv_src;
  AGLDType key_dtype;
  AGLDType val_dtype;
};

class SparseKFeatureFiller : public Filler {
 public:
  SparseKFeatureFiller(
      std::shared_ptr<SparseKFeatureArray>& array_to_fill, int index_offset,
      int feat_offset, int max_dim,
      agl::proto::graph_feature::Features_SparseKFeatures* spk_src,
      AGLDType key_type);

  ~SparseKFeatureFiller() override = default;
  void Fill() override;

 private:
  std::shared_ptr<SparseKFeatureArray> array;
  int index_offset;
  int feat_offset;
  int max_dim;
  agl::proto::graph_feature::Features_SparseKFeatures* spk_src;
  AGLDType key_dtype;
};

class CSRFiller : public Filler {
 public:
  CSRFiller(std::shared_ptr<CSR>& dst, int ind_position_offset,
            int indices_position_offset, int n1_content_offset,
            int n2_content_offset, agl::proto::graph_feature::Edges_CSR* src);

  ~CSRFiller() override = default;
  void Fill() override;

 private:
  std::shared_ptr<CSR> adj;
  int ind_position_offset;      // offset of dst node
  int indices_position_offset;  // edge  offset
  int n1_content_offset;        // dst node's  offset
  int n2_content_offset;        // src node's offset
  agl::proto::graph_feature::Edges_CSR* csr_pb;
};
}  // namespace agl

#endif  // AGL_FEATURE_FILLER_H
