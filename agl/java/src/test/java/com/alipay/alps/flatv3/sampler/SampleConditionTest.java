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

import org.testng.Assert;
import org.testng.annotations.Test;

public class SampleConditionTest {

  @Test
  public void testConstructor() {
    String sampleMeta = "weighted_sample(by=index.weight, limit=5, reverse=true, replacement=False)";
    SampleCondition sampleCondition = new SampleCondition(sampleMeta);
    Assert.assertEquals("weighted_sample", sampleCondition.getMethod());
    Assert.assertEquals("index.weight", sampleCondition.getKey());
    Assert.assertEquals(5, sampleCondition.getLimit());
    Assert.assertFalse(sampleCondition.isReplacement());
    Assert.assertTrue(sampleCondition.isReverse());
  }

  @Test
  public void testToString() {
    SampleCondition sampleCondition = new SampleCondition("weighted_sample", "index.weight", 5,
        false, true, 34);
    String expected = "SampleCondition{method='weighted_sample', key='index.weight', limit=5, replacement=false, reverse=true, randomSeed=34}";
    Assert.assertEquals(expected, sampleCondition.toString());
  }
}