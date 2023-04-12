package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;

public class SamplerFactory {
    public static AbstractSampler createSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        switch (sampleCondition.getMethod().toLowerCase()) {
            case "weighted_sampler":
                // If the sampling method is weighted_sampler, return a WeightedSampler
                return new WeightedSampler(sampleCondition, neighborDataset);
            case "topk":
                // If the sampling method is topk, return a TopKSampler
                return createTopKSampler(sampleCondition, neighborDataset);
            case "random_sampler":
                // If the sampling method is random_sampler, return a RandomSampler
                return new RandomSampler(sampleCondition, neighborDataset);
            default:
                // If the sampling method is not supported, throw an exception
                throw new IllegalArgumentException("Sampling method " + sampleCondition.getMethod() + " not supported");
        }
    }

    private static AbstractSampler createTopKSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        // Get the data type of the key
        String dtype = neighborDataset.getDtype(sampleCondition.getKey()).toLowerCase();
        switch (dtype) {
            // If the data type is float, return a TopKSampler<Float>
            case "float":
                return new TopKSampler<Float>(sampleCondition, neighborDataset);
                // If the data type is long, return a TopKSampler<Long>
            case "long":
                return new TopKSampler<Long>(sampleCondition, neighborDataset);
                // If the data type is string, return a TopKSampler<String>
            case "string":
                return new TopKSampler<String>(sampleCondition, neighborDataset);
            default:
                // If the data type is not supported, throw an exception
                throw new IllegalArgumentException("Unsupported data type for key: " + sampleCondition.getKey());
        }
    }
}