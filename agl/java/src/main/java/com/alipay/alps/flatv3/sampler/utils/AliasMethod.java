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

package com.alipay.alps.flatv3.sampler.utils;

import java.util.List;
import java.util.Random;

/**
 * A data structure that implements the Alias method for generating random samples without replacement from a discrete probability distribution.
 * The algorithm creates two tables, one with probabilities and one with corresponding indices for each element in the distribution.
 * The alias table is created such that the table has entries with probabilities equal to or greater than the average of the distribution, and each entry is paired with another entry with a probability less than the average.
 * The data structure is then used to generate random samples from the probability distribution using the nextRandom() method.
 */
public class AliasMethod {

  // Pre-computed probability and alias tables
  public float[] probabilityTable;
  private int[] aliasTable;
  private Random rand;

  public AliasMethod(List<Float> weights, Random rand) {
    this.rand = rand;
    int n = weights.size();
    probabilityTable = new float[n];
    aliasTable = new int[n];
    Float sum = 0F;
    for (Float w : weights) {
      sum += w;
    }
    for (int i = 0; i < weights.size(); i++) {
      probabilityTable[i] = weights.get(i) / sum;
    }
    initAlias();
  }

  /**
   * Initializes the probability and alias tables for the given probability distribution.
   * 1, All buckets contain 1/n liquids
   * 2, each bucket contains at most 2 different kinds of liquids
   */
  public void initAlias() {
    int n = probabilityTable.length;
    float average = 1.0F / n;
    // Set up the probability and alias tables
    int[] small = new int[n];
    int[] large = new int[n];

    int smallSize = 0;
    int largeSize = 0;
    for (int i = 0; i < n; i++) {
      if (probabilityTable[i] >= average) {
        large[largeSize++] = i;
      } else {
        small[smallSize++] = i;
      }
    }

    while (smallSize > 0 && largeSize > 0) {
      int l = small[--smallSize];
      int g = large[--largeSize];
      float gLeft = probabilityTable[l] + probabilityTable[g] - average;
      aliasTable[l] = g;

      probabilityTable[g] = gLeft;
      if (gLeft >= average) {
        large[largeSize++] = g;
      } else {
        small[smallSize++] = g;
      }
    }

    while (largeSize > 0) {
      probabilityTable[large[--largeSize]] = average;
    }
    while (smallSize > 0) {
      probabilityTable[small[--smallSize]] = average;
    }
  }

  /**
   * Generates a random sample from the probability distribution using the Alias method.
   *
   * @return An integer representing the index of the sampled element.
   */
  public int nextSample() {
    int n = probabilityTable.length;
    int i = rand.nextInt(n);

    float x = rand.nextFloat() / n;
    if (x < probabilityTable[i]) {
      return i;
    } else {
      return aliasTable[i];
    }
  }
}
