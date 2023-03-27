package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.ArithmeticCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CmpExpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RangeIndex extends BaseIndex {
    public RangeIndex(String indexMeta, NeighborDataset neighborDataset) {
        super(indexMeta, neighborDataset);
    }

    @Override
    public void buildIndex() {
        String indexColumn = getIndexColumn();
        originIndex = sortBy(neighborDataset.getAttributeList(indexColumn));
        neighborDataset.shuffle(originIndex);
    }

    @Override
    public IndexResult search(CmpExpWrapper cmpExpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        Range range = binarySearch((ArithmeticCmpWrapper)cmpExpWrapper, inputVariables);
        List<Range> ranges = new ArrayList<>();
        ranges.add(range);
        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, ranges);
        return rangeIndexResult;
    }

    private <T extends Comparable<T>> Integer[] sortBy(List<T> datas) {
        Integer[] index = new Integer[datas.size()];
        for (int i = 0; i < datas.size(); i++) {
            index[i] = i;
        }
        Arrays.sort(index, new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return datas.get(i1).compareTo(datas.get(i2));
            }
        });
        return index;
    }

    public Integer[] getOriginIndex() {
        return originIndex;
    }

    private <T> int lowerBound(List<T> nums, Map<VariableSource, Map<String, Element.Number>> inputVariables, java.util.function.Function<T, Boolean> f) {
        int left = 0, right = nums.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (f.apply(nums.get(mid))) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }
    private <T> int upperBound(List<T> nums, Map<VariableSource, Map<String, Element.Number>> inputVariables, java.util.function.Function<T, Boolean> f) {
        int left = 0, right = nums.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (f.apply(nums.get(mid))) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return right;
    }

    private Range binarySearch(ArithmeticCmpWrapper arithCmpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        if (arithCmpWrapper.getCmpExp().getOp() == CmpOp.EQ || arithCmpWrapper.getCmpExp().getOp() == CmpOp.NE) {
            CmpExp.Builder cmpExpBuilder = CmpExp.newBuilder().mergeFrom(arithCmpWrapper.getCmpExp());
            cmpExpBuilder.setOp(CmpOp.LE);
            Range rangeLE = binarySearchInequation(new ArithmeticCmpWrapper(cmpExpBuilder.build()), inputVariables);
            cmpExpBuilder.setOp(CmpOp.GE);
            Range rangeGE = binarySearchInequation(new ArithmeticCmpWrapper(cmpExpBuilder.build()), inputVariables);
            if (arithCmpWrapper.getCmpExp().getOp() == CmpOp.EQ) {
                rangeLE.join(rangeGE);
                return rangeLE;
            }
            // will return two ranges: [0, range.key-1] [range.value+1, size-1]
            throw new Exception("!= not implemented");
        }
        return binarySearchInequation(arithCmpWrapper, inputVariables);
    }

    private <T> Range binarySearchInequation(ArithmeticCmpWrapper arithCmpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        String indexColumn = arithCmpWrapper.getIndexColumn();
        Map<String, Element.Number> indexVariableMap = new HashMap<>();
        indexVariableMap.put(indexColumn, null);
        inputVariables.put(VariableSource.INDEX, indexVariableMap);

        Range range = new Range(0, originIndex.length-1);
        boolean hasLowerBound = arithCmpWrapper.hasLowerBound();
        if (neighborDataset.getFloatAttributes(indexColumn) != null) {
            java.util.function.Function<Float, Boolean> comparison = (neighboringValue) -> {
                inputVariables.get(VariableSource.INDEX).put(indexColumn, Element.Number.newBuilder().setF(neighboringValue).build());
                return arithCmpWrapper.eval(inputVariables);
            };
            if (hasLowerBound) {
                range.setLow(lowerBound(neighborDataset.getFloatAttributes(indexColumn), inputVariables, comparison));
            } else {
                range.setHigh(upperBound(neighborDataset.getFloatAttributes(indexColumn), inputVariables, comparison));
            }
        } else {
            java.util.function.Function<Long, Boolean> comparison = (neighboringValue) -> {
                inputVariables.get(VariableSource.INDEX).put(indexColumn, Element.Number.newBuilder().setI(neighboringValue).build());
                return arithCmpWrapper.eval(inputVariables);
            };
            if (hasLowerBound) {
                range.setLow(lowerBound(neighborDataset.getLongAttributes(indexColumn), inputVariables, comparison));
            } else {
                range.setHigh(upperBound(neighborDataset.getLongAttributes(indexColumn), inputVariables, comparison));
            }
        }
        return range;
    }
}
