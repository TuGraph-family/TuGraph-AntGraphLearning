package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.CommonIndexResult;
import com.alipay.alps.flatv3.index.IndexResult;
import com.alipay.alps.flatv3.index.Range;
import com.alipay.alps.flatv3.index.RangeIndexResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class WeightedSampler extends Sampler {
    private boolean hasInitializePrefixSum = false;
    private List<Float> prefixSum = null;
    private List<Float> weights = null;
    private AliasMethod aliasMethod = null;
    private float getSummationBetween(int l, int r) {
        return prefixSum.get(r) - (l > 0 ? prefixSum.get(l - 1) : 0);
    }

    private Random random = new Random(34);


    public WeightedSampler(SampleCondition sampleCondition, BaseIndex index) {
        super(sampleCondition, index);
    }
    /**
     * Constructor of the WeighedSampler class.
     * @param sampleCondition the sample condition
     * @param indexes the index of the data
     */
    public WeightedSampler(SampleCondition sampleCondition, HashMap<String, BaseIndex> indexes) {
        super(sampleCondition, indexes);
    }


    /**
     * Finds a random sample from the probability distribution according to the IndexResult.
     * @param indexResult the IndexResult of the data
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    @Override
    public List<Integer> sampleImpl(IndexResult indexResult) {
        weights = new ArrayList<>(indexResult.getSize());
        indexResult.copyNumberAttributes(getSampleCondition().key, weights);
        int candidateCount = indexResult.getSize();
        if (candidateCount <= this.getSampleCondition().limit) {
            return indexResult.getIndices();
        }
        if (this.getSampleCondition().replacement == false && this.getSampleCondition().limit * 5 > candidateCount) {
            return sampleByOrderStatisticTree(indexResult);
        }
        if (!indexResult.hasFilterCondition() && indexResult instanceof RangeIndexResult) {
            // alias
            return sampleByAliasMethod(indexResult);
        } else {
            // prefix sum
            if (indexResult instanceof RangeIndexResult) {
                return sampleByPrefixSum((RangeIndexResult) indexResult);
            } else {
                return sampleByPrefixSum((CommonIndexResult) indexResult);
            }
        }
    }

    /**
     * Generates a random sample from the probability distribution using the Alias method.
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    private List<Integer> sampleByAliasMethod(IndexResult indexResult) {
        if (aliasMethod == null) {
            aliasMethod = new AliasMethod(weights);
        }
        ArrayList<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < this.getSampleCondition().limit; i++) {
            sampledIndex.add(aliasMethod.nextRandom());
        }
        return null;
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
    /**
     * Initializes the prefixSum array.
     * @param weights the weights array
     */
    private void initializePrefixSum(List<Float> weights) {
        hasInitializePrefixSum = true;
        prefixSum = Collections.nCopies(weights.size(), weights.get(0));
        for (int i = 1; i < weights.size(); i++) {
            prefixSum.set(i, prefixSum.get(i-1) + weights.get(i));
        }
    }

    private void initializePrefixSum(List<Float> weights, List<Integer> validIndices) {
        hasInitializePrefixSum = true;
        prefixSum = Collections.nCopies(validIndices.size(), 0.0F);
        prefixSum.set(0, weights.get(validIndices.get(0)));
        for (int i = 1; i < validIndices.size(); i++) {
            prefixSum.set(i, prefixSum.get(i-1) + weights.get(validIndices.get(i)));
        }
    }
    /**
     * Generates a random sample from the probability distribution using the Prefix Sum method.
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    private List<Integer> sampleByPrefixSum(RangeIndexResult indexResult) {
        if (!hasInitializePrefixSum) {
           initializePrefixSum(weights);
        }

        List<Range> sortedIntervals = indexResult.sortedIntervals;
        List<Float> intervalPrefixSum = Collections.nCopies(sortedIntervals.size(), 0.0F);
        for (int i = 0; i < sortedIntervals.size(); i++) {
            if (i > 0) {
                intervalPrefixSum.set(i, intervalPrefixSum.get(i-1));
            }
            Range range = sortedIntervals.get(i);
            float weights = getSummationBetween(range.getLow(), range.getHigh());
            intervalPrefixSum.set(i, intervalPrefixSum.get(i) + weights);
        }
        Float maxIntervalPrefixSum = intervalPrefixSum.get(intervalPrefixSum.size() - 1);

        ArrayList<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < this.getSampleCondition().limit; i++) {
            int rangeIndex = lowerBound(intervalPrefixSum, 0, intervalPrefixSum.size() - 1, random.nextFloat() * maxIntervalPrefixSum);
            Range range = sortedIntervals.get(rangeIndex);
            int neighborIndex = lowerBound(prefixSum, range.getLow(), range.getHigh(),
                    random.nextFloat() * getSummationBetween(range.getLow(), range.getHigh()) + (range.getLow() > 1 ? prefixSum.get(range.getLow() - 1) : 0));
            sampledIndex.add(neighborIndex);
        }
        return sampledIndex;
    }

    private List<Integer> sampleByPrefixSum(CommonIndexResult indexResult) {
        initializePrefixSum(weights, indexResult.getIndices());
        ArrayList<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < this.getSampleCondition().limit; i++) {
            int neighborIndex = lowerBound(prefixSum, 0, prefixSum.size(), random.nextFloat() * prefixSum.get(prefixSum.size() - 1));
            sampledIndex.add(neighborIndex);
        }
        return sampledIndex;
    }

    private List<Integer> sampleByOrderStatisticTree(IndexResult indexResult) {
        List<Integer> indices = indexResult.getIndices();
        List<Float> weights = new ArrayList<Float>(indices.size());
        for (int i = 0; i < indices.size(); i++) {
            weights.set(i, weights.get(indices.get(i)));
        }
        WeightedSelectionTree tree = new WeightedSelectionTree(indices, weights);
        ArrayList<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < this.getSampleCondition().limit; i++) {
            WeightedSelectionTree.Node root = tree.getRoot();
            Float totalWeight = root.elementWeight + root.rightBranchWeight + root.leftBranchWeight;
            int removedNode = tree.selectNode(totalWeight * random.nextFloat());
            sampledIndex.add(removedNode);
        }
        return sampledIndex;
    }
}
