package com.alipay.alps.flatv3.sampler;

import java.util.Map;

import com.alipay.alps.flatv3.index.BaseIndex;
public class SamplerFactory {
    public static Sampler createSampler(SampleCondition sampleCondition, Map<String, BaseIndex> indexes) {
        // If the sampling method is weighted_sampler, return a WeightedSampler
        if (sampleCondition.getMethod().compareToIgnoreCase("weighted_sampler") == 0) {
            return new WeightedSampler(sampleCondition, indexes);
        // If the sampling method is topk, return a TopKSampler
        } else if (sampleCondition.getMethod().compareToIgnoreCase("topk") == 0) {
            return createTopKSampler(sampleCondition, indexes);
        // If the sampling method is random_sampler, return a RandomSampler
        } else if (sampleCondition.getMethod().compareToIgnoreCase("random_sampler") == 0) {
            return new RandomSampler(sampleCondition, indexes);
        // If the sampling method is not supported, throw an exception
        } else {
            throw new IllegalArgumentException("Sampling method " + sampleCondition.getMethod() + " not supported");
        }
    }

    private static Sampler createTopKSampler(SampleCondition sampleCondition, Map<String, BaseIndex> indexes) {
        // Get the data type of the key
        String dtype = getDtype(indexes, sampleCondition.getKey());
        // If the data type is float, return a TopKSampler<Float>
        if (dtype.compareToIgnoreCase("float") == 0) {
            return new TopKSampler<Float>(sampleCondition, indexes);
        // If the data type is long, return a TopKSampler<Long>
        } else if (dtype.compareToIgnoreCase("long") == 0) {
            return new TopKSampler<Long>(sampleCondition, indexes);
        // If the data type is string, return a TopKSampler<String>
        } else if (dtype.compareToIgnoreCase("string") == 0) {
            return new TopKSampler<String>(sampleCondition, indexes);
        }
        // If the data type is not supported, throw an exception
        throw new IllegalArgumentException("Unsupported data type for key: " + sampleCondition.getKey());
    }
    private static String getDtype(Map<String, BaseIndex> indexes, String key) {
        // Loop through each index
        for (BaseIndex index : indexes.values()) {
            // Get the data type of the key
            String dtype = index.getDtype(key);
            if (dtype != null) {
                return dtype;
            }
        }
        return null;
    }
}