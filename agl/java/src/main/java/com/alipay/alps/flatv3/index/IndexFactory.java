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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexFactory {

  public static final String HASH_INDEX = "hash_index";
  public static final String RANGE_INDEX = "range_index";
  public static final String NO_FILTER = "";

  /**
   * @param indexMeta:       index_type:column_name:index_dtype
   * @param neighborDataset: neighbor dataset
   * @return an index
   */
  public BaseIndex createIndex(String indexMeta, HeteroDataset neighborDataset) {
    String t[] = indexMeta.split(":");
    String indexColumn = NO_FILTER;
    String indexDtype = "";
    if (t.length > 2) {
      indexColumn = t[1];
      indexDtype = t[2];
    }
    return getOrCreate(t[0], indexColumn, indexDtype, neighborDataset);
  }

  /**
   * @param indexMetas:      a list of index metas, it will introduce multiple indexes
   * @param neighborDataset: neighbor dataset
   * @return a map of index column and index
   */
  public Map<String, BaseIndex> getIndexesMap(List<String> indexMetas,
      HeteroDataset neighborDataset) {
    Map<String, BaseIndex> indexesMap = new HashMap<>();
    if (indexMetas == null || indexMetas.size() == 0) {
      BaseIndex index = createIndex("", neighborDataset);
      indexesMap.put(index.getIndexColumn(), index);
    } else {
      for (String indexMeta : indexMetas) {
        BaseIndex index = createIndex(indexMeta, neighborDataset);
        indexesMap.put(index.getIndexColumn(), index);
      }
    }
    return indexesMap;
  }

  // if indexColumn can be found in indexesMap, return the index
  // else create a new index, update indexesMap and return the index
  private BaseIndex getOrCreate(String indexType, String indexColumn, String indexDtype,
      HeteroDataset neighborDataset) {
    BaseIndex index = null;
    switch (indexType) {
      case HASH_INDEX:
        index = new HashIndex(indexType, indexColumn, indexDtype);
        break;
      case RANGE_INDEX:
        index = new RangeIndex(indexType, indexColumn, indexDtype);
        break;
      case NO_FILTER:
        index = new BaseIndex(NO_FILTER, indexColumn, indexDtype);
        break;
      default:
        throw new RuntimeException("Not support hash_range_index yet");
    }
    index.buildIndex(neighborDataset);
    return index;
  }

  public BaseIndex loadIndex(String indexMeta, byte[] buf) {
    String t[] = indexMeta.split(":");
    String indexColumn = NO_FILTER;
    String indexDtype = "";
    if (t.length > 2) {
      indexColumn = t[1];
      indexDtype = t[2];
    }
    return getOrCreate(t[0], indexColumn, indexDtype, buf);
  }

  private BaseIndex getOrCreate(String indexType, String indexColumn, String indexDtype,
      byte[] buf) {
    BaseIndex index = null;
    switch (indexType) {
      case HASH_INDEX:
        index = new HashIndex(indexType, indexColumn, indexDtype);
        break;
      case RANGE_INDEX:
        index = new RangeIndex(indexType, indexColumn, indexDtype);
        break;
      case NO_FILTER:
        index = new BaseIndex(NO_FILTER, indexColumn, indexDtype);
        break;
      default:
        throw new RuntimeException("Not support hash_range_index yet");
    }
    index.load(buf);
    return index;
  }
}
