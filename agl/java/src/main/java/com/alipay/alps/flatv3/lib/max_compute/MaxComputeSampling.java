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

package com.alipay.alps.flatv3.lib.max_compute;

import com.alipay.alps.flatv3.lib.GNNSamplingLib;
import com.alipay.alps.flatv3.lib.WriteSubGraphElement;
import com.aliyun.odps.udf.UDTFCollector;
import java.util.List;
import java.util.Map;

public class MaxComputeSampling implements WriteSubGraphElement {

  private Map<String, Integer> neighborColumnIndex;
  private Map<String, Integer> seedColumnIndex;
  private GNNSamplingLib gnnSamplingLib;
  private UDTFCollector udtfCollector;

  public MaxComputeSampling(GNNSamplingLib gnnSamplingLib, UDTFCollector udtfCollector) {
    this.gnnSamplingLib = gnnSamplingLib;
    this.udtfCollector = udtfCollector;
  }

  public void setNeighborColumnIndex(Map<String, Integer> neighborColumnIndex) {
    this.neighborColumnIndex = neighborColumnIndex;
  }

  public void setSeedColumnIndex(Map<String, Integer> seedColumnIndex) {
    this.seedColumnIndex = seedColumnIndex;
  }

  public Map<String, Integer> getNeighborColumnIndex() {
    return neighborColumnIndex;
  }

  public Map<String, Integer> getSeedColumnIndex() {
    return seedColumnIndex;
  }

  public Object[] buildIndexes(Object neighborData[], List<String> indexMetas) {
    Object[] ans = new Object[indexMetas.size()];
    for (int i = 0; i < indexMetas.size(); i++) {
      String indexMeta = indexMetas.get(i);
      String indexColumn = indexMeta.split(":")[1];
      List valueList = (List) neighborData[getNeighborColumnIndex().get(indexColumn)];
      ans[i] = gnnSamplingLib.buildIndex(indexMeta, valueList);
    }
    return ans;
  }

  @Override
  public void write(Object... values) {
    udtfCollector.collect(values);
  }
}
