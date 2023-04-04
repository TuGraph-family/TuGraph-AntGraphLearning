package com.alipay.alps.flatv3.sampler.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;

// this class maintains a prefix sum of the weights of the elements in an array,
// and allows for random selection with replacement of an element based on its weight.

public class PrefixSumSelection {
    private List<Float> weights;
    private List<Float> prefixSum;
    List<Float> intervalPrefixSum;
    List<Range> sortedIntervals;
    private Random rand;

    public PrefixSumSelection(Integer[] originIndices, List<Float> weights, boolean reusePrefixSum, Random rand) {
        this.weights = weights;
        this.rand = rand;
        prefixSum = new ArrayList<>(Collections.nCopies(weights.size(), weights.get(0)));
        if (reusePrefixSum) {
            // Initializes the prefixSum array.
            for (int i = 1; i < weights.size(); i++) {
                prefixSum.set(i, prefixSum.get(i-1) + weights.get(originIndices[i]));
            }
        }
    }
    
    public void initializePrefixSum(AbstractIndexResult indexResult) {
        if (indexResult instanceof RangeIndexResult) {
            initializePrefixSumOfRanges(((RangeIndexResult) indexResult).getRangeList());
        } else {
            initializePrefixSumOfValidIndices(indexResult.getIndices());
        }
    }

    private void initializePrefixSumOfRanges(List<Range> sortedIntervals) {
        this.sortedIntervals = sortedIntervals;
        intervalPrefixSum = new ArrayList<>(Collections.nCopies(sortedIntervals.size(), 0.0F));
        intervalPrefixSum.set(0, getSummationBetween(sortedIntervals.get(0).getLow(), sortedIntervals.get(0).getHigh()));
        for (int i = 1; i < sortedIntervals.size(); i++) {
            Range range = sortedIntervals.get(i);
            intervalPrefixSum.set(i, intervalPrefixSum.get(i-1) + getSummationBetween(range.getLow(), range.getHigh()));
        }
    }

    private void initializePrefixSumOfValidIndices(List<Integer> validIndices) {
        sortedIntervals = new ArrayList<>();
        sortedIntervals.add(new Range(0, validIndices.size() - 1));
        prefixSum.set(0, weights.get(validIndices.get(0)));
        for (int i = 1; i < validIndices.size(); i++) {
            prefixSum.set(i, prefixSum.get(i-1) + weights.get(validIndices.get(i)));
        }
    }

    public int nextSample() {
        int rangeIndex = 0;
        if (sortedIntervals != null && sortedIntervals.size() > 1) {
            rangeIndex = lowerBound(intervalPrefixSum, 0, intervalPrefixSum.size() - 1, rand.nextFloat() * intervalPrefixSum.get(intervalPrefixSum.size() - 1));
        }
        Range range = this.sortedIntervals.get(rangeIndex);
        return lowerBound(prefixSum, range.getLow(), range.getHigh(),
            rand.nextFloat() * getSummationBetween(range.getLow(), range.getHigh()) + (range.getLow() > 1 ? prefixSum.get(range.getLow() - 1) : 0));
    }

    /**
     * Finds the lower bound of the index result.
     * @param prefixSum the prefixSum array
     * @param l the left bound
     * @param r the right bound
     * @param weight the weight to find the lower bound
     * @return An integer representing the lower bound of the index result.
     */
    private int lowerBound(List<Float> prefixSum, int l, int r, Float weight) {
        while (l <= r) {
            int mid = l + (r - l) / 2;
            if (prefixSum.get(mid) >= weight) {
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return l;
    }

    private float getSummationBetween(int l, int r) {
        return prefixSum.get(r) - (l > 0 ? prefixSum.get(l - 1) : 0);
    }

}
