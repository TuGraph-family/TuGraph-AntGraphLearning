package com.alipay.alps.flatv3.filter_exp;

import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CompareExpTest {

    @Test
    public void testEvalEqualExp() {
        String filterCond = "index.time - (seed.1 - seed.2 * seed.3) / index.time2 == 94.6";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> indexVariableMap = new HashMap<>();
        indexVariableMap.put("time", Element.Number.newBuilder().setF(100.0F).build());
        indexVariableMap.put("time2", Element.Number.newBuilder().setF(10.0F).build());
        inputVariables.put(VariableSource.INDEX, indexVariableMap);
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        seedVariableMap.put("1", Element.Number.newBuilder().setF(60.0F).build());
        seedVariableMap.put("2", Element.Number.newBuilder().setF(2F).build());
        seedVariableMap.put("3", Element.Number.newBuilder().setF(3.0F).build());
        inputVariables.put(VariableSource.SEED, seedVariableMap);
        boolean result = new ArithmeticCmpWrapper(cmpExp).eval(inputVariables);
        Assert.assertTrue(result);
    }

   @Test
   public void testEvalLargerThanExp() {
       String filterCond = "index.time - seed.1 / index.time2 >=  10 * seed.2";
       FilterConditionParser filterConditionParser = new FilterConditionParser();
       LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
       CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
       Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
       Map<String, Element.Number> indexVariableMap = new HashMap<>();
       indexVariableMap.put("time", Element.Number.newBuilder().setF(100.0F).build());
       indexVariableMap.put("time2", Element.Number.newBuilder().setF(10.0F).build());
       inputVariables.put(VariableSource.INDEX, indexVariableMap);
       Map<String, Element.Number> seedVariableMap = new HashMap<>();
       seedVariableMap.put("1", Element.Number.newBuilder().setF(60.0F).build());
       seedVariableMap.put("2", Element.Number.newBuilder().setF(2F).build());
       inputVariables.put(VariableSource.SEED, seedVariableMap);
       boolean result = new ArithmeticCmpWrapper(cmpExp).eval(inputVariables);
       Assert.assertTrue(result);
   }

   @Test
   public void testEvalTypeEqualExp() {
       String filterCond = "index.type = seed.1";
       FilterConditionParser filterConditionParser = new FilterConditionParser();
       LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
       CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
       Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
       Map<String, Element.Number> indexVariableMap = new HashMap<>();
       indexVariableMap.put("type", Element.Number.newBuilder().setS("item").build());
       inputVariables.put(VariableSource.INDEX, indexVariableMap);
       Map<String, Element.Number> seedVariableMap = new HashMap<>();
       seedVariableMap.put("1", Element.Number.newBuilder().setS("item").build());
       inputVariables.put(VariableSource.SEED, seedVariableMap);
       boolean result = new ArithmeticCmpWrapper(cmpExp).eval(inputVariables);
       Assert.assertTrue(result);
   }

   @Test
   public void testEvalInCategoryExp() {
       String filterCond = "seed.1 in (user, item)";
       FilterConditionParser filterConditionParser = new FilterConditionParser();
       LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
       CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
       Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
       Map<String, Element.Number> seedVariableMap = new HashMap<>();
       seedVariableMap.put("1", Element.Number.newBuilder().setS("item").build());
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
       String filterCond = "seed.1 not in (user, item)";
       FilterConditionParser filterConditionParser = new FilterConditionParser();
       LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
       CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
       Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
       Map<String, Element.Number> seedVariableMap = new HashMap<>();
       seedVariableMap.put("1", Element.Number.newBuilder().setS("item").build());
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
