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
import java.util.List;

public abstract class AbstractResult {

  private final BaseIndex index;

  public AbstractResult(BaseIndex index) {
    this.index = index;
  }

  public BaseIndex getIndex() {
    return index;
  }

  public abstract AbstractResult join(AbstractResult right);

  public abstract AbstractResult union(AbstractResult right);

  public abstract int getSize();

  public abstract List<Integer> getIndices();

  public abstract int getOriginIndex(int i);

  protected BaseIndex updateIndex(AbstractResult right) {
    BaseIndex finalIndex;
    BaseIndex leftIndex = getIndex();
    if (leftIndex != null && leftIndex.getIndexColumn() != null) {
      finalIndex = leftIndex;
    } else {
      finalIndex = right.getIndex();
    }
    return finalIndex;
  }

  public boolean hasFilterCondition() {
    return index != null && index.getIndexColumn() != null && !index.getIndexColumn().isEmpty();
  }

}
