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

package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter.parser.AbstractCmpWrapper;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseIndex implements Serializable {

  protected int[] originIndices = null;

  /*
   * indexType: can be any of [hash_index, range_index]
   * indexDtype: can be any of [long, float, string]
   * indexColumn: the column name of the index
   */
  private String indexType;
  private String indexDtype;
  private String indexColumn;

  public BaseIndex() {
  }

  public BaseIndex(String indexType, String indexColumn, String indexDtype) {
    this.indexType = indexType;
    this.indexColumn = indexColumn;
    this.indexDtype = indexDtype;
  }

  public byte[] dump() {
    ByteBuffer buffer = ByteBuffer.allocate(4 * (1 + originIndices.length));
    buffer.putInt(originIndices.length);
    for (int idx : originIndices) {
      buffer.putInt(idx);
    }
    return buffer.array();
  }

  public void load(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    int len = buffer.getInt();
    originIndices = new int[len];
    for (int i = 0; i < len; i++) {
      originIndices[i] = buffer.getInt();
    }
  }

  public int[] getOriginIndices() {
    return originIndices;
  }

  public void setOriginIndices(int[] originIndices) {
    this.originIndices = originIndices;
  }

  public String getIndexType() {
    return indexType;
  }

  public void setIndexType(String indexType) {
    this.indexType = indexType;
  }

  public String getIndexColumn() {
    return indexColumn;
  }

  public void setIndexColumn(String indexColumn) {
    this.indexColumn = indexColumn;
  }

  public String getIndexDtype() {
    return indexDtype;
  }

  public void setIndexDtype(String indexDtype) {
    this.indexDtype = indexDtype;
  }

  public int[] buildIndex(HeteroDataset neighborDataset) {
    originIndices = new int[neighborDataset.getArraySize()];
    for (int i = 0; i < neighborDataset.getArraySize(); i++) {
      originIndices[i] = i;
    }
    return originIndices;
  }

  public AbstractResult search(AbstractCmpWrapper cmpExpWrapper,
      Map<VariableSource, Map<String, Element.Number>> inputVariables,
      HeteroDataset neighborDataset) throws Exception {
    List<RangeUnit> ranges = new ArrayList<>();
    ranges.add(new RangeUnit(0, originIndices.length - 1));
    RangeResult rangeIndexResult = new RangeResult(this, ranges);
    return rangeIndexResult;
  }
}
