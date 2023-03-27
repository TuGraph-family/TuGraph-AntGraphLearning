package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
/**
 SamplerFactory is a class used to create an instance of the Sampler class,
 depending on the type of sampler specified in the constructor.
 */
public class SamplerFactory {
    /**
     Creates an instance of the Sampler class, depending on the type of sampler specified
     in the parameter.
     @param sampleCondition The metadata used to determine the type of sampler
     @param index The Index used to create the sampler
     @return An instance of the Sampler class
     */
    public static Sampler createSampler(SampleCondition sampleCondition, BaseIndex index) {
        if (sampleCondition.getMethod().compareToIgnoreCase("weighted_sampler") == 0) {
            return new WeightedSampler(sampleCondition, index);
        } else if (sampleCondition.getMethod().compareToIgnoreCase("topk") == 0) {
            String dtype = index.getNeighborDataset().getDtype(sampleCondition.getKey());
            if (dtype.compareToIgnoreCase("float") == 0) {
                return new TopKSampler<Float>(sampleCondition, index);
            } else if (dtype.compareToIgnoreCase("long") == 0) {
                return new TopKSampler<Long>(sampleCondition, index);
            } else if (dtype.compareToIgnoreCase("string") == 0) {
                return new TopKSampler<String>(sampleCondition, index);
            }
        } else if (sampleCondition.getMethod().compareToIgnoreCase("random_sampler") == 0) {
            return new RandomSampler(sampleCondition, index);
        }
        return null;
    }
}
