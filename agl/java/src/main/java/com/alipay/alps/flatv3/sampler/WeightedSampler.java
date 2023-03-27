package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import com.alipay.alps.flatv3.sampler.utils.AliasMethod;
import com.alipay.alps.flatv3.sampler.utils.WeightedSelectionTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WeightedSampler extends Sampler {
    private boolean hasInitializePrefixSum = false;
    private List<Float> prefixSum = null;
    private List<Float> weights = null;
    private AliasMethod aliasMethod = null;
    /**
     * Constructor of the WeighedSampler class.
     * @param sampleCondition the sample condition
     * @param index the index of the data
     */
    public WeightedSampler(SampleCondition sampleCondition, BaseIndex index) {
        super(sampleCondition, index);
    }

    public WeightedSampler(SampleCondition sampleCondition, HashMap<String, BaseIndex> indexes) {
        super(sampleCondition, indexes);
    }

    /**
     * Finds a random sample from the probability distribution according to the IndexResult.
     * @param indexResult the IndexResult of the data
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    @Override
    protected List<Integer> sampleImpl(IndexResult indexResult) {
        weights = new ArrayList<>(indexResult.getSize());
        indexResult.copyNumberAttributes(getSampleCondition().getKey(), weights);
        int candidateCount = indexResult.getSize();
        if (candidateCount <= getSampleCondition().getLimit()) {
            return indexResult.getIndices();
        }
        // alias method
        if (!indexResult.hasFilterCondition() && (getSampleCondition().isReplacement() || getSampleCondition().getLimit() * 4 <= candidateCount)) {
            return sampleByAliasMethod(getSampleCondition().isReplacement());
        }
        // search in order statistic tree
        if (getSampleCondition().isReplacement() == false && getSampleCondition().getLimit() * 4 > candidateCount) {
            return sampleByOrderStatisticTree(indexResult);
        }
        // binary search on prefix sum array
        if (indexResult instanceof RangeIndexResult) {
            return sampleByPrefixSum((RangeIndexResult) indexResult, getSampleCondition().isReplacement());
        } else {
            return sampleByPrefixSum((CommonIndexResult) indexResult, getSampleCondition().isReplacement());
        }
    }

    /**
     * Generates a random sample from the probability distribution using the Alias method.
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    private List<Integer> sampleByAliasMethod(boolean replacement) {
        if (aliasMethod == null) {
            aliasMethod = new AliasMethod(weights);
        }
        if (replacement) {
            ArrayList<Integer> sampledIndex = new ArrayList<>();
            for (int i = 0; i < getSampleCondition().getLimit(); i++) {
                sampledIndex.add(aliasMethod.nextRandom());
            }
            return sampledIndex;
        } else {
            Set<Integer> sampledDistinctIndex = new HashSet<>();
            while (sampledDistinctIndex.size() < getSampleCondition().getLimit()) {
                sampledDistinctIndex.add(aliasMethod.nextRandom());
            }
            return new ArrayList<>(sampledDistinctIndex);
        }
    }

    /**
     * Initializes the prefixSum array.
     * @param weights the weights array
     */
    private void initializePrefixSum(List<Float> weights) {
        hasInitializePrefixSum = true;
        prefixSum = new ArrayList<>(Collections.nCopies(weights.size(), weights.get(0)));
        for (int i = 1; i < weights.size(); i++) {
            prefixSum.set(i, prefixSum.get(i-1) + weights.get(i));
        }
    }

    /**
     * Generates a random sample from the probability distribution using the Prefix Sum method.
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    private List<Integer> sampleByPrefixSum(RangeIndexResult indexResult, boolean replacement) {
        if (!hasInitializePrefixSum) {
           initializePrefixSum(weights);
        }

        List<Range> sortedIntervals = indexResult.getRangeList();
        List<Float> intervalPrefixSum = new ArrayList<>(Collections.nCopies(sortedIntervals.size(), 0.0F));
        for (int i = 0; i < sortedIntervals.size(); i++) {
            if (i > 0) {
                intervalPrefixSum.set(i, intervalPrefixSum.get(i-1));
            }
            Range range = sortedIntervals.get(i);
            float weights = getSummationBetween(range.getLow(), range.getHigh());
            intervalPrefixSum.set(i, intervalPrefixSum.get(i) + weights);
        }
        Float maxIntervalPrefixSum = intervalPrefixSum.get(intervalPrefixSum.size() - 1);

        if (replacement) {
            ArrayList<Integer> sampledIndex = new ArrayList<>();
            for (int i = 0; i < this.getSampleCondition().getLimit(); i++) {
                int rangeIndex = lowerBound(intervalPrefixSum, 0, intervalPrefixSum.size() - 1, getNextFloat() * maxIntervalPrefixSum);
                Range range = sortedIntervals.get(rangeIndex);
                int neighborIndex = lowerBound(prefixSum, range.getLow(), range.getHigh(),
                        getNextFloat() * getSummationBetween(range.getLow(), range.getHigh()) + (range.getLow() > 1 ? prefixSum.get(range.getLow() - 1) : 0));
                sampledIndex.add(neighborIndex);
            }
            return sampledIndex;
        } else {
            Set<Integer> sampledIndex = new HashSet<>();
            while (sampledIndex.size() < this.getSampleCondition().getLimit()) {
                int rangeIndex = lowerBound(intervalPrefixSum, 0, intervalPrefixSum.size() - 1, getNextFloat() * maxIntervalPrefixSum);
                Range range = sortedIntervals.get(rangeIndex);
                int neighborIndex = lowerBound(prefixSum, range.getLow(), range.getHigh(),
                        getNextFloat() * getSummationBetween(range.getLow(), range.getHigh()) + (range.getLow() > 1 ? prefixSum.get(range.getLow() - 1) : 0));
                sampledIndex.add(neighborIndex);
            }
            return new ArrayList<>(sampledIndex);
        }
    }

    private void initializePrefixSum(List<Float> weights, List<Integer> validIndices) {
        prefixSum = new ArrayList<>(Collections.nCopies(validIndices.size(), 0.0F));
        prefixSum.set(0, weights.get(validIndices.get(0)));
        for (int i = 1; i < validIndices.size(); i++) {
            prefixSum.set(i, prefixSum.get(i-1) + weights.get(validIndices.get(i)));
        }
    }
    private List<Integer> sampleByPrefixSum(CommonIndexResult indexResult, boolean replacement) {
        initializePrefixSum(weights, indexResult.getIndices());
        if (!replacement) {
            ArrayList<Integer> sampledIndex = new ArrayList<>();
            for (int i = 0; i < this.getSampleCondition().getLimit(); i++) {
                int neighborIndex = lowerBound(prefixSum, 0, prefixSum.size(), getNextFloat() * prefixSum.get(prefixSum.size() - 1));
                sampledIndex.add(neighborIndex);
            }
            return sampledIndex;
        } else {
            Set<Integer> sampledIndex = new HashSet<>();
            while (sampledIndex.size() < this.getSampleCondition().getLimit()) {
                int neighborIndex = lowerBound(prefixSum, 0, prefixSum.size(), getNextFloat() * prefixSum.get(prefixSum.size() - 1));
                sampledIndex.add(neighborIndex);
            }
            return new ArrayList<>(sampledIndex);
        }
    }

    private List<Integer> sampleByOrderStatisticTree(IndexResult indexResult) {
        List<Integer> indices = indexResult.getIndices();
        List<Float> tmpWeights = new ArrayList<Float>(indices.size());
        for (int i = 0; i < indices.size(); i++) {
            tmpWeights.add(this.weights.get(indices.get(i)));
        }
        WeightedSelectionTree tree = new WeightedSelectionTree(indices, tmpWeights);
        ArrayList<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < this.getSampleCondition().getLimit(); i++) {
            Float totalWeight = tree.getTotalWeight();
            int removedNode = tree.selectNode(totalWeight * getNextFloat());
            sampledIndex.add(removedNode);
        }
        return sampledIndex;
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
