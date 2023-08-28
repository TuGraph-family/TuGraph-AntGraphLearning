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
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CompareExpTest {

  @Test
  public void testEvalEqualExp() {
    String filterCond = "index.time - (seed.time1 - seed.time2 * seed.time3) / index.time2 == 94.6";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
    Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
    Map<String, Element.Number> indexVariableMap = new HashMap<>();
    indexVariableMap.put("time", Element.Number.newBuilder().setF(100.0F).build());
    indexVariableMap.put("time2", Element.Number.newBuilder().setF(10.0F).build());
    inputVariables.put(VariableSource.INDEX, indexVariableMap);
    Map<String, Element.Number> seedVariableMap = new HashMap<>();
    seedVariableMap.put("time1", Element.Number.newBuilder().setF(60.0F).build());
    seedVariableMap.put("time2", Element.Number.newBuilder().setF(2F).build());
    seedVariableMap.put("time3", Element.Number.newBuilder().setF(3.0F).build());
    inputVariables.put(VariableSource.SEED, seedVariableMap);
    boolean result = new ArithmeticCmpWrapper(cmpExp).eval(inputVariables);
    Assert.assertTrue(result);
  }

  @Test
  public void testEvalLargerThanExp() {
    String filterCond = "index.time - seed.time1 / index.time2 >=  10 * seed.time2";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
    Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
    Map<String, Element.Number> indexVariableMap = new HashMap<>();
    indexVariableMap.put("time", Element.Number.newBuilder().setF(100.0F).build());
    indexVariableMap.put("time2", Element.Number.newBuilder().setF(10.0F).build());
    inputVariables.put(VariableSource.INDEX, indexVariableMap);
    Map<String, Element.Number> seedVariableMap = new HashMap<>();
    seedVariableMap.put("time1", Element.Number.newBuilder().setF(60.0F).build());
    seedVariableMap.put("time2", Element.Number.newBuilder().setF(2F).build());
    inputVariables.put(VariableSource.SEED, seedVariableMap);
    boolean result = new ArithmeticCmpWrapper(cmpExp).eval(inputVariables);
    Assert.assertTrue(result);
  }

  @Test
  public void testEvalTypeEqualExp() {
    String filterCond = "index.type = seed.type";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
    Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
    Map<String, Element.Number> indexVariableMap = new HashMap<>();
    indexVariableMap.put("type", Element.Number.newBuilder().setS("item").build());
    inputVariables.put(VariableSource.INDEX, indexVariableMap);
    Map<String, Element.Number> seedVariableMap = new HashMap<>();
    seedVariableMap.put("type", Element.Number.newBuilder().setS("item").build());
    inputVariables.put(VariableSource.SEED, seedVariableMap);
    boolean result = new CategoryCmpWrapper(cmpExp).eval(inputVariables);
    Assert.assertTrue(result);
  }

  @Test
  public void testEvalInCategoryExp() {
    String filterCond = "seed.type in (user, item)";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
    Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
    Map<String, Element.Number> seedVariableMap = new HashMap<>();
    seedVariableMap.put("type", Element.Number.newBuilder().setS("item").build());
    inputVariables.put(VariableSource.SEED, seedVariableMap);
    boolean result = false;
    try {
      result = new CategoryCmpWrapper(cmpExp).eval(inputVariables);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue(result);
  }


  @Test
  public void testEvalNotInCategoryExp() {
    String filterCond = "seed.type not in (user, item)";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
    Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
    Map<String, Element.Number> seedVariableMap = new HashMap<>();
    seedVariableMap.put("type", Element.Number.newBuilder().setS("item").build());
    inputVariables.put(VariableSource.SEED, seedVariableMap);
    boolean result = false;
    try {
      result = new CategoryCmpWrapper(cmpExp).eval(inputVariables);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertFalse(result);
  }
}
