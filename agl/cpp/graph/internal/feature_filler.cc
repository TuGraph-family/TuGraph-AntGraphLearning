#include "internal/feature_filler.h"

#include <cstring>
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

namespace {
using namespace agl;

void CopyPBListToNDArray(std::shared_ptr<NDArray>& nd_dst, AGLDType dtype,
                         int dst_offset, int src_offset,
                         agl::proto::graph_feature::FloatList* f32_list,
                         agl::proto::graph_feature::Float64List* f64_list,
                         agl::proto::graph_feature::Int64List* i64_list,
                         agl::proto::graph_feature::BytesList* str_list) {
  switch (dtype) {
    case AGLDType::FLOAT: {
      AGL_CHECK(f32_list != nullptr);
      auto* dst_ptr = nd_dst->Flat<float>() + dst_offset;
      const auto* src_ptr = f32_list->value().data() + src_offset;

      std::memcpy(dst_ptr, src_ptr,
                  (f32_list->value_size() - src_offset) * sizeof(float));
      break;
    }
    case AGLDType::DOUBLE: {
      AGL_CHECK(f64_list != nullptr);
      auto* dst_ptr = nd_dst->Flat<double>() + dst_offset;
      const auto* src_ptr = f64_list->value().data() + src_offset;
      std::memcpy(dst_ptr, src_ptr,
                  (f64_list->value_size() - src_offset) * sizeof(double));

      break;
    }
    case AGLDType::INT64: {
      AGL_CHECK(i64_list != nullptr);
      auto* dst_ptr = nd_dst->Flat<int64_t>() + dst_offset;
      const auto* src_ptr = i64_list->value().data() + src_offset;
      std::memcpy(dst_ptr, src_ptr,
                  (i64_list->value_size() - src_offset) * sizeof(int64_t));

      break;
    }
    case AGLDType::STR: {
      AGL_CHECK(false) << "Not supported copy str to nd array";
      break;
    }
    default:
      AGL_CHECK(false) << "Not supported dtype AGLDType::" << dtype;
  }
}

void CopyIndToNDArray(std::shared_ptr<NDArray>& nd_dst,
                      const agl::proto::graph_feature::Int64List& i64_src,
                      int dst_position, int src_position, int copy_num,
                      int content_offset) {
  int64_t* data = nd_dst->Flat<int64_t>();
  for (size_t i = 0; i < copy_num; ++i) {
    int src_index = src_position + i;
    int dst_index = dst_position + i;
    data[dst_index] = i64_src.value(src_index) + content_offset;
  }
}

}  // namespace

namespace agl {

DenseFeatureFiller::DenseFeatureFiller(
    std::shared_ptr<DenseFeatureArray>& array_to_fill, int offset,
    agl::proto::graph_feature::Features_DenseFeatures* df_src, AGLDType dtype)
    : array(array_to_fill), offset(offset), df_src(df_src), dtype(dtype) {}

void DenseFeatureFiller::Fill() {
  auto* f32 = df_src->has_f32s() ? df_src->mutable_f32s() : nullptr;
  auto* f64 = df_src->has_f64s() ? df_src->mutable_f64s() : nullptr;
  auto* i64 = df_src->has_i64s() ? df_src->mutable_i64s() : nullptr;
  auto* raws = df_src->has_raws() ? df_src->mutable_raws() : nullptr;
  CopyPBListToNDArray(array->GetFeatureArray(), dtype, offset, 0, f32, f64, i64,
                      raws);
}

SparseKVFeatureFiller::SparseKVFeatureFiller(
    std::shared_ptr<SparseKVFeatureArray>& array_to_fill, int index_offset,
    int feat_offset, int max_dim,
    agl::proto::graph_feature::Features_SparseKVFeatures* spkv_src,
    AGLDType key_type, AGLDType val_type)
    : array(array_to_fill),
      index_offset(index_offset),
      feat_offset(feat_offset),
      max_dim(max_dim),
      spkv_src(spkv_src),
      key_dtype(key_type),
      val_dtype(val_type) {}

void SparseKVFeatureFiller::Fill() {
  auto& sp_kv_ptr = array->GetFeatureArray();
  // step 1: index_offset + 1 to adapt to standard csr
  CopyIndToNDArray(sp_kv_ptr->ind_offset_, spkv_src->lens(), index_offset + 1,
                   0, spkv_src->lens().value_size(), feat_offset);
  // step 2: keys
  // todo: should check max dim ?
  CopyPBListToNDArray(sp_kv_ptr->keys_, key_dtype, feat_offset, 0, nullptr,
                      nullptr, spkv_src->mutable_keys(), nullptr);
  // step 3: values
  auto* f32 = spkv_src->has_f32s() ? spkv_src->mutable_f32s() : nullptr;
  auto* f64 = spkv_src->has_f64s() ? spkv_src->mutable_f64s() : nullptr;
  auto* i64 = spkv_src->has_i64s() ? spkv_src->mutable_i64s() : nullptr;
  CopyPBListToNDArray(sp_kv_ptr->values_, val_dtype, feat_offset, 0, f32, f64,
                      i64, nullptr);
}

SparseKFeatureFiller::SparseKFeatureFiller(
    std::shared_ptr<SparseKFeatureArray>& array_to_fill, int index_offset,
    int feat_offset, int max_dim,
    agl::proto::graph_feature::Features_SparseKFeatures* spk_src,
    AGLDType key_type)
    : array(array_to_fill),
      index_offset(index_offset),
      feat_offset(feat_offset),
      max_dim(max_dim),
      spk_src(spk_src),
      key_dtype(key_type) {}

void SparseKFeatureFiller::Fill() {
  auto& sp_k_ptr = array->GetFeatureArray();
  // step 1: index_offset + 1 to adapt to standard csr
  CopyIndToNDArray(sp_k_ptr->ind_offset_, spk_src->lens(), index_offset + 1, 0,
                   spk_src->lens().value_size(), feat_offset);
  // step 2: keys
  // todo: should check max dim ?
  auto* i64 = spk_src->has_i64s() ? spk_src->mutable_i64s() : nullptr;
  auto* raw = spk_src->has_raws() ? spk_src->mutable_raws() : nullptr;
  CopyPBListToNDArray(sp_k_ptr->keys_, key_dtype, feat_offset, 0, nullptr,
                      nullptr, i64, raw);
}

CSRFiller::CSRFiller(std::shared_ptr<CSR>& dst, int ind_position_offset,
                     int indices_position_offset, int n1_content_offset,
                     int n2_content_offset,
                     agl::proto::graph_feature::Edges_CSR* src)
    : adj(dst),
      ind_position_offset(ind_position_offset),
      indices_position_offset(indices_position_offset),
      n1_content_offset(n1_content_offset),
      n2_content_offset(n2_content_offset),
      csr_pb(src) {}

void CSRFiller::Fill() {
  // ind
  auto& ind_ptr = adj->ind_;
  CopyIndToNDArray(ind_ptr, csr_pb->indptr(), ind_position_offset + 1, 0,
                   csr_pb->indptr().value_size(), indices_position_offset);
  auto& indices = adj->indices_;
  CopyIndToNDArray(indices, csr_pb->nbrs_indices(), indices_position_offset, 0,
                   csr_pb->nbrs_indices().value_size(), n2_content_offset);
}

}  // namespace agl