package com.alipay.alps.flatv3.sampler.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.alipay.alps.flatv3.index.result.Range;

// this class maintains a prefix sum of the weights of the elements in an array,
// and allows for random selection with replacement of an element based on its weight.

public class PrefixSumSelection {
    private List<Float> weights;
    private List<Float> prefixSum;
    private List<Float> intervalPrefixSum;
    private List<Range> sortedIntervals;
    private Integer[] originIndices;
    private List<Integer> validIndices;
    private Random rand;

    public PrefixSumSelection(List<Range> sortedIntervals, Integer[] originIndices, List<Float> prefixSum, Random rand) {
        this.sortedIntervals = sortedIntervals;
        this.originIndices = originIndices;
        this.prefixSum = prefixSum;
        this.rand = rand;
        initializePrefixSumOfRanges(sortedIntervals);
    }

    public PrefixSumSelection(List<Integer> validIndices, List<Float> weights, Random rand) {
        this.validIndices = validIndices;
        this.weights = weights;
        this.rand = rand;
        initializePrefixSumOfValidIndices(validIndices);
    }

    private void initializePrefixSumOfRanges(List<Range> sortedIntervals) {
        intervalPrefixSum = new ArrayList<>(sortedIntervals.size());
        intervalPrefixSum.add(getSummationBetween(sortedIntervals.get(0).getLow(), sortedIntervals.get(0).getHigh()));
        for (int i = 1; i < sortedIntervals.size(); i++) {
            Range range = sortedIntervals.get(i);
            intervalPrefixSum.add(intervalPrefixSum.get(i-1) + getSummationBetween(range.getLow(), range.getHigh()));
        }
    }

    private void initializePrefixSumOfValidIndices(List<Integer> validIndices) {
        sortedIntervals = new ArrayList<>();
        sortedIntervals.add(new Range(0, validIndices.size() - 1));
        prefixSum = new ArrayList<>(validIndices.size());
        prefixSum.add(weights.get(validIndices.get(0)));
        for (int i = 1; i < validIndices.size(); i++) {
            prefixSum.add(prefixSum.get(i-1) + weights.get(validIndices.get(i)));
        }
        intervalPrefixSum = new ArrayList<>(sortedIntervals.size());
        intervalPrefixSum.add(prefixSum.get(validIndices.size() - 1));
    }

    public int nextSample() {
        float randWeight = rand.nextFloat() * intervalPrefixSum.get(intervalPrefixSum.size() - 1);
        int rangeIndex = 0;
        if (sortedIntervals != null && sortedIntervals.size() > 1) {
            rangeIndex = lowerBound(intervalPrefixSum, 0, intervalPrefixSum.size() - 1, randWeight);
        }
        Range range = this.sortedIntervals.get(rangeIndex);
        float leftWeight = randWeight - (rangeIndex >= 1 ? intervalPrefixSum.get(rangeIndex - 1) : 0);
        int chosenIndex = lowerBound(prefixSum, range.getLow(), range.getHigh(),
                leftWeight + (range.getLow() >= 1 ? prefixSum.get(range.getLow() - 1) : 0));
        return originIndices != null ? originIndices[chosenIndex] : validIndices.get(chosenIndex);
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
