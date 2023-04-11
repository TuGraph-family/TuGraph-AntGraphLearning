package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.AbstractCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.ArithmeticCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.LogicOp;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Filter {
    private LogicExps logicExps = null;

    /*
     * Construct a filter.
     * @param indexMetas: a list of index metas, it will introduce multiple indexes
     * @param filterCond: filter condition
     * @param neighborDataset: neighbor dataset
     */
    public Filter(List<String> indexMetas, String filterCond, NeighborDataset neighborDataset) throws Exception {
        if (indexMetas == null || indexMetas.size() == 0) {
            IndexFactory.createIndex("", neighborDataset);
        } else {
            for (String indexMeta : indexMetas) {
                IndexFactory.createIndex(indexMeta, neighborDataset);
            }
        }
        logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    }

    private BaseIndex getIndex(String indexColumn) {
        if (!IndexFactory.indexesMap.containsKey(indexColumn)) {
            throw new RuntimeException("Index not found: " + indexColumn);
        }
        return IndexFactory.indexesMap.get(indexColumn);
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
        // in case of empty filter condition, we are using the base_index, NO_FILTER is the index column
        if (logicExps.getExpRPNCount() == 0) {
            return getIndex(IndexFactory.NO_FILTER).search(null, inputVariables);
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
                AbstractCmpWrapper expWrapper = (cmpExp.getOp() == CmpOp.IN || cmpExp.getOp() == CmpOp.NOT_IN) ?
                        new CategoryCmpWrapper(cmpExp) : new ArithmeticCmpWrapper(cmpExp);
                String indexColumn = expWrapper.getIndexColumn();
                BaseIndex index = getIndex(indexColumn);
                indexResult = index.search(expWrapper, inputVariables);
            }
            indexResultStack.add(indexResult);
        }
        return indexResultStack.pop();
    }
}
