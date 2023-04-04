package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import com.alipay.alps.flatv3.sampler.utils.AliasMethod;
import com.alipay.alps.flatv3.sampler.utils.PrefixSumSelection;
import com.alipay.alps.flatv3.sampler.utils.WeightedSelectionTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WeightedSampler extends AbstractSampler {
    private List<Float> weights = null;
    private AliasMethod aliasMethod = null;
    private PrefixSumSelection prefixSumSelection = null;
    
    public WeightedSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        super(sampleCondition, neighborDataset);
    }

    /**
     * Finds a random sample from the probability distribution according to the IndexResult.
     * @param indexResult the IndexResult of the data
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    @Override
    protected List<Integer> sampleImpl(AbstractIndexResult indexResult) {
        // get weights from the neighborDataset by the key in the sampleCondition
        weights = getNeighborDataset().getNumberAttributeList(getSampleCondition().getKey());
        int candidateCount = indexResult.getSize();
        // If there are fewer candidates than the limit, return all of them
        if (candidateCount <= getSampleCondition().getLimit()) {
            return indexResult.getIndices();
        }
        // If there is no filter condition, and we don't need to sample with replacement, and the limit is small, use the alias method
        if (!indexResult.hasFilterCondition() && (getSampleCondition().isReplacement() || getSampleCondition().getLimit() * 4 <= candidateCount)) {
            return sampleByAliasMethod(getSampleCondition().isReplacement());
        }
        // If we don't need to sample with replacement, and the limit is small, use the order statistic tree
        if (!getSampleCondition().isReplacement() && getSampleCondition().getLimit() > candidateCount * sampleCountToCandidateCountRatio) {
            return sampleByOrderStatisticTree(indexResult);
        }
        // If all other options fail, use the prefix sum array
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
            aliasMethod = new AliasMethod(weights, getRand());
        }
        if (replacement) {
            ArrayList<Integer> sampledIndex = new ArrayList<>();
            for (int i = 0; i < getSampleCondition().getLimit(); i++) {
                sampledIndex.add(aliasMethod.nextSample());
            }
            return sampledIndex;
        } else {
            Set<Integer> sampledDistinctIndex = new HashSet<>();
            while (sampledDistinctIndex.size() < getSampleCondition().getLimit()) {
                sampledDistinctIndex.add(aliasMethod.nextSample());
            }
            return new ArrayList<>(sampledDistinctIndex);
        }
    }

    /**
     * Generates a random sample from the probability distribution using the Prefix Sum method.
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    private List<Integer> sampleByPrefixSum(AbstractIndexResult indexResult, boolean replacement) {
        if (indexResult instanceof RangeIndexResult) { 
            if (prefixSumSelection == null) {
                prefixSumSelection = new PrefixSumSelection(indexResult.getOriginIndice(), weights, true, getRand());
            }
        } else {
            prefixSumSelection = new PrefixSumSelection(indexResult.getOriginIndice(), weights, false, getRand());
        }
        prefixSumSelection.initializePrefixSum(indexResult);
        int sampleLimit = getSampleCondition().getLimit();
        if (replacement) {
            ArrayList<Integer> sampledIndex = new ArrayList<>();
            for (int i = 0; i < sampleLimit; i++) {
                sampledIndex.add(prefixSumSelection.nextSample());
            }
            return sampledIndex;
        } else {
            Set<Integer> sampledDistinctIndex = new HashSet<>();
            while (sampledDistinctIndex.size() < sampleLimit) {
                sampledDistinctIndex.add(prefixSumSelection.nextSample());
            }
            return new ArrayList<>(sampledDistinctIndex);
        }
    }

    /**
     * Generates a random sample from the probability distribution using the Order Statistic Tree method.
     * @return An ArrayList of integers representing the index of the sampled elements.
     */
    private List<Integer> sampleByOrderStatisticTree(AbstractIndexResult indexResult) {
        //create the tree
        WeightedSelectionTree tree = new WeightedSelectionTree(indexResult.getIndices(), weights, getRand());
        //sample the indices
        ArrayList<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < this.getSampleCondition().getLimit(); i++) {
            sampledIndex.add(tree.nextSample());
        }
        return sampledIndex;
    }
}
