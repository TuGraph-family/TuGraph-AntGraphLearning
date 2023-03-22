package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.ArithmeticCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CmpExpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
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
import java.lang.Double;

public class RangeIndex<ID> extends BaseIndex<ID> {
    public RangeIndex(String indexMeta) {
        super(indexMeta);
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

    @Override
    public void buildIndex() {
        String indexColumn = getIndexColumn();
        if (floatAttributes != null && floatAttributes.containsKey(indexColumn)) {
            this.originIndex = sortBy(floatAttributes.get(indexColumn));
        } else {
            this.originIndex = sortBy(longAttributes.get(indexColumn));
        }
        ArrayList<Integer> shuffleIndex = new ArrayList<Integer>(Collections.nCopies(this.originIndex.length, 0));
        int candidateCount = this.node2IDs.size();
        for (int i = 0; i < candidateCount; i++) {
            shuffleIndex.set(this.originIndex[i], i);
        }
        for (int i = 0; i < candidateCount; i++) {
            while (shuffleIndex.get(i) != i) {
                for (String key : this.floatAttributes.keySet()) {
                    Collections.swap(this.floatAttributes.get(key), i, shuffleIndex.get(i));
                }
                for (String key : this.longAttributes.keySet()) {
                    Collections.swap(this.longAttributes.get(key), i, shuffleIndex.get(i));
                }
                Collections.swap(shuffleIndex, i, shuffleIndex.get(i));
            }
        }
    }

    public Integer[] getOriginIndex() {
        return originIndex;
    }

//    public HashMap<String, List<Double>> getIndexedData() {
//        return indexedDouble;
//    }

    private int binarySearch(List<Double> nums, Double seed) {
        int left = 0, right = nums.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums.get(mid) == seed) {
                return mid;
            } else if (nums.get(mid) - seed > 0) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return -1;
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
        if (floatAttributes != null && floatAttributes.containsKey(indexColumn)) {
            System.out.println("------------binarySearchInequation indexColumn:" + indexColumn + " vals:" + Arrays.toString(floatAttributes.get(indexColumn).toArray()));
            java.util.function.Function<Float, Boolean> comparison = (neighboringValue) -> {
                inputVariables.get(VariableSource.INDEX).put(indexColumn, Element.Number.newBuilder().setF(neighboringValue).build());
                return arithCmpWrapper.eval(inputVariables);
            };
            if (hasLowerBound) {
                range.setLow(lowerBound(floatAttributes.get(indexColumn), inputVariables, comparison));
            } else {
                range.setHigh(upperBound(floatAttributes.get(indexColumn), inputVariables, comparison));
            }
        } else {
            java.util.function.Function<Long, Boolean> comparison = (neighboringValue) -> {
                inputVariables.get(VariableSource.INDEX).put(indexColumn, Element.Number.newBuilder().setI(neighboringValue).build());
                return arithCmpWrapper.eval(inputVariables);
            };
            if (hasLowerBound) {
                range.setLow(lowerBound(longAttributes.get(indexColumn), inputVariables, comparison));
            } else {
                range.setHigh(upperBound(longAttributes.get(indexColumn), inputVariables, comparison));
            }
        }
        return range;
    }

    @Override
    public IndexResult search(CmpExpWrapper cmpExpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        Range range = binarySearch((ArithmeticCmpWrapper)cmpExpWrapper, inputVariables);
        List<Range> ranges = new ArrayList<>();
        ranges.add(range);
        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, ranges);
        return rangeIndexResult;
    }

//    public <Double> RangeIndexResult search(LogicExps logicExps, Double seedValue) throws Exception {
//        ArrayList<ArrayList<Expression>> unionJoinFilters = logicExps.unionJoinFilters;
//        ArrayList<Range> unionRanges = new ArrayList<>();
//        for (int i = 0; i < unionJoinFilters.size(); i++) {
//            Range joinRange = new Pair<>(0, Integer.MAX_VALUE);
//            for (int j = 0; j < unionJoinFilters.get(i).size(); j++) {
//                Range range = binarySearchInequation(unionJoinFilters.get(i).get(j), (java.lang.Double) seedValue);
//                joinRange.join(range);
//            }
//            java.util.function.BiFunction<Range, Range, Boolean> lower = (e, t) -> e.key >= t.key;
//            int leftIndex = lowerBound(unionRanges, joinRange, lower);
//            int rightIndex = lowerBound(unionRanges, new Pair<>(joinRange.value, -1), lower);
//            if (rightIndex == leftIndex) {
//                if (unionRanges.size() > leftIndex && unionRanges.get(leftIndex).value >= joinRange.key) {
//                    unionRanges.get(leftIndex).value = Math.max(unionRanges.get(leftIndex).value, joinRange.value);
//                }
//                unionRanges.add(leftIndex, joinRange);
//            } else {
//                if (unionRanges.get(leftIndex).value < joinRange.key) {
//                    leftIndex++;
//                }
//                unionRanges.get(leftIndex).key = joinRange.key;
//                unionRanges.get(leftIndex).value = Math.max(unionRanges.get(rightIndex).value, joinRange.value);
//                while (unionRanges.size() > leftIndex + 1) {
//                    unionRanges.remove(leftIndex + 1);
//                }
//            }
//        }
//        System.out.println("---unionRanges:" + Arrays.toString(unionRanges.toArray()));
//        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, unionRanges);
//        return rangeIndexResult;
//    }

    public static void main(String[] args) throws Exception {
        List<String> ids = new ArrayList<>();
        List<Long> timestamp = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            timestamp.add(2L * i);
        }
        RangeIndex<String> rangeIndex = new RangeIndex<>("range_index:time:int,");
        rangeIndex.setNode2IDs(ids);
        rangeIndex.addAttributes("time", timestamp);
        rangeIndex.buildIndex();
        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        seedVariableMap.put("1", Element.Number.newBuilder().setI(15L).build());
        inputVariables.put(VariableSource.SEED, seedVariableMap);

        String filterCond = "index.time - seed.1 <= 0 && seed.1 - index.time <= 10";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        System.out.println("----cmpExp: " + cmpExp);
        System.out.println(rangeIndex.search(new ArithmeticCmpWrapper(cmpExp), inputVariables));
    }
}
