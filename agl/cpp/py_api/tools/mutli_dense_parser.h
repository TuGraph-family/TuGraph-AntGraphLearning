#ifndef AGL_MUTLI_DENSE_PARSER_H
#define AGL_MUTLI_DENSE_PARSER_H

#include <sstream>
#include <iostream>
#include <memory>
#include <string>
#include <chrono>
#include <stdexcept>

#include "base_data_structure/dtype.h"

namespace agl {

// 支持 int，int64, float, double 四种数据类型的parse
class StringToNumberParser {
 public:
  StringToNumberParser(std::string token, char sep, int dim, int element_i,
                       void* dst_data, AGLDType dtype)
      : token_(token),
        sep_(sep),
        dim_(dim),
        element_i_(element_i),
        dst_data_(dst_data),
        dtype_(dtype) {}

  void Parse() {
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
            // std::cout << ">>>> " << element_i_ * dim_ + i << " res is:" << rs_i << " str is:" << num_str << "\n";
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
        // todo 后面需要把 AGL_CHECK 以及 c++ 层的异常转换为python层的异常，防止core 出现
      } catch(const std::invalid_argument &e) {
        // dirty code. 目前的原因是 转换失败报错不够明显
        AGL_CHECK(false) << " Error: " << e.what() << "， maybe stoi/stol/stof/stod failed. dtype is AGLDType::"<< dtype_ << ", str is:" << num_str << "\n";
      } catch(const std::string& s) {
        AGL_CHECK(false) << s << "\n";
      }
      i++;
    }
    AGL_CHECK_EQUAL(i, dim_);
  }

 private:
  std::string token_;
  char sep_;  // todo now only support char as separator
  int dim_;
  int element_i_;
  void* dst_data_;
  AGLDType dtype_;
};

}

#endif  // AGL_MUTLI_DENSE_PARSER_H
