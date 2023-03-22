package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.filter_exp.ArithmeticCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CmpExpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.sampler.Sampler;
import com.alipay.alps.flatv3.sampler.SamplerFactory;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.LogicOp;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class FilterAndSampler {
    public String filterCond = null;
    public LogicExps logicExps = null;
    public String sampleCond = null;
    public String otherOutputSchema;

    public FilterAndSampler(String otherOutputSchema, String filterCond, String sampleCond) {
        this.otherOutputSchema = otherOutputSchema.replaceAll("\\s", "");
        this.filterCond = filterCond;
        logicExps = new FilterConditionParser().parseFilterCondition(filterCond);
        this.sampleCond = sampleCond;
    }


    private IndexResult search(LogicExps logicExps, Map<String, BaseIndex> indexesMap, List<Object> seedValues) throws Exception {
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

        Stack<IndexResult> indexResultStack = new Stack<>();
        for (int i = 0; i < logicExps.getExpRPNCount(); i++) {
            LogicExps.ExpOrOp expOrOp = logicExps.getExpRPN(i);
            IndexResult indexResult = null;
            if (expOrOp.getDataCase() == LogicExps.ExpOrOp.DataCase.OP) {
                LogicOp logicOp = expOrOp.getOp();
                IndexResult indexResult1 = indexResultStack.pop();
                IndexResult indexResult2 = indexResultStack.pop();
                if (logicOp == LogicOp.AND) {
                    indexResult = indexResult1.intersection(indexResult2);
                } else if (logicOp == LogicOp.OR) {
                    indexResult = indexResult1.union(indexResult2);
                }
            } else {
                CmpExp cmpExp = expOrOp.getExp();
                CmpExpWrapper expWrapper = (cmpExp.getOp() == CmpOp.IN || cmpExp.getOp() == CmpOp.NOT_IN) ?
                        new CategoryCmpWrapper(cmpExp) : new ArithmeticCmpWrapper(cmpExp);
                String indexColumn = expWrapper.getIndexColumn();
                BaseIndex index = indexesMap.get(indexColumn);
                indexResult = index.search(expWrapper, inputVariables);
            }
            indexResultStack.add(indexResult);
        }
        return indexResultStack.pop();
    }

    public List<List<Integer>> process(List<String> seeds, List<List<Object>> seedAttrs, List<BaseIndex> indexList,
                                       Map<String, Object> frontierValues, List<Object> neigborAttrs) throws Exception {
        Map<String, BaseIndex> indexesMap = new HashMap<>();
        for (int i = 0; i < indexList.size(); i++) {
            indexesMap.put(indexList.get(i).getIndexColumn(), indexList.get(i));
        }
        List<List<Integer>> neighborList = new ArrayList<>();
        SampleCondition sampleCondition = new SampleCondition(sampleCond);
        BaseIndex sampleIndex = indexesMap.get(sampleCondition.key);
        Sampler sampler = SamplerFactory.createSampler(sampleCondition, sampleIndex);
        for (int i = 0; i < seeds.size(); i++) {
            String seedId = seeds.get(i);
            IndexResult indexResult = search(logicExps, indexesMap, seedAttrs.get(i));
            List<Integer> neighborIndices = sampler.sample(indexResult);
            neighborList.add(neighborIndices);
        }
        return neighborList;
    }
}
