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

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RangeUnitIndexResultTest {

  @Test
  public void testJoin() {
    List<Integer> ids = new ArrayList<>();
    List<Float> weights = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      ids.add(i);
      weights.add(1.0F * i);
    }
    HeteroDataset neighborDataset = new HeteroDataset(weights.size());
    neighborDataset.addAttributeList("weight", weights);
    BaseIndex index = new IndexFactory().createIndex("range_index:weight:float", neighborDataset);

    List<RangeUnit> ranges1 = new ArrayList<>();
    RangeUnit range1 = new RangeUnit(1, 3);
    RangeUnit range2 = new RangeUnit(2, 4);
    RangeUnit range3 = new RangeUnit(3, 5);
    ranges1.add(range1);
    ranges1.add(range2);
    ranges1.add(range3);
    RangeResult result1 = new RangeResult(index, ranges1);

    List<RangeUnit> ranges2 = new ArrayList<>();
    RangeUnit range4 = new RangeUnit(2, 5);
    RangeUnit range5 = new RangeUnit(4, 8);
    ranges2.add(range4);
    ranges2.add(range5);
    RangeResult result2 = new RangeResult(index, ranges2);

    AbstractResult joinedResult = result1.join(result2);

    List<RangeUnit> expectedRanges = new ArrayList<>();
    expectedRanges.add(new RangeUnit(2, 3));
    expectedRanges.add(new RangeUnit(4, 4));

    RangeResult expectedResult = new RangeResult(index, expectedRanges);
    Assert.assertEquals(expectedResult.getIndex().getIndexColumn(),
        joinedResult.getIndex().getIndexColumn());
    Assert.assertEquals(expectedResult.getIndices(), joinedResult.getIndices());
    List<RangeUnit> expectedRangeList = expectedResult.getRangeList();
    List<RangeUnit> resultRangeList = ((RangeResult) joinedResult).getRangeList();
    for (int i = 0; i < expectedRangeList.size(); i++) {
      Assert.assertEquals(expectedRangeList.get(i).getLow(), resultRangeList.get(i).getLow());
      Assert.assertEquals(expectedRangeList.get(i).getHigh(), resultRangeList.get(i).getHigh());
    }
  }
}
