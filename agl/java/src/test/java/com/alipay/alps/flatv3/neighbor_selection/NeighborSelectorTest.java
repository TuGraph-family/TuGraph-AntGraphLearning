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

package com.alipay.alps.flatv3.neighbor_selection;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class NeighborSelectorTest {

  private HeteroDataset neighborDataset;
  List<Integer> indices = new ArrayList<>();
  List<String> ids = new ArrayList<>();
  List<Long> times = new ArrayList<>();
  List<String> types = Arrays
      .asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");

  @BeforeTest
  public void setUp() {
    for (int i = 0; i < 10; i++) {
      ids.add(String.valueOf(i));
      times.add(2L * i);
      indices.add(i);
    }
    neighborDataset = new HeteroDataset(ids.size());
    neighborDataset.addAttributeList("time", times);
    neighborDataset.addAttributeList("type", types);
  }

  @Test
  public void testNoFilterTopkSampler() throws Exception {
    // Create the list of seed IDs as strings.
    List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");

    // Create the list of seed attributes as lists of objects.
    HeteroDataset seedAttrs = new HeteroDataset(seedIds.size());
    List<Long> seedTimes = new ArrayList<>();
    for (int i = 0; i < seedIds.size(); i++) {
      seedTimes.add((i + 1) * 1L);
    }
    seedAttrs.addAttributeList("time", seedTimes);

    // Create the filter condition.
    String filterCond = "";

    // Create the sample condition.
    String sampleCond = "topk(by=time, limit=2)";

    // Create the PropagateSeed object.

    Filter filter = new Filter(filterCond);
    NeighborSelector neighborSelector = new NeighborSelector("", filter,
        new SampleCondition(sampleCond));

    // Run the algorithm.
    Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(null, neighborDataset);
    List<List<Integer>> sampledNeighbors = neighborSelector
        .process(indexMap, neighborDataset, seedIds, seedAttrs);

    // Create the list of expected results.
    Assert.assertEquals(Arrays.asList(0, 1), sampledNeighbors.get(0));
    Assert.assertEquals(Arrays.asList(0, 1), sampledNeighbors.get(1));
    Assert.assertEquals(Arrays.asList(0, 1), sampledNeighbors.get(2));
    Assert.assertEquals(Arrays.asList(0, 1), sampledNeighbors.get(3));
    Assert.assertEquals(Arrays.asList(0, 1), sampledNeighbors.get(4));
  }

  @Test
  public void testRangeFilterTopkSampler() throws Exception {
    // add 1 range index meta
    List<String> indexMetas = new ArrayList<>();
    indexMetas.add("range_index:time:long");

    // create seed ids
    List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");

    // create seed attrs
    HeteroDataset seedAttrs = new HeteroDataset(seedIds.size());
    List<Long> seedTimes = new ArrayList<>();
    for (int i = 0; i < seedIds.size(); i++) {
      seedTimes.add((i + 1) * 1L);
    }
    seedAttrs.addAttributeList("time", seedTimes);

    // create filter condition
    String filterCond = "index.time - seed.time >= 0.5 AND index.time <= seed.time + 11";

    // create sample condition
    String sampleCond = "topk(by=time, limit=2)";

    // create propagate seed
    Filter filter = new Filter(filterCond);
    NeighborSelector neighborSelector = new NeighborSelector("", filter,
        new SampleCondition(sampleCond));

    // process propagate seed
    Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
    List<List<Integer>> sampledNeighbors = neighborSelector
        .process(indexMap, neighborDataset, seedIds, seedAttrs);

    // create expected result
    Assert.assertEquals(Arrays.asList(1, 2), sampledNeighbors.get(0));
    Assert.assertEquals(Arrays.asList(2, 3), sampledNeighbors.get(1));
    Assert.assertEquals(Arrays.asList(2, 3), sampledNeighbors.get(2));
    Assert.assertEquals(Arrays.asList(3, 4), sampledNeighbors.get(3));
    Assert.assertEquals(Arrays.asList(3, 4), sampledNeighbors.get(4));
  }

  @Test
  public void testRangeTypeFilterTopkSampler() throws Exception {
    // add a range index meta and a hash index meta
    List<String> indexMetas = new ArrayList<>();
    indexMetas.add("range_index:time:long");
    indexMetas.add("hash_index:type:string");

    // the attributes of the seeds
    List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");

    HeteroDataset seedAttrs = new HeteroDataset(seedIds.size());
    List<Long> seedTimes = new ArrayList<>();
    for (int i = 0; i < seedIds.size(); i++) {
      seedTimes.add((i + 1) * 1L);
    }
    seedAttrs.addAttributeList("time", seedTimes);

    // the filter condition for the propagation
    String filterCond = "index.time - seed.time >= 0.5 AND index.time <= seed.time + 11 OR index.type in ('item', 'user')";

    // the sample condition for the propagation
    String sampleCond = "topk(by=time, limit=3)";

    Filter filter = new Filter(filterCond);
    NeighborSelector neighborSelector = new NeighborSelector("", filter,
        new SampleCondition(sampleCond));

    // the result of the propagation
    Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
    List<List<Integer>> sampledNeighbors = neighborSelector
        .process(indexMap, neighborDataset, seedIds, seedAttrs);
    List<List<String>> chosenNeighbors = new ArrayList<>();
    for (int seedIdx = 0; seedIdx < seedIds.size(); seedIdx++) {
      List<Integer> chosenNeighbor = sampledNeighbors.get(seedIdx);
      List<String> neighborIds = new ArrayList<>();
      for (Integer neighborIdx : chosenNeighbor) {
        neighborIds.add(ids.get(neighborIdx));
      }
      chosenNeighbors.add(neighborIds);
    }
    Collections.shuffle(indices);
    for (int i = 0; i < 10; i++) {
      while (indices.get(i) != i) {
        int newIdx = indices.get(i);
        Collections.swap(ids, i, newIdx);
        Collections.swap(times, i, newIdx);
        Collections.swap(types, i, newIdx);
        Collections.swap(indices, i, newIdx);
      }
    }
    HeteroDataset newNeighborDataset = new HeteroDataset(times.size());
    newNeighborDataset.addAttributeList("new_time", times);
    newNeighborDataset.addAttributeList("new_type", types);

    String newFilterCond = "index.new_time - seed.time >= 0.5 AND index.new_time <= seed.time + 11 OR index.new_type in ('item', 'user')";
    Filter newFilter = new Filter(newFilterCond);
    String newSampleCond = "topk(by=new_time, limit=3)";

    List<String> newIndexMetas = new ArrayList<>();
    newIndexMetas.add("range_index:new_time:long");
    newIndexMetas.add("hash_index:new_type:string");
    Map<String, BaseIndex> newIndexMap = new IndexFactory()
        .getIndexesMap(newIndexMetas, newNeighborDataset);
    NeighborSelector newNeighborSelector = new NeighborSelector("", newFilter,
        new SampleCondition(newSampleCond));
    List<List<Integer>> sampledNeighbors2 = newNeighborSelector
        .process(newIndexMap, newNeighborDataset, seedIds, seedAttrs);
    List<List<String>> newChosenNeighbors = new ArrayList<>();
    for (int seedIdx = 0; seedIdx < seedIds.size(); seedIdx++) {
      List<Integer> chosenNeighbor = sampledNeighbors2.get(seedIdx);
      List<String> neighborIds = new ArrayList<>();
      for (Integer neighborIdx : chosenNeighbor) {
        neighborIds.add(ids.get(neighborIdx));
      }
      newChosenNeighbors.add(neighborIds);
    }
    Assert.assertEquals(chosenNeighbors, newChosenNeighbors);
  }

  @Test
  public void testTypeFilterTopkSampler() throws Exception {
    List<String> types = Arrays.asList("shop", "item", "user");
    HeteroDataset neighborDataset = new HeteroDataset(types.size());
    neighborDataset.addAttributeList("type", types);
    // add a range index meta and a hash index meta  index.type in (user, item);
    List<String> indexMetas = new ArrayList<>();
    indexMetas.add("hash_index:type:string");

    // the attributes of the seeds
    List<String> seedIds = Arrays.asList("4", "2", "3");

    HeteroDataset seedAttrs = new HeteroDataset(seedIds.size());

    // the filter condition for the propagation
    String filterCond = "index.type in (item)";

    // the sample condition for the propagation
    String sampleCond = "random_sampler(limit=3)";

    Filter filter = new Filter(filterCond);
    NeighborSelector neighborSelector = new NeighborSelector("", filter,
        new SampleCondition(sampleCond));

    // the result of the propagation
    Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
    List<List<Integer>> sampledNeighbors = neighborSelector
        .process(indexMap, neighborDataset, seedIds, seedAttrs);

    Assert.assertEquals(Arrays.asList(1), sampledNeighbors.get(0));
    Assert.assertEquals(Arrays.asList(1), sampledNeighbors.get(1));
    Assert.assertEquals(Arrays.asList(1), sampledNeighbors.get(2));
  }
}