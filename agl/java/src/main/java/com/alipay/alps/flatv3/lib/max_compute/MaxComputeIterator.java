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

import com.alipay.alps.flatv3.lib.SubGraphElement;
import com.alipay.alps.flatv3.lib.SubGraphElementIterator;
import java.util.List;

public class MaxComputeIterator implements SubGraphElementIterator {

  private int currentPosition = 0;
  private List<Object[]> values = null;
  private ArraySubGraphElement currentVal = null;

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public SubGraphElement getNext() {
    currentVal.setCurrentInput(values.get(currentPosition++));
    return currentVal;
  }

  @Override
  public void reset() {

  }
}
