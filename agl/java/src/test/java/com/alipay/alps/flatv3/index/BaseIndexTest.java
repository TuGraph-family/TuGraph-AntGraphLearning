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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.alipay.alps.flatv3.filter.result.AbstractResult;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class BaseIndexTest {

  private HeteroDataset neighborDataset = null;
  private Integer[] originIndex = null;

  @Before
  public void setUp() {
    neighborDataset = new HeteroDataset(4);
    neighborDataset.addAttributeList("attr1", Arrays.asList("val1", "val2", "val3", "val4"));
    neighborDataset.addAttributeList("attr2", Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f));
    neighborDataset.addAttributeList("attr3", Arrays.asList(1L, 2L, 3L, 4L));
    originIndex = new Integer[]{0, 1, 2, 3};
  }

  @Test
  public void testBuildIndex() {
    BaseIndex baseIndex = new IndexFactory()
        .createIndex("hash_index:attr1:string", neighborDataset);
    assertEquals("hash_index", baseIndex.getIndexType());
    assertEquals("attr1", baseIndex.getIndexColumn());
    assertEquals("string", baseIndex.getIndexDtype());
  }

  @Test
  public void testGetters() {
    BaseIndex baseIndex = new IndexFactory()
        .createIndex("range_index:attr2:float", neighborDataset);
    assertEquals("range_index", baseIndex.getIndexType());
    assertEquals("attr2", baseIndex.getIndexColumn());
    assertEquals("float", baseIndex.getIndexDtype());
  }

  @Test
  public void testGetDtype() {
    BaseIndex baseIndex = new IndexFactory()
        .createIndex("range_index:attr2:float", neighborDataset);
  }

  // test for a case with empty indexMeta and no filter conditions
  @Test
  public void testGetIndexResultWithEmptyIndexMetaAndNOFilter() throws Exception {
    BaseIndex baseIndex = new IndexFactory().createIndex("", neighborDataset);
    AbstractResult indexResult = baseIndex.search(null, null, neighborDataset);
    assertEquals(4, indexResult.getSize());
    assertArrayEquals(originIndex, indexResult.getIndices().toArray());
  }
}

