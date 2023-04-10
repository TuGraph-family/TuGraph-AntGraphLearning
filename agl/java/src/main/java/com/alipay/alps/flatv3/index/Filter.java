package com.alipay.alps.flatv3.index;

import com.antfin.agl.proto.sampler.LogicExps;
import com.alipay.alps.flatv3.filter_exp.ArithmeticCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.AbstactCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicOp;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Filter {
    private LogicExps logicExps = null;
    private Map<String, BaseIndex> indexesMap = null;

    /*
     * @param indexesMap: indexName -> index
     * @param filterCond: filter condition
     */
    public Filter(Map<String, BaseIndex> indexesMap, String filterCond) {
        this.indexesMap = indexesMap;
        logicExps = new FilterConditionParser().parseFilterCondition(filterCond);
    }

    /* 
     * @param seedValues: values of one seed
     * @return: neighbor indices conforming to the filter condition
     */
    public AbstractIndexResult filter(List<Object> seedValues) throws Exception {
        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        for (int i = 0; i < seedValues.size(); i++) {
            Object seedValue = seedValues.get(i);
            if (seedValue instanceof Float) {
                seedVariableMap.put(String.valueOf(i + 1), Element.Number.newBuilder().setF((Float) seedValue).build());
            } else if (seedValue instanceof Long) {
                seedVariableMap.put(String.valueOf(i + 1), Element.Number.newBuilder().setI((Long) seedValue).build());
            } else if (seedValue instanceof String) {
                seedVariableMap.put(String.valueOf(i + 1), Element.Number.newBuilder().setS((String) seedValue).build());
            }
        }
        inputVariables.put(VariableSource.SEED, seedVariableMap);
        if (logicExps.getExpRPNCount() == 0) {
            return indexesMap.get(null).search(null, inputVariables);
        }
        Stack<AbstractIndexResult> indexResultStack = new Stack<>();
        for (int i = 0; i < logicExps.getExpRPNCount(); i++) {
            LogicExps.ExpOrOp expOrOp = logicExps.getExpRPN(i);
            AbstractIndexResult indexResult = null;
            if (expOrOp.getDataCase() == LogicExps.ExpOrOp.DataCase.OP) {
                LogicOp logicOp = expOrOp.getOp();
                AbstractIndexResult indexResult1 = indexResultStack.pop();
                AbstractIndexResult indexResult2 = indexResultStack.pop();
                if (logicOp == LogicOp.AND) {
                    indexResult = indexResult1.join(indexResult2);
                } else if (logicOp == LogicOp.OR) {
                    indexResult = indexResult1.union(indexResult2);
                }
            } else {
                CmpExp cmpExp = expOrOp.getExp();
                AbstactCmpWrapper expWrapper = (cmpExp.getOp() == CmpOp.IN || cmpExp.getOp() == CmpOp.NOT_IN) ?
                        new CategoryCmpWrapper(cmpExp) : new ArithmeticCmpWrapper(cmpExp);
                String indexColumn = expWrapper.getIndexColumn();
                BaseIndex index = indexesMap.get(indexColumn);
                indexResult = index.search(expWrapper, inputVariables);
            }
            indexResultStack.add(indexResult);
        }
        return indexResultStack.pop();
    }
}
