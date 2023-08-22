/**
 * Copyright 2023 AntGroup CO., Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.alipay.alps.flatv3.filter.parser;

import com.antfin.agl.proto.sampler.CmpExp;

public class CmpWrapperFactory {

  public static AbstractCmpWrapper createCmpWrapper(CmpExp cmpExp) {
    switch (cmpExp.getOp()) {
      case EQ:
      case NE:
      case GT:
      case GE:
      case LT:
      case LE:
        return new ArithmeticCmpWrapper(cmpExp);
      case IN:
      case NOT_IN:
        return new CategoryCmpWrapper(cmpExp);
      default:
        throw new RuntimeException("Unsupported cmp op: " + cmpExp.getOp());
    }
  }
}
