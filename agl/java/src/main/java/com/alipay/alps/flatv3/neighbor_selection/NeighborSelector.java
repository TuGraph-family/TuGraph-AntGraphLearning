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
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.sampler.AbstractSampler;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.sampler.SamplerFactory;
import com.antfin.agl.proto.sampler.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// this is a api class for users to propagate seeds to next hop neighbors by using the indexes, filters and samplers.
public class NeighborSelector {

  private Filter filter = null;
  private AbstractSampler sampler = null;
  private SampleCondition sampleCond = null;
  private String otherOutputSchema;

  /*
   * Constructor of PropagateSeed.
   * @param otherOutputSchema: schema of output, e.g. "id:STRING,age:INT", which is used to
   *                           build the index for neighbor table.
   * @param Filter:            a filter object
   * @param sampleCond:        a sample condition, e.g. "topk(by=time, limit=5)".
   */
  public NeighborSelector(String otherOutputSchema, Filter filter, SampleCondition sampleCond)
      throws Exception {
    this.otherOutputSchema = otherOutputSchema.replaceAll("\\s", "");
    this.filter = filter;
    this.sampleCond = sampleCond;
  }

  public NeighborSelector(Filter filter, SampleCondition sampleCond) throws Exception {
    this.filter = filter;
    this.sampleCond = sampleCond;
  }

  public void initSampler(HeteroDataset neighborDataset) {
    this.sampler = SamplerFactory.createSampler(sampleCond, neighborDataset);
  }

  /*
   * This method will select a list of neighbor indices for each seed.
   * @param indexesMap: a map of indexes, e.g. {"age": ageIndex, "type": typeIndex}
   * @param neighborDataset: neighbor dataset
   * @param seeds:           a list of seeds, e.g. ["1", "2", "3"]
   * @param seedAttrs:       a list of seed attributes, e.g. ["1,18", "2,20", "3,25"]
   * @param frontierValues:  a map used to store the frontier values. e.g. {"age": 20}
   * @param neighborAttrs:   a list of neighbor attributes. e.g. ["1,18", "2,20", "3,25"]
   * @return a SampleOutput object, which contains the sampled neighbor indices. e.g. [[1, 2, 3], [4, 5, 6], [2, 4]]
   */
  public List<List<Integer>> process(Map<String, BaseIndex> indexesMap,
      HeteroDataset neighborDataset,
      List<String> seeds, HeteroDataset seedAttrs) throws Exception {
    List<List<Integer>> sampledNeighbors = new ArrayList<>();
    AbstractSampler sampler = SamplerFactory.createSampler(sampleCond, neighborDataset);
    for (int seedIndex = 0; seedIndex < seeds.size(); seedIndex++) {
      AbstractResult indexResult = null;
      if (filter == null) {
        List<RangeUnit> ranges = new ArrayList<>();
        ranges.add(new RangeUnit(0, neighborDataset.getArraySize() - 1));
        indexResult = new RangeResult(null, ranges);
      } else {
        Map<String, Element.Number> seedVariableMap = seedAttrs.fillVariables(seedIndex);
        indexResult = filter.filter(seedVariableMap, neighborDataset, indexesMap);
      }
      List<Integer> neighborIndices = sampler.sample(indexResult);
      sampledNeighbors.add(neighborIndices);
    }
    return sampledNeighbors;
  }

  public List<Integer> chooseNeighbor(Map<String, BaseIndex> indexesMap,
      HeteroDataset neighborDataset, Map<String, Element.Number> seedVariableMap) throws Exception {
    AbstractResult indexResult = null;
    if (filter == null) {
      List<RangeUnit> ranges = new ArrayList<>();
      ranges.add(new RangeUnit(0, neighborDataset.getArraySize() - 1));
      indexResult = new RangeResult(null, ranges);
    } else {
      indexResult = filter.filter(seedVariableMap, neighborDataset, indexesMap);
    }
    List<Integer> neighborIndices = sampler.sample(indexResult);
    return neighborIndices;
  }
}
