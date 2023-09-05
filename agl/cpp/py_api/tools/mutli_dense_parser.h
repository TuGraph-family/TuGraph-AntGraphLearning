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
#ifndef AGL_MUTLI_DENSE_PARSER_H
#define AGL_MUTLI_DENSE_PARSER_H

#include "base_data_structure/dtype.h"

namespace agl {

// parse int, int64, float, double from string
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

  void Parse();

 private:
  std::string token_;
  char sep_;  // todo now only support char as separator
  int dim_;
  int element_i_;
  void* dst_data_;
  AGLDType dtype_;
};

}  // namespace agl

#endif  // AGL_MUTLI_DENSE_PARSER_H
