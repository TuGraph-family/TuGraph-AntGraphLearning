package com.alipay.alps.flatv3.filter;

import com.alipay.alps.flatv3.filter.parser.AbstractCmpWrapper;
import com.alipay.alps.flatv3.filter.parser.CmpWrapperFactory;
import com.alipay.alps.flatv3.filter.parser.FilterConditionParser;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.LogicOp;
import com.antfin.agl.proto.sampler.VariableSource;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Filter implements Serializable {
    private LogicExps logicExps = null;

    /*
     * Construct a filter.
     * @param filterCond: filter condition string
     */
    public Filter(String filterCond) throws Exception {
        logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    }

    /*
     * @param seedValues: values of one seed
     * @param indexesMap: a map of indexes
     * @return: neighbor indices conforming to the filter condition
     */
    public AbstractResult filter(int seedIndex, HeteroDataset seedValues, HeteroDataset neighborValues, Map<String, BaseIndex> indexesMap) throws Exception {
        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> seedVariableMap = seedValues.fillVariables(seedIndex);
        inputVariables.put(VariableSource.SEED, seedVariableMap);
        // in case of empty filter condition, we are using the base_index, NO_FILTER is the index column
        if (logicExps.getExpRPNCount() == 0) {
            return indexesMap.get(IndexFactory.NO_FILTER).search(null, inputVariables, neighborValues);
        }
        Stack<AbstractResult> indexResultStack = new Stack<>();
        for (int i = 0; i < logicExps.getExpRPNCount(); i++) {
            LogicExps.ExpOrOp expOrOp = logicExps.getExpRPN(i);
            AbstractResult indexResult = null;
            if (expOrOp.getDataCase() == LogicExps.ExpOrOp.DataCase.OP) {
                LogicOp logicOp = expOrOp.getOp();
                AbstractResult indexResult1 = indexResultStack.pop();
                AbstractResult indexResult2 = indexResultStack.pop();
                if (logicOp == LogicOp.AND) {
                    indexResult = indexResult1.join(indexResult2);
                } else if (logicOp == LogicOp.OR) {
                    indexResult = indexResult1.union(indexResult2);
                }
            } else {
                CmpExp cmpExp = expOrOp.getExp();
                AbstractCmpWrapper expWrapper = CmpWrapperFactory.createCmpWrapper(cmpExp);
                String indexColumn = expWrapper.getIndexColumn();
                BaseIndex index = indexesMap.get(indexColumn);
                if (indexColumn.compareToIgnoreCase("time") == 0) {
                    index = (RangeIndex) index;
                }
                indexResult = index.search(expWrapper, inputVariables, neighborValues);
            }
            indexResultStack.add(indexResult);
        }
        return indexResultStack.pop();
    }
}
