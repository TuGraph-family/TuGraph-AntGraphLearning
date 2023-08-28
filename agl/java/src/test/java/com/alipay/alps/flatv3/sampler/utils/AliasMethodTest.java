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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AliasMethodTest {

  @Test
  public void testNextRandom() {
    List<Float> probabilities = new ArrayList<Float>(Arrays.asList(0.6F, 0.8F, 0.2F, 0.4F));

    AliasMethod aliasMethod = new AliasMethod(probabilities, new Random());
    int[] counts = new int[probabilities.size()];

    // Generate 1000000 samples and count occurrences of each bucket
    for (int i = 0; i < 10000000; i++) {
      int bucket = aliasMethod.nextSample();
      counts[bucket]++;
    }
    // Check that the proportions of occurrences are roughly equal to the given probabilities
    float sum = 0;
    for (int i = 0; i < probabilities.size(); i++) {
      sum += probabilities.get(i);
    }
    for (int i = 0; i < probabilities.size(); i++) {
      float proportion = (float) counts[i] / 10000000;
      Assert.assertEquals(probabilities.get(i) / sum, proportion, 0.01);
    }
  }
}

