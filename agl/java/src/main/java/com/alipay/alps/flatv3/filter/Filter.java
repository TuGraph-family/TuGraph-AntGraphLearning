package com.alipay.alps.flatv3.filter;

import com.alipay.alps.flatv3.filter.parser.AbstractCmpWrapper;
import com.alipay.alps.flatv3.filter.parser.CmpWrapperFactory;
import com.alipay.alps.flatv3.filter.parser.FilterConditionParser;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.LogicOp;
import com.antfin.agl.proto.sampler.VariableSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public Map<VariableSource, Set<String>> getReferSourceAndColumns() {
        Map<VariableSource, Set<String>> referColumns = new HashMap<>();
        for (LogicExps.ExpOrOp expOrOp : logicExps.getExpRPNList()) {
            if (!expOrOp.hasExp()) {
                continue;
            }
            CmpExp cmpExp = expOrOp.getExp();
            List<Element> elements = new ArrayList<>(cmpExp.getLhsRPNList());
            elements.addAll(cmpExp.getRhsRPNList());
            for (Element element : elements) {
                if (element.hasVar()) {
                    Element.Variable variable = element.getVar();
                    VariableSource variableSource = variable.getSource();
                    if (!referColumns.containsKey(variableSource)) {
                        referColumns.put(variableSource, new HashSet<String>());
                    }
                    referColumns.get(variableSource).add(variable.getName());
                }
            }
        }
        return referColumns;
    }

    /*
     * @param seedValues: values of one seed
     * @param indexesMap: a map of indexes
     * @return: neighbor indices conforming to the filter condition
     */
    public AbstractResult filter(Map<String, Element.Number> seedVariableMap, HeteroDataset neighborValues, Map<String, BaseIndex> indexesMap) throws Exception {
        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        inputVariables.put(VariableSource.SEED, seedVariableMap);
        // in case of empty filter condition, we just return all neighbors
        if (logicExps.getExpRPNCount() == 0) {
            List<RangeUnit> ranges = new ArrayList<>();
            ranges.add(new RangeUnit(0, neighborValues.getArraySize() - 1));
            return new RangeResult(null, ranges);
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
                indexResult = index.search(expWrapper, inputVariables, neighborValues);
            }
            indexResultStack.add(indexResult);
        }
        return indexResultStack.pop();
    }
}
