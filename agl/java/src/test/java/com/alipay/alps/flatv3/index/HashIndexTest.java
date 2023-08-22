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

import com.alipay.alps.flatv3.filter.parser.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter.parser.FilterConditionParser;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class HashIndexTest {

  @Test
  public void testHashIndex() throws Exception {
    // Create a neighbor dataset
    HeteroDataset neighborDataset = new HeteroDataset(5);

    // Add some attributes to the neighbor dataset
    List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
    neighborDataset.addAttributeList("node_type", typeList);

    // Create a hash index
    BaseIndex hashIndex = new IndexFactory()
        .createIndex("hash_index:node_type:string", neighborDataset);

    Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
    String filterCond = "index.node_type in (user, shop)";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
    AbstractResult indexResult = hashIndex
        .search(new CategoryCmpWrapper(cmpExp), inputVariables, neighborDataset);
    assertArrayEquals(Arrays.asList(1, 2, 4).toArray(), indexResult.getIndices().toArray());
  }
}

