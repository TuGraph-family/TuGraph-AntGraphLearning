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

import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.index.HeteroDataset;
import java.util.List;
import java.util.Random;

/**
 * Sampler is an abstract class that provides a template for sampling data from an AbstractIndexResult
 */
public abstract class AbstractSampler {

  // SampleCondition object that stores the properties used for sampling
  private final SampleCondition sampleCondition;
  // neighborDataset object that stores the data used for sampling
  private final HeteroDataset neighborDataset;
  // Random object used to generate a random seed
  private Random rand = null;

  // If it is larger than this threshold, we will delete the sampled node from the candidate set physically
  // If sample ratio is less than this threshold,
  // we will maintain a distinct set of samples to perform sampling with replacement without deleting the sampled node physically.
  protected final Float sampleCountToCandidateCountRatio = 0.25f;

  /**
   * Constructor for Sampler class with two parameters
   *
   * @param sampleCondition SampleCondition object for storing the properties used for sampling
   * @param neighborDataset NeighborDataset object for storing the data used for sampling
   */
  public AbstractSampler(SampleCondition sampleCondition, HeteroDataset neighborDataset) {
    this.sampleCondition = sampleCondition;
    this.neighborDataset = neighborDataset;
//        this.rand = new Random();
    this.rand = new Random(sampleCondition.getRandomSeed());
  }

  /**
   * Abstract method used to sample data from an indexResult
   *
   * @param indexResult IndexResult object used when sampling data
   * @return ArrayList of integers containing the sampled data
   */
  public abstract List<Integer> sample(AbstractResult indexResult);

  // Getter for SampleCondition object
  public SampleCondition getSampleCondition() {
    return sampleCondition;
  }

  // Getter for random object
  public Random getRand() {
    return rand;
  }

  // Getter for NeighborDataset object
  public HeteroDataset getNeighborDataset() {
    return neighborDataset;
  }

  public int getNextRandomInt(int bound) {
    return rand.nextInt(bound);
  }
}
