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
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.index.HeteroDataset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * TopKSampler is a class extending Sampler class.
 * It is used to get the top K elements from a given set of data that meets a given condition.
 * It has a PriorityQueue to store the top K elements and a comparator.
 * The sample() method is overridden to find the top K elements from the given data.
 * The isReverse attribute is used to determine the order of the elements in the PriorityQueue.
 */
public class TopKSampler<T extends Comparable<T>> extends AbstractSampler {

  private List<T> weights = null;
  private PriorityQueue<Integer> priorityQueue = null;
  private Comparator<Integer> comparator = null;
  private ArrayList<Integer> cachedIndex = null;

  public TopKSampler(SampleCondition sampleCondition, HeteroDataset neighborDataset) {
    super(sampleCondition, neighborDataset);
  }

  private void setupPriorityQueue(SampleCondition sampleCondition) {
    int isReverse = sampleCondition.isReverse() ? -1 : 1;
    comparator = new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return weights.get(o1).compareTo(weights.get(o2)) * isReverse;
      }
    };
    priorityQueue = new PriorityQueue<>(sampleCondition.getLimit(), comparator);
  }

  @Override
  public List<Integer> sample(AbstractResult indexResult) {
    int sampleCount = this.getSampleCondition().getLimit();
    ArrayList<Integer> sampledIndex = new ArrayList<>();
    String originIndexColumn =
        (indexResult.getIndex() == null) ? null : indexResult.getIndex().getIndexColumn();
    if (indexResult instanceof RangeResult && originIndexColumn != null
        && originIndexColumn.compareTo(getSampleCondition().getKey()) == 0) {
      // reuse sorted neighbors in indexing stage
      if (indexResult instanceof RangeResult) {
        List<RangeUnit> sortedIntervals = ((RangeResult) indexResult).getRangeList();
        if (getSampleCondition().isReverse()) {
          for (int i = sortedIntervals.size() - 1; i >= 0; i--) {
            RangeUnit range = sortedIntervals.get(i);
            for (int j = range.getHigh(); j >= range.getLow() && sampledIndex.size() < sampleCount;
                j--) {
              sampledIndex.add(indexResult.getOriginIndex(j));
            }
          }
        } else {
          for (RangeUnit range : sortedIntervals) {
            for (int i = range.getLow(); i <= range.getHigh() && sampledIndex.size() < sampleCount;
                i++) {
              sampledIndex.add(indexResult.getOriginIndex(i));
            }
          }
        }
      } else {
        List<Integer> sortedIndices = indexResult.getIndices();
        if (sortedIndices.size() <= sampleCount) {
          sampledIndex.addAll(sortedIndices);
          if (getSampleCondition().isReverse()) {
            Collections.reverse(sampledIndex);
          }
        }
        if (getSampleCondition().isReverse()) {
          for (int i = sortedIndices.size() - 1; i >= 0 && sampledIndex.size() < sampleCount; i--) {
            sampledIndex.add(sortedIndices.get(i));
          }
        } else {
          for (int i = 0; i < sortedIndices.size() && sampledIndex.size() < sampleCount; i++) {
            sampledIndex.add(sortedIndices.get(i));
          }
        }
      }
    } else {
      // sort neighbors by attribute at runtime
      // if there is no filter condition, the selected samples are always the same, we can cache the result
      if (originIndexColumn == null && cachedIndex != null) {
        return new ArrayList<>(cachedIndex); // 一个节点上复用
      }
      if (weights == null) {
        weights = getNeighborDataset().getAttributeList(getSampleCondition().getKey());
      }
      if (priorityQueue == null) {
        setupPriorityQueue(getSampleCondition());
      }
      priorityQueue.clear();

      for (int idx : indexResult.getIndices()) {
        priorityQueue.add(idx);
      }
      for (int i = 0; i < sampleCount && !priorityQueue.isEmpty(); i++) {
        sampledIndex.add(priorityQueue.poll());
      }
      if (originIndexColumn == null && cachedIndex == null) {
        cachedIndex = new ArrayList<>(sampledIndex);
      }
    }
    return sampledIndex;
  }
}
