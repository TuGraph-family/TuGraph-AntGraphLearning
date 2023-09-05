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

package com.alipay.alps.flatv3.filter.result;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CommonIndexResultTest {

  private BaseIndex index;
  private CommonResult result;

  @Before
  public void setup() {
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      ids.add(i);
    }
    HeteroDataset neighborDataset = new HeteroDataset(5);
    index = new IndexFactory().createIndex("", neighborDataset);
    result = new CommonResult(index, Arrays.asList(1, 2, 3, 4, 5));
  }

  @Test
  public void testJoin() {
    CommonResult otherResult = new CommonResult(index, Arrays.asList(3, 4, 5, 6, 7));
    AbstractResult joinedResult = result.join(otherResult);
    assertArrayEquals(Arrays.asList(3, 4, 5).toArray(), joinedResult.getIndices().toArray());
  }

  @Test
  public void testUnion() {
    CommonResult otherResult = new CommonResult(index, Arrays.asList(3, 4, 5, 6, 7));
    AbstractResult unionResult = result.union(otherResult);

    assertArrayEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7).toArray(),
        unionResult.getIndices().toArray());
  }

  @Test
  public void testGetSize() {
    assertEquals(5, result.getSize());
  }

  @Test
  public void testGetIndices() {
    assertArrayEquals(Arrays.asList(1, 2, 3, 4, 5).toArray(), result.getIndices().toArray());
  }
}
