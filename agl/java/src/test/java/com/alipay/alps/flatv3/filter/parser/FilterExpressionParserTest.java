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

package com.alipay.alps.flatv3.filter.parser;

import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.LogicOp;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class FilterExpressionParserTest {

  @Test
  public void testEmptyFilterCond() {
    String filterCond = "";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    Assert.assertEquals(logicExps.getExpRPNCount(), 0);
  }

  @Test
  public void testTypeFilter() {
    String filterCond = "index.type in (node, item)";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
    Assert.assertEquals(expOps.size(), 1);
    Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.IN);
  }

  @Test
  public void testTimeStampRangeFilter() {
    String filterCond = "INDEX.TIME - SEED.TIME >= 0.5 AND INDEX.TIME <= SEED.TIME + 11";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
    Assert.assertEquals(expOps.size(), 3);
    Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
    Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.LE);
    Assert.assertEquals(expOps.get(2),
        LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("AND")).build());
  }

  @Test
  public void testUnionFilters() {
    String filterCond = "index.time - seed.time >= 0.5 or index.type not in (node, item)";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
    Assert.assertEquals(expOps.size(), 3);
    Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
    Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.NOT_IN);
    Assert.assertEquals(expOps.get(2),
        LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("OR")).build());
  }

  @Test
  public void testJoinFilters() {
    String filterCond = "index.time - seed.time >= 0.5 and index.type not in (node, item)";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
    Assert.assertEquals(expOps.size(), 3);
    Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
    Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.NOT_IN);
    Assert.assertEquals(expOps.get(2),
        LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("AND")).build());
  }

  @Test
  public void testJoinUnionFilters() {
    String filterCond = "index.time - (seed.time - seed.time2 * 4.3) / index.type >= 0.5 and INDEX.weight - 10 < SEED.weight + 100 or SEED.type in (item, user)";
    LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
    List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
    Assert.assertEquals(expOps.size(), 5);
    Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
    Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.LT);
    Assert.assertEquals(expOps.get(2),
        LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("AND")).build());
    Assert.assertEquals(expOps.get(3).getExp().getOp(), CmpOp.IN);
    Assert.assertEquals(expOps.get(4),
        LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("OR")).build());
  }
}