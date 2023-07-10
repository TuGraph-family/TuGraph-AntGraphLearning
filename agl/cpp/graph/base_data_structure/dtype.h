#ifndef AGL_DTYPE_H
#define AGL_DTYPE_H

#include <limits>
#include <stdint.h>
#include "common/safe_check.h"

namespace agl {
enum AGLDType : int {
  UNKNOWN = 0,
  INT8 = 1,
  UINT16 = 2,
  INT16 = 3,
  UINT32 = 4,
  INT32 = 5,
  UINT64 = 6,
  INT64 = 7,
  FLOAT = 8,
  DOUBLE = 9,
  STR = 10,
  UINT8 = 11,
  BF16 = 12,
  FP16 = 13,
  DType_INT_MIN_SENTINEL_DO_NOT_USE_ = std::numeric_limits<int32_t>::min(),
  DType_INT_MAX_SENTINEL_DO_NOT_USE_ = std::numeric_limits<int32_t>::max()
};

// id 使用的数据类型, e.g.:
// mapping 后的数据类型
// adj 中 index 使用的数据类型
// 特征索引使用的数据类型
// 对应的 AGLDType 是 AGLDType::INT64
#define IdDType int64_t


inline int GetDtypeSize(const AGLDType& type){
  switch (type) {
    case INT8:
      return sizeof(int8_t);
    case UINT8:
      return sizeof(uint8_t);
    case UINT16:
      return sizeof(uint16_t);
    case INT16:
      return sizeof(int16_t);
    case UINT32:
      return sizeof(uint32_t);
    case INT32:
      return sizeof(int32_t);
    case UINT64:
      return sizeof(uint64_t);
    case INT64:
      return sizeof(int64_t);
    case FLOAT:
      return sizeof(float);
    case DOUBLE:
      return sizeof(double);
    default:
      // todo raise error and log
      AGL_CHECK(false) << " Not supported AGLDType:" << type;
      return 0;
  }
}

template<class T>
AGLDType GetDTypeFromT() {
  if (std::is_same<T, std::string>::value) {
    return AGLDType::STR;
  }
  else if (std::is_same<T, uint64_t>::value) {
    return AGLDType::UINT64;
  }
  else if (std::is_same<T, int64_t>::value) {
    return AGLDType::INT64;
  }
  else if (std::is_same<T, int32_t>::value) {
    return AGLDType::INT32;
  }
  else if (std::is_same<T, uint32_t>::value) {
    return AGLDType::UINT32;
  }
  else if (std::is_same<T, int16_t>::value) {
    return AGLDType::INT16;
  }
  else if (std::is_same<T, uint16_t>::value) {
    return AGLDType::UINT16;
  }
  else if (std::is_same<T, double>::value) {
      return AGLDType::DOUBLE;
  }
  else if (std::is_same<T, float>::value) {
    return AGLDType::FLOAT;
  }
  AGL_CHECK(false) << "Can not get dtype";
}

}

#endif  // AGL_DTYPE_H
