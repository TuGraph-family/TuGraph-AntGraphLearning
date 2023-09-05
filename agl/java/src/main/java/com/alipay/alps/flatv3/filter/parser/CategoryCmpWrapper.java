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
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CategoryCmpWrapper extends AbstractCmpWrapper {

  public CategoryCmpWrapper(CmpExp cmpExp) {
    super(cmpExp);
  }

  public boolean eval(Map<VariableSource, Map<String, Element.Number>> inputVariables) {
    String left = getStringValue(cmpExp.getLhsRPN(0), inputVariables);
    if (cmpExp.getOp() == CmpOp.IN || cmpExp.getOp() == CmpOp.NOT_IN) {
      Set<String> rightTypes = new HashSet<>();
      for (int i = 0; i < cmpExp.getRhsRPNCount(); i++) {
        rightTypes.add(getStringValue(cmpExp.getRhsRPN(i), inputVariables));
      }
      return (cmpExp.getOp() == CmpOp.IN) == rightTypes.contains(left);
    } else {
      String right = getStringValue(cmpExp.getRhsRPN(0), inputVariables);
      return compare(left, right, cmpExp.getOp());
    }
  }

  // TODO: support more types, like long, etc.
  public String getStringValue(Element element,
      Map<VariableSource, Map<String, Element.Number>> inputVariables) {
    if (element.getSymbolCase() == Element.SymbolCase.NUM) {
      return element.getNum().getS();
    } else {
      VariableSource sourceType = element.getVar().getSource();
      String name = element.getVar().getName();
      return inputVariables.get(sourceType).get(name).getS();
    }
  }
}
