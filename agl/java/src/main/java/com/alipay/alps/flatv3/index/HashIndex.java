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

public class HashIndex<ID> extends BaseIndex<ID> {
    Map<String, Range> typeRanges = null;
    public HashIndex(String indexMeta) {
        super(indexMeta);
    }

    @Override
    public void buildIndex() {
        Map<String, List<Integer>> typeIndexes = new HashMap<>();
        List<String> types = this.stringAttributes.get(getIndexColumn());
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            if (!typeIndexes.containsKey(type)) {
                typeIndexes.put(type, new ArrayList<>());
            }
            typeIndexes.get(type).add(i);
        }
        if (this.originIndex == null) {
            this.originIndex = new Integer[types.size()];
        }
        typeRanges = new HashMap<>();
        int count = 0;
        for (String type : typeIndexes.keySet()) {
            List<Integer> indices = typeIndexes.get(type);
            Range range = new Range(count, -1);
            for (int index : indices) {
                this.originIndex[count++] = index;
            }
            range.setHigh(count - 1);
            typeRanges.put(type, range);
        }
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

    @Override
    public IndexResult search(CmpExpWrapper cmpExpWrapper, Map<VariableSource, Map<java.lang.String, Element.Number>> inputVariables) throws Exception {
        List<Range> ranges = searchType((CategoryCmpWrapper)cmpExpWrapper, inputVariables);
        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, ranges);
        return rangeIndexResult;
    }

//    public <String> IndexResult search(FilterConditionSingleton filterConditionSingleton, String seedValue) throws Exception {
//        ArrayList<ArrayList<Expression>> unionJoinFilters = filterConditionSingleton.unionJoinFilters;
//        List<Range> unionRanges = new ArrayList<>();
//        for (int i = 0; i < unionJoinFilters.size(); i++) {
//            List<Range> joinRanges = new ArrayList<>();
//            for (int j = 0; j < unionJoinFilters.get(i).size(); j++) {
//                List<Range> ranges = searchType((TypeExp) unionJoinFilters.get(i).get(j), (java.lang.String) seedValue);
//                joinRanges = joinRanges(joinRanges, ranges);
//            }
//            unionRanges = unionRanges(unionRanges, joinRanges);
//        }
//        System.out.println("---unionRanges:" + Arrays.toString(unionRanges.toArray()));
//        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, unionRanges);
//        return rangeIndexResult;
//    }
public static void main(String[] args) throws Exception {
    List<Integer> ids = new ArrayList<>();
    List<String> timestamp = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        ids.add(i);
        timestamp.add("node_" + String.valueOf(i%3));
    }
    HashIndex<Integer> rangeIndex = new HashIndex<>("hash_index:node_type:string");
    rangeIndex.setNode2IDs(ids);
    rangeIndex.addAttributes("node_type", timestamp);
    rangeIndex.buildIndex();
    Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
    String filterCond = "index.node_type in (node_1, node_2)";
    FilterConditionParser filterConditionParser = new FilterConditionParser();
    LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
    CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
    System.out.println("----cmpExp: " + cmpExp);
    System.out.println(rangeIndex.search(new CategoryCmpWrapper(cmpExp), inputVariables));
}
}
