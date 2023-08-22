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

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/*
 * RangeUnit is a class that represents a range of integer values. It has the following properties:
 * 1. It has a low and a high integer value. The low and high value are inclusive.
 * 2. It has a size, which is the number of integers it contains.
 * 3. It can be used to test if a given integer is contained in the range.
 * 4. It can join another range, it will contain all the integers that are in both of the two ranges.
 *
 * The following are examples of valid ranges:
 * 1. [0, 0] is a valid range that contains only the integer 0.
 * 2. [0, 2] is a valid range that contains the integers 0, 1, and 2.
 *
 * The following are examples of invalid ranges:
 * 1. [0, -1] is invalid because the low value is greater than the high value.
 * 2. [-1, -1] is invalid because the low value and high value are less than 0.
 * 3. [1, 0] is invalid because the low value is greater than the high value.
 *
 * [2, 9] join [6, 10], we can get [6, 9]
 */
public class RangeUnit implements Serializable {

  @Getter
  @Setter
  private int low;
  @Getter
  @Setter
  private int high;

  public void setLow(int low) {
    this.low = low;
  }

  public void setHigh(int high) {
    this.high = high;
  }

  public int getLow() {
    return low;
  }

  public int getHigh() {
    return high;
  }

  public RangeUnit() {
    this.low = -1;
    this.high = -1;
  }

  public RangeUnit(RangeUnit r) {
    this.low = r.low;
    this.high = r.low;
    if (this.low > this.high) {
      this.low = -1;
      this.high = -1;
    }
  }

  public RangeUnit(int low, int high) {
    this.low = low;
    this.high = high;
  }

  public boolean contains(int number) {
    return (number >= low && number <= high);
  }

  public void join(RangeUnit range) {
    this.low = Math.max(this.low, range.low);
    this.high = Math.min(this.high, range.high);
    if (this.low > this.high) {
      this.low = -1;
      this.high = -1;
    }
  }

  public int getSize() {
    if (this.low == -1 && this.high == -1) {
      return 0;
    }
    return high - low + 1;
  }

  @Override
  public String toString() {
    return "Range{" +
        "low=" + low +
        ", high=" + high +
        '}';
  }
}
