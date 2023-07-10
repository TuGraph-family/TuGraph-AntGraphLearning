#ifndef AGL_FEATURE_FILLER_H
#define AGL_FEATURE_FILLER_H

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
/**
 *  这个文件中的 filler 主要是不融合 case 下，
 *  通过 src 和 dst 的偏移量，把 pb 中的特征copy到连续内存块中
 */

#define COPY_DATA_WITH_REAL_TYPE(SRC, DST, SRC_BEGIN, DST_BEGIN, NUMS) \
  for (int copy_pb_values_i = 0; copy_pb_values_i < NUMS;              \
       copy_pb_values_i++) {                                           \
    int src_index = SRC_BEGIN + copy_pb_values_i;                      \
    int dst_index = DST_BEGIN + copy_pb_values_i;                      \
    DST[dst_index] = SRC->value(src_index);                            \
  }

void CopyPBListToNDArray(std::shared_ptr<NDArray>& nd_dst, AGLDType dtype,
                         int dst_offset, int src_offset,
                         agl::proto::graph_feature::FloatList* f32_list,
                         agl::proto::graph_feature::Float64List* f64_list,
                         agl::proto::graph_feature::Int64List* i64_list,
                         agl::proto::graph_feature::BytesList* str_list) {
  switch (dtype) {
    case AGLDType::FLOAT: {
      AGL_CHECK(f32_list != nullptr);
      auto* dst_ptr = nd_dst->Flat<float>();
      // std::cout << "f32_list size:" << f32_list->value_size() << "\n";
      COPY_DATA_WITH_REAL_TYPE(f32_list, dst_ptr, src_offset, dst_offset,
                               f32_list->value_size());
      /*std::cout << "dense float dst_ptr, row:" << nd_dst->GetRowNumber() << ", col:" <<  nd_dst-> GetColNumber() << " \n";
      int row_num = nd_dst->GetRowNumber();
      int col_num = nd_dst->GetColNumber();
      for(size_t i =0; i<row_num; ++i) {
        for(size_t j =0; j< col_num; ++j) {
          std::cout << " " << dst_ptr[i*col_num + j];
        }
        std:: cout << "\n";
      }*/
      break;
    }
    case AGLDType::DOUBLE: {
      AGL_CHECK(f64_list != nullptr);
      auto* dst_ptr = nd_dst->Flat<double>();
      COPY_DATA_WITH_REAL_TYPE(f64_list, dst_ptr, src_offset, dst_offset,
                               f64_list->value_size());
      break;
    }
    case AGLDType::INT64: {
      AGL_CHECK(i64_list != nullptr);
      auto* dst_ptr = nd_dst->Flat<int64_t>();
      COPY_DATA_WITH_REAL_TYPE(i64_list, dst_ptr, src_offset, dst_offset,
                               i64_list->value_size());
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
    // ind offset 需要加上一定的偏移量, 因为原始的就是偏移量
    //std::cout << "CopyIndToNDArray  src index:" << src_index << "dst:" << dst_index \
    //          << ", content:" << i64_src.value(src_index) << "value size:" << i64_src.value_size() << "content now:" <<i64_src.value(src_index) + content_offset << "\n";
    data[dst_index] = i64_src.value(src_index) + content_offset;
  }
}

class Filler {
 public:
  virtual ~Filler() = default;
  virtual void Fill() = 0;
};

class DenseFeatureFiller : public Filler{
 public:
  DenseFeatureFiller(std::shared_ptr<DenseFeatureArray>& array_to_fill,
                     int offset,
                     agl::proto::graph_feature::Features_DenseFeatures* df_src,
                     AGLDType dtype)
      : array(array_to_fill), offset(offset), df_src(df_src), dtype(dtype) {
    //std::cout << "dense fill construct f32_list size:" << df_src->mutable_f32s()->value_size() << "\n";
  }

  ~DenseFeatureFiller() override = default;
  void Fill() override {
    //std::cout << "dense fill begin, offset:" << offset <<" f32_list at fill:"<< df_src->mutable_f32s()->value_size() << ", dtype:" << dtype<< "\n";
    auto *f32 = df_src->has_f32s() ? df_src->mutable_f32s() : nullptr;
    auto *f64 = df_src->has_f64s() ? df_src->mutable_f64s() : nullptr;
    auto *i64 = df_src->has_i64s() ? df_src->mutable_i64s() : nullptr;
    auto *raws = df_src->has_raws() ? df_src->mutable_raws() : nullptr;
    CopyPBListToNDArray(array->GetFeatureArray(), dtype, offset, 0,
                        f32, f64,
                        i64, raws);
  }

 private:
  std::shared_ptr<DenseFeatureArray> array;
  int offset;
  agl::proto::graph_feature::Features_DenseFeatures* df_src;
  AGLDType dtype;
};

class SparseKVFeatureFiller : public Filler{
 public:
  SparseKVFeatureFiller(
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
        val_dtype(val_type) {
    /*std::cout << "spkv fill construct lens size:" << spkv_src->lens().value_size() << "\n";
    for(size_t m =0; m < spkv_src->lens().value_size(); ++m) {
      std:: cout << " " << spkv_src->lens().value(m);
    }
    std::cout << "\n";*/
  }
  ~SparseKVFeatureFiller() override = default;
  void Fill() override{
    //std::cout << "sparse kv fill begin, feat_offset:"<<feat_offset << ",index_offset:" <<index_offset <<"\n";
    auto& sp_kv_ptr = array->GetFeatureArray();
    // step 1: ind offset 需要加上一定的偏移量, 因为原始的就是偏移量
    // todo 和 torch 兼容的话，需要仔细考虑是否 content_offset = index_offset +
    // 1 还是怎么做，目前的做法应该没法直接 torch from csr
    /*std::cout << "spkv config, lens:" << spkv_src->lens().value_size() << "\n";
    for(size_t m =0; m < spkv_src->lens().value_size(); ++m) {
      std:: cout << " " << spkv_src->lens().value(m);
    }
    std::cout << "\n";*/
    // ind offset 需要 + 1
    CopyIndToNDArray(sp_kv_ptr->ind_offset_, spkv_src->lens(),
                     index_offset+1, 0, spkv_src->lens().value_size(),
                     feat_offset);
    // step 2: keys
    // todo: should check max dim ?
    CopyPBListToNDArray(sp_kv_ptr->keys_, key_dtype, feat_offset, 0, nullptr,
                        nullptr, spkv_src->mutable_keys(), nullptr);
    // step 3: values
    auto *f32 = spkv_src->has_f32s() ? spkv_src->mutable_f32s() : nullptr;
    auto *f64 = spkv_src->has_f64s() ? spkv_src->mutable_f64s() : nullptr;
    auto *i64 = spkv_src->has_i64s() ? spkv_src->mutable_i64s() : nullptr;
    CopyPBListToNDArray(sp_kv_ptr->values_, val_dtype, feat_offset, 0,
                        f32, f64,
                        i64, nullptr);
  }

 private:
  std::shared_ptr<SparseKVFeatureArray> array;
  int index_offset;
  int feat_offset;
  int max_dim;
  agl::proto::graph_feature::Features_SparseKVFeatures* spkv_src;
  AGLDType key_dtype;
  AGLDType val_dtype;
};

class SparseKFeatureFiller : public Filler{
 public:
  SparseKFeatureFiller(
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

  ~SparseKFeatureFiller() override = default;
  void Fill() override{
    //std::cout << "sparse k fill begin" << "\n";
    auto& sp_k_ptr = array->GetFeatureArray();
    // step 1: ind offset 需要加上一定的偏移量, 因为原始的就是偏移量
    // todo 和 torch 兼容的话，需要仔细考虑是否 content_offset = index_offset +
    // 1 还是怎么做，目前的做法应该没法直接 torch from csr
    CopyIndToNDArray(sp_k_ptr->ind_offset_, spk_src->lens(),
                     index_offset+1, 0, spk_src->lens().value_size(),
                     feat_offset);
    // step 2: keys
    // todo: should check max dim ?
    auto *i64 = spk_src->has_i64s() ? spk_src->mutable_i64s() : nullptr;
    auto *raw = spk_src->has_raws() ? spk_src->mutable_raws() : nullptr;
    CopyPBListToNDArray(sp_k_ptr->keys_, key_dtype, feat_offset, 0, nullptr,
                        nullptr, i64, raw);
  }

 private:
  std::shared_ptr<SparseKFeatureArray> array;
  int index_offset;
  int feat_offset;
  int max_dim;
  agl::proto::graph_feature::Features_SparseKFeatures* spk_src;
  AGLDType key_dtype;
};

class CSRFiller : public Filler{
 public:
  CSRFiller(std::shared_ptr<CSR>& dst, int ind_position_offset,
            int indices_position_offset, int n1_content_offset,
            int n2_content_offset, agl::proto::graph_feature::Edges_CSR* src)
      : adj(dst),
        ind_position_offset(ind_position_offset),
        indices_position_offset(indices_position_offset),
        n1_content_offset(n1_content_offset),
        n2_content_offset(n2_content_offset),
        csr_pb(src){}
  ~CSRFiller() override = default;
  void Fill() override{
    //std::cout << "csr fill begin. ind_position_offset:"<<ind_position_offset << ", indices_position_offset:"
    //          << indices_position_offset << ", n1_content_offset:" << n1_content_offset << ", n2_content_offset" << n2_content_offset<< "\n";
    // ind
    auto& ind_ptr = adj->ind_;
    CopyIndToNDArray(ind_ptr, csr_pb->indptr(), ind_position_offset+1, 0,
                     csr_pb->indptr().value_size(), indices_position_offset);
    auto& indices = adj->indices_;
    CopyIndToNDArray(indices, csr_pb->nbrs_indices(),
                     indices_position_offset, 0,
                     csr_pb->nbrs_indices().value_size(), n2_content_offset);
    // todo 考虑 data 字段，也即 edge index 字段是否要 fill ?
    //auto& data = adj->data_;
    //data = std::shared_ptr<NDArray>(nullptr);
  }

 private:
  std::shared_ptr<CSR> adj;
  // todo ind_position_offset 目前 n1_content_offset 是相等的，考虑是否去掉一个？
  int ind_position_offset; // dst node的offset
  int indices_position_offset; // edge 的 offset
  int n1_content_offset; // dst node 的 offset
  int n2_content_offset; // src node 的 offset
  agl::proto::graph_feature::Edges_CSR* csr_pb;
};
}  // namespace agl

#endif  // AGL_FEATURE_FILLER_H
