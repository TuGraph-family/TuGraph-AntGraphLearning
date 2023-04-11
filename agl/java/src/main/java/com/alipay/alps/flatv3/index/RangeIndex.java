package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.AbstractCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.ArithmeticCmpWrapper;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RangeIndex extends BaseIndex {
    private List sortedWeights;

    public RangeIndex(String indexType, String indexColumn, String indexDtype, NeighborDataset neighborDataset) {
        super(indexType, indexColumn, indexDtype, neighborDataset);
    }

    @Override
    public int[] buildIndex() {
        String indexColumn = getIndexColumn();
        sortedWeights = neighborDataset.copyAttributeList(indexColumn);
        originIndices = super.buildIndex();
        quicksort(originIndices, sortedWeights, 0, sortedWeights.size() - 1);
        return originIndices;
    }

    public List getSortedWeights() {
        return sortedWeights;
    }

    @Override
    public AbstractIndexResult search(AbstractCmpWrapper cmpExpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        Range range = binarySearch((ArithmeticCmpWrapper) cmpExpWrapper, inputVariables);
        List<Range> ranges = new ArrayList<>();
        if (range.getSize() > 0) {
            ranges.add(range);
        }
        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, ranges);
        return rangeIndexResult;
    }

    public static void quicksort(int[] indices, List<Comparable> weights, int left, int right) {
        if (left < right) {
            int pivotIndex = partition(indices, weights, left, right);
            quicksort(indices, weights, left, pivotIndex - 1);
            quicksort(indices, weights, pivotIndex + 1, right);
        }
    }

    private static int partition(int[] indices, List<Comparable> weights, int left, int right) {
        Comparable pivotWeight = weights.get(right);
        int i = left - 1;
        for (int j = left; j < right; j++) {
            if (weights.get(j).compareTo(pivotWeight) < 0) {
                i++;
                swap(indices, i, j);
                Collections.swap(weights, i, j);
            }
        }
        swap(indices, i + 1, right);
        Collections.swap(weights, i + 1, right);
        return i + 1;
    }

    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private <T> int lowerBound(List<T> nums, java.util.function.Function<T, Boolean> f) {
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

    private <T> int upperBound(List<T> nums, java.util.function.Function<T, Boolean> f) {
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

    // arithCmpWrapper: index.time  < seed.1 + 2
    // inputVariables: seed.1 = 2; index.time = list<float/xxx>
    // 
    private Range binarySearchInequation(ArithmeticCmpWrapper arithCmpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        String indexColumn = arithCmpWrapper.getIndexColumn(); // index.time
        Map<String, Element.Number> indexVariableMap = new HashMap<>();
        indexVariableMap.put(indexColumn, null);
        inputVariables.put(VariableSource.INDEX, indexVariableMap);

        Range range = new Range(0, originIndices.length - 1);
        boolean hasLowerBound = arithCmpWrapper.hasLowerBound();
        if (neighborDataset.getFloatAttributeList(indexColumn) != null) {
            java.util.function.Function<Float, Boolean> comparison = (neighboringValue) -> {
                inputVariables.get(VariableSource.INDEX).put(indexColumn, Element.Number.newBuilder().setF(neighboringValue).build());
                return arithCmpWrapper.eval(inputVariables);
            };
            if (hasLowerBound) {
                range.setLow(lowerBound(sortedWeights, comparison));
            } else {
                range.setHigh(upperBound(sortedWeights, comparison));
            }
        } else {
            java.util.function.Function<Long, Boolean> comparison = (neighboringValue) -> {
                inputVariables.get(VariableSource.INDEX).put(indexColumn, Element.Number.newBuilder().setI(neighboringValue).build());
                return arithCmpWrapper.eval(inputVariables);
            };
            if (hasLowerBound) {
                range.setLow(lowerBound(sortedWeights, comparison));
            } else {
                range.setHigh(upperBound(sortedWeights, comparison));
            }
        }
        return range;
    }
}
