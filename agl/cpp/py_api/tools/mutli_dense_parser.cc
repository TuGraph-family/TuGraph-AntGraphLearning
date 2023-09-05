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

#include "py_api/tools/mutli_dense_parser.h"

#include <chrono>
#include <sstream>
#include <stdexcept>
#include <string>

namespace agl {
void StringToNumberParser::Parse() {
  std::stringstream iss2(token_);
  std::string num_str;
  int i = 0;
  while (std::getline(iss2, num_str, sep_)) {
    try {
      switch (dtype_) {
        case AGLDType::INT32: {
          int rs_i = std::stoi(num_str);
          auto* dst_ptr = reinterpret_cast<int*>(dst_data_);
          dst_ptr[element_i_ * dim_ + i] = rs_i;
          break;
        }
        case AGLDType::INT64: {
          int64_t rs_i = std::stol(num_str);
          auto* dst_ptr = reinterpret_cast<int64_t*>(dst_data_);
          dst_ptr[element_i_ * dim_ + i] = rs_i;
          break;
        }
        case AGLDType::FLOAT: {
          float rs_i = std::stof(num_str);
          auto* dst_ptr = reinterpret_cast<float*>(dst_data_);
          dst_ptr[element_i_ * dim_ + i] = rs_i;
          break;
        }
        case AGLDType::DOUBLE: {
          double rs_i = std::stod(num_str);
          auto* dst_ptr = reinterpret_cast<double*>(dst_data_);
          dst_ptr[element_i_ * dim_ + i] = rs_i;
          break;
        }
        default:
          AGL_CHECK(false) << "Not supported dtype, AGLDType::" << dtype_
                           << " str is:" << num_str;
      }
    } catch (const std::invalid_argument& e) {
      // dirty code.
      AGL_CHECK(false)
          << " Error: " << e.what()
          << "， maybe stoi/stol/stof/stod failed. dtype is AGLDType::"
          << dtype_ << ", str is:" << num_str << "\n";
    } catch (const std::string& s) {
      AGL_CHECK(false) << s << "\n";
    }
    i++;
  }
  AGL_CHECK_EQUAL(i, dim_);
}
}  // namespace agl