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
#ifndef AGL_DTYPE_H
#define AGL_DTYPE_H

#include <stdint.h>

#include <limits>

#include "common/safe_check.h"

namespace agl {
// data type of all kind ids for output: int64_t
// such as index in adj matrix
#define IdDType int64_t

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

inline int GetDtypeSize(const AGLDType& type) {
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
      AGL_CHECK(false) << " Not supported AGLDType:" << type;
      return 0;
  }
}

template <class T>
AGLDType GetDTypeFromT() {
  if (std::is_same<T, std::string>::value) {
    return AGLDType::STR;
  } else if (std::is_same<T, uint64_t>::value) {
    return AGLDType::UINT64;
  } else if (std::is_same<T, int64_t>::value) {
    return AGLDType::INT64;
  } else if (std::is_same<T, int32_t>::value) {
    return AGLDType::INT32;
  } else if (std::is_same<T, uint32_t>::value) {
    return AGLDType::UINT32;
  } else if (std::is_same<T, int16_t>::value) {
    return AGLDType::INT16;
  } else if (std::is_same<T, uint16_t>::value) {
    return AGLDType::UINT16;
  } else if (std::is_same<T, double>::value) {
    return AGLDType::DOUBLE;
  } else if (std::is_same<T, float>::value) {
    return AGLDType::FLOAT;
  }
  AGL_CHECK(false) << "Can not get dtype";
}

}  // namespace agl

#endif  // AGL_DTYPE_H
