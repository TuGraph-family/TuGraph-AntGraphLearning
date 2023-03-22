package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.IndexResult;
import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
/**
 This class represents a random sampler that performs random sampling on an input IndexResult object.
 The class extends the Sampler abstract class.
 It uses a random number generator to select a subset of the IndexResult object based on the provided SampleCondition.
 */
public class RandomSampler extends Sampler {
    /**
     Constructs a new RandomSampler object with the provided SampleCondition and IndexResult object.
     @param sampleCondition A SampleCondition object containing the conditions for the sampling process
     @param index An Index object containing the data to be sampled
     */
    public RandomSampler(SampleCondition sampleCondition, BaseIndex index) {
        super(sampleCondition, index);
    }

    /**
     Perform random sampling on an input IndexResult object.
     If the sample size is smaller than a quarter of the candidate count, it selects elements randomly without replacement.
     Otherwise, it performs Fisher-Yates shuffle to randomly select a subset of elements.
     @param indexResult An IndexResult object containing the data to be sampled
     @return An ArrayList<Integer> object containing the indices of the selected elements
     */
    @Override
    public List<Integer> sampleImpl(IndexResult indexResult) {
        int candidateCount = indexResult.getSize();
        int sampleCount = this.getSampleCondition().limit;
        // If the number of samples requested is less than 1/4 of the input size,
        // simply select samples at random without replacement using a HashSet.
        if (sampleCount < candidateCount / 4) {
            HashSet<Integer> sampledIndex = new HashSet<>();
            while (sampledIndex.size() < sampleCount) {
                int rnd = getRand().nextInt(candidateCount);
                if (!sampledIndex.contains(rnd)) {
                    sampledIndex.add(rnd);
                }
            }
            return new ArrayList<>(sampledIndex);
        }

        // Otherwise, if the number of samples requested is more than 1/2 of the input size,
        // select the complement set (i.e. non-selected samples) using an ArrayList and return it.
        ArrayList<Integer> sampledIndex = new ArrayList<>();
        for (int i = 0; i < candidateCount; i++) {
            sampledIndex.add(i);
        }
        boolean sampleRemain = true;
        if (sampleCount >= candidateCount) {
            return sampledIndex;
        } else if (sampleCount > candidateCount / 2) {
            sampleCount = candidateCount - sampleCount;
            sampleRemain = false;
        }
        for (int i = 0; i < sampleCount; i++) {
            int rnd = getRand().nextInt(candidateCount - i);
            if (rnd != i) {
                int temp = sampledIndex.get(i);
                sampledIndex.set(i, sampledIndex.get(rnd));
                sampledIndex.set(rnd, temp);
            }
        }
        sampleCount = Math.max(sampleCount, 0);
        return new ArrayList<Integer>(sampleRemain ? sampledIndex.subList(0, sampleCount) : sampledIndex.subList(sampleCount, sampledIndex.size()));
    }
}
