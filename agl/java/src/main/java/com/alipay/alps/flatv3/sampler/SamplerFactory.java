/**
 * Copyright 2023 AntGroup CO., Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.HeteroDataset;

public class SamplerFactory {

  public static AbstractSampler createSampler(SampleCondition sampleCondition,
      HeteroDataset neighborDataset) {
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
        throw new IllegalArgumentException(
            "Sampling method " + sampleCondition.getMethod() + " not supported");
    }
  }

  private static AbstractSampler createTopKSampler(SampleCondition sampleCondition,
      HeteroDataset neighborDataset) {
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
        throw new IllegalArgumentException(
            "Unsupported data type for key: " + sampleCondition.getKey());
    }
  }
}