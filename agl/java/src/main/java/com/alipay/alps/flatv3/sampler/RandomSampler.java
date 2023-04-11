package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * This class represents a random sampler that performs random sampling on an input IndexResult object.
 * The class extends the Sampler abstract class.
 * It uses a random number generator to select a subset of the IndexResult object based on the provided SampleCondition.
 */
public class RandomSampler extends AbstractSampler {
    public RandomSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        super(sampleCondition, neighborDataset);
    }

    /**
     * Perform random sampling on an input IndexResult object.
     * If the sample size is smaller than a quarter of the candidate count, it selects elements randomly without replacement.
     * Otherwise, it performs Fisher-Yates shuffle to randomly select a subset of elements.
     *
     * @param indexResult An IndexResult object containing the data to be sampled
     * @return An ArrayList<Integer> object containing the indices of the selected elements
     */
    @Override
    public List<Integer> sample(AbstractIndexResult indexResult) {
        int candidateCount = indexResult.getSize();
        int sampleCount = this.getSampleCondition().getLimit();
        // If the number of samples requested is less than 1/sampleCountToCandidateCountRatio of the input size,
        // simply select samples at random without replacement using a HashSet.
        List<Integer> sampled = sampleWithCount(candidateCount, sampleCount);
        List<Integer> sampledIndex = new ArrayList<>(sampled.size());
        if (indexResult instanceof CommonIndexResult) {
            List<Integer> originIndices = indexResult.getIndices();
            for (int i = 0; i < sampled.size(); i++) {
                sampledIndex.add(originIndices.get(sampled.get(i)));
            }
        } else {
            assert indexResult instanceof RangeIndexResult;
            for (int i = 0; i < sampled.size(); i++) {
                int idx = ((RangeIndexResult) indexResult).getRangeIndex(sampled.get(i));
                sampledIndex.add(indexResult.getOriginIndex(idx));
            }

        }
        return sampledIndex;
    }

    private List<Integer> sampleWithCount(int candidateCount, int sampleCount) {
        if (getSampleCondition().isReplacement()) {
            List<Integer> samples = new ArrayList<>(sampleCount);
            for (int i = 0; i < sampleCount; i++) {
                samples.add(getNextRandomInt(candidateCount));
            }
            return samples;
        }
        if (sampleCount >= candidateCount) {
            List<Integer> samples = new ArrayList<>(candidateCount);
            for (int i = 0; i < candidateCount; i++) {
                samples.add(i);
            }
            return samples;
        }
        if (sampleCount <= candidateCount * sampleCountToCandidateCountRatio) {
            HashSet<Integer> sampledIndex = new HashSet<>();
            while (sampledIndex.size() < sampleCount) {
                int rnd = getNextRandomInt(candidateCount);
                if (!sampledIndex.contains(rnd)) {
                    sampledIndex.add(rnd);
                }
            }
            return new ArrayList<>(sampledIndex);
        }

        return sampleByShuffle(candidateCount, sampleCount);
    }

    // implement random sampling k elements without replacement out of n elements
    private List<Integer> sampleByShuffle(int n, int k) {
        List<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sampledIndex.add(i);
        }
        Collections.shuffle(sampledIndex);
        return sampledIndex.subList(0, k);
    }
}

