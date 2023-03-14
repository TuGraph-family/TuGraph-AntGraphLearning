package com.alipay.alps.flatv3.filter_exp;

import com.antfin.ai.alps.graph.flat.sample.CmpExp;
import com.antfin.ai.alps.graph.flat.sample.Element;
import com.antfin.ai.alps.graph.flat.sample.LogicExps;
import com.antfin.ai.alps.graph.flat.sample.VariableSource;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CompareExpTest {

    @Test
    public void testEvalDoubleCompareExp() {
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
        boolean result = CompareExpUtil.evalDoubleCompareExp(cmpExp, inputVariables);
        Assert.assertTrue(result);
    }

    @Test
    public void testEvalDoubleCompareExp2() {
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
        boolean result = CompareExpUtil.evalDoubleCompareExp(cmpExp, inputVariables);
        Assert.assertTrue(result);
    }

    @Test
    public void testEvalStringCompareExp() {
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
        boolean result = CompareExpUtil.evalStringCompareExp(cmpExp, inputVariables);
        Assert.assertTrue(result);
    }

    @Test
    public void testEvalStringCompareExp2() {
        String filterCond = "seed.1 < user";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        seedVariableMap.put("1", Element.Number.newBuilder().setS("item").build());
        inputVariables.put(VariableSource.SEED, seedVariableMap);
        boolean result = CompareExpUtil.evalStringCompareExp(cmpExp, inputVariables);
        Assert.assertTrue(result);
    }

    @Test
    public void testEvalCategoryExp() {
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
            result = CompareExpUtil.evalCategoryExp(cmpExp, inputVariables);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertTrue(result);
    }


    @Test
    public void testEvalCategoryExp2() {
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
            result = CompareExpUtil.evalCategoryExp(cmpExp, inputVariables);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertFalse(result);
    }

    @Test
    public void testEvalCategoryExp3() {
        String filterCond = "seed.1 not in (user, item)";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        seedVariableMap.put("1", Element.Number.newBuilder().setS("merchant").build());
        inputVariables.put(VariableSource.SEED, seedVariableMap);
        boolean result = false;
        try {
            result = CompareExpUtil.evalCategoryExp(cmpExp, inputVariables);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertTrue(result);
    }
}
