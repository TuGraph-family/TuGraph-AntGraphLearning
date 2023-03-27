package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CmpExpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class HashIndex extends BaseIndex {
    private Map<String, Range> typeRanges;
    public HashIndex(String indexMeta, NeighborDataset neighborDataset) {
        super(indexMeta, neighborDataset);
    }

    @Override
    protected void buildIndex() {
        Map<String, List<Integer>> typeIndexes = new HashMap<>();
        List<String> types = neighborDataset.getAttributeList(getIndexColumn());
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            if (!typeIndexes.containsKey(type)) {
                typeIndexes.put(type, new ArrayList<>());
            }
            typeIndexes.get(type).add(i);
        }
        originIndex = new Integer[types.size()];
        this.typeRanges = new HashMap<>();
        int count = 0;
        for (String type : typeIndexes.keySet()) {
            List<Integer> indices = typeIndexes.get(type);
            Range range = new Range(count, -1);
            for (int index : indices) {
                originIndex[count++] = index;
            }
            range.setHigh(count - 1);
            this.typeRanges.put(type, range);
        }
    }

    @Override
    public IndexResult search(CmpExpWrapper cmpExpWrapper, Map<VariableSource, Map<java.lang.String, Element.Number>> inputVariables) throws Exception {
        List<Range> ranges = searchType((CategoryCmpWrapper)cmpExpWrapper, inputVariables);
        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, ranges);
        return rangeIndexResult;
    }

    private List<Range> searchType(CategoryCmpWrapper cateCmpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        String indexColumn = getIndexColumn();
        List<Range> ansList = new ArrayList<>();
        Map<String, Element.Number> indexVariableMap = new HashMap<>();
        indexVariableMap.put(indexColumn, null);
        inputVariables.put(VariableSource.INDEX, indexVariableMap);
        for (String type : this.typeRanges.keySet()) {
            inputVariables.get(VariableSource.INDEX).put(indexColumn, Element.Number.newBuilder().setS(type).build());
            if (cateCmpWrapper.eval(inputVariables)) {
                ansList.add(this.typeRanges.get(type));
            }
        }
        return ansList;
    }

    public static void main(String[] args) throws Exception {
        List<Integer> ids = new ArrayList<>();
        List<String> type = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(i);
            type.add("node_" + String.valueOf(i%3));
        }
        NeighborDataset<Integer> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("node_type", type);
        HashIndex rangeIndex = new HashIndex("hash_index:node_type:string", neighborDataset);

        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        String filterCond = "index.node_type in (node_1, node_2)";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        System.out.println("----cmpExp: " + cmpExp);
        System.out.println(rangeIndex.search(new CategoryCmpWrapper(cmpExp), inputVariables));
    }
}
