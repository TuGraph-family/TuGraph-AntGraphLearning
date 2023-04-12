package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter.parser.AbstractCmpWrapper;
import com.alipay.alps.flatv3.filter.parser.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HashIndex extends BaseIndex {
    private Map<String, RangeUnit> typeRanges;

    public HashIndex(String indexType, String indexColumn, String indexDtype, NeighborDataset neighborDataset) {
        super(indexType, indexColumn, indexDtype, neighborDataset);
    }

    @Override
    protected int[] buildIndex() {
        Map<String, List<Integer>> typeIndexes = new TreeMap<>();
        List<String> types = neighborDataset.getAttributeList(getIndexColumn());
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            if (!typeIndexes.containsKey(type)) {
                typeIndexes.put(type, new ArrayList<>());
            }
            typeIndexes.get(type).add(i);
        }
        originIndices = new int[types.size()];
        this.typeRanges = new HashMap<>();
        int count = 0;
        for (String type : typeIndexes.keySet()) {
            List<Integer> indices = typeIndexes.get(type);
            RangeUnit range = new RangeUnit(count, -1);
            for (int index : indices) {
                originIndices[count++] = index;
            }
            range.setHigh(count - 1);
            this.typeRanges.put(type, range);
        }
        return originIndices;
    }

    @Override
    public AbstractResult search(AbstractCmpWrapper cmpExpWrapper, Map<VariableSource, Map<java.lang.String, Element.Number>> inputVariables) throws Exception {
        List<RangeUnit> ranges = searchType((CategoryCmpWrapper) cmpExpWrapper, inputVariables);
        RangeResult rangeIndexResult = new RangeResult(this, ranges);
        return rangeIndexResult;
    }

    private List<RangeUnit> searchType(CategoryCmpWrapper cateCmpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        String indexKey = getIndexColumn();
        List<RangeUnit> ansList = new ArrayList<>();
        Map<String, Element.Number> indexVariableMap = new HashMap<>();
        indexVariableMap.put(indexKey, null);
        inputVariables.put(VariableSource.INDEX, indexVariableMap);
        for (String type : this.typeRanges.keySet()) {
            inputVariables.get(VariableSource.INDEX).put(indexKey, Element.Number.newBuilder().setS(type).build());
            if (cateCmpWrapper.eval(inputVariables)) {
                ansList.add(this.typeRanges.get(type));
            }
        }
        return ansList;
    }
}
