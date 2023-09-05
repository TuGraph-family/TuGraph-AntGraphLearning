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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommonResult extends AbstractResult {

  private List<Integer> indices = null;

  public CommonResult(BaseIndex index, List<Integer> sortedIndices) {
    super(index);
    this.indices = sortedIndices;
  }

  public static List<Integer> joinList(List<Integer> sortedList1, List<Integer> sortedList2) {
    Set<Integer> joinedList = new HashSet<>(sortedList1);
    joinedList.retainAll(new HashSet<>(sortedList2));
    return new ArrayList<>(joinedList);
  }

  public static List<Integer> unionList(List<Integer> sortedList1, List<Integer> sortedList2) {
    Set<Integer> unionList = new HashSet<>(sortedList1);
    unionList.addAll(sortedList2);
    return new ArrayList<>(unionList);
  }

  @Override
  public AbstractResult join(AbstractResult right) {
    List<Integer> joinedList = joinList(indices, right.getIndices());
    return new CommonResult(updateIndex(right), joinedList);
  }

  @Override
  public AbstractResult union(AbstractResult right) {
    List<Integer> unionedList = unionList(indices, right.getIndices());
    return new CommonResult(updateIndex(right), unionedList);
  }

  @Override
  public List<Integer> getIndices() {
    return indices;
  }

  @Override
  public int getOriginIndex(int i) {
    return i;
  }

  @Override
  public int getSize() {
    return indices.size();
  }
}
