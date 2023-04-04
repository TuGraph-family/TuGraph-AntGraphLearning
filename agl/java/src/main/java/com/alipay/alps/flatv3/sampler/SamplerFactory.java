package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;

public class SamplerFactory {
    public static AbstractSampler createSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        // If the sampling method is weighted_sampler, return a WeightedSampler
        if (sampleCondition.getMethod().compareToIgnoreCase("weighted_sampler") == 0) {
            return new WeightedSampler(sampleCondition, neighborDataset);
        // If the sampling method is topk, return a TopKSampler
        } else if (sampleCondition.getMethod().compareToIgnoreCase("topk") == 0) {
            return createTopKSampler(sampleCondition, neighborDataset);
        // If the sampling method is random_sampler, return a RandomSampler
        } else if (sampleCondition.getMethod().compareToIgnoreCase("random_sampler") == 0) {
            return new RandomSampler(sampleCondition, neighborDataset);
        // If the sampling method is not supported, throw an exception
        } else {
            throw new IllegalArgumentException("Sampling method " + sampleCondition.getMethod() + " not supported");
        }
    }

    private static AbstractSampler createTopKSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        // Get the data type of the key
        String dtype = neighborDataset.getDtype(sampleCondition.getKey());
        // If the data type is float, return a TopKSampler<Float>
        if (dtype.compareToIgnoreCase("float") == 0) {
            return new TopKSampler<Float>(sampleCondition, neighborDataset);
        // If the data type is long, return a TopKSampler<Long>
        } else if (dtype.compareToIgnoreCase("long") == 0) {
            return new TopKSampler<Long>(sampleCondition, neighborDataset);
        // If the data type is string, return a TopKSampler<String>
        } else if (dtype.compareToIgnoreCase("string") == 0) {
            return new TopKSampler<String>(sampleCondition, neighborDataset);
        }
        // If the data type is not supported, throw an exception
        throw new IllegalArgumentException("Unsupported data type for key: " + sampleCondition.getKey());
    }
}