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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RangeUnitTest {

  @Test
  public void testContains() {
    RangeUnit range = new RangeUnit(5, 10);
    assertTrue(range.contains(5));
    assertTrue(range.contains(10));
    assertTrue(range.contains(7));
    assertFalse(range.contains(4));
    assertFalse(range.contains(11));
  }

  @Test
  public void testGetLow() {
    RangeUnit range = new RangeUnit(5, 10);
    assertEquals(5, range.getLow());
  }

  @Test
  public void testGetHigh() {
    RangeUnit range = new RangeUnit(5, 10);
    assertEquals(10, range.getHigh());
  }

  @Test
  public void testSetLow() {
    RangeUnit range = new RangeUnit(5, 10);
    range.setLow(7);
    assertEquals(7, range.getLow());
  }

  @Test
  public void testSetHigh() {
    RangeUnit range = new RangeUnit(5, 10);
    range.setHigh(12);
    assertEquals(12, range.getHigh());
  }

  @Test
  public void testJoin() {
    RangeUnit range1 = new RangeUnit(5, 10);
    RangeUnit range2 = new RangeUnit(7, 12);
    range1.join(range2);
    assertEquals(7, range1.getLow());
    assertEquals(10, range1.getHigh());
  }

  @Test
  public void testGetSize() {
    RangeUnit range = new RangeUnit(5, 10);
    assertEquals(6, range.getSize());
  }
}