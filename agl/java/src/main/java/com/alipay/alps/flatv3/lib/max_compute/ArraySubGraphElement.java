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

public class ArraySubGraphElement implements SubGraphElement {

  private Object[] currentData = null;

  public ArraySubGraphElement(Object[] data) {
    this.currentData = data;
  }

  public void setCurrentInput(Object[] data) {
    this.currentData = data;
  }

  @Override
  public String getString(String column) {
    return null;
  }

  @Override
  public int getInt(String column) {
    return 0;
  }

  @Override
  public float getFloat(String column) {
    return 0;
  }
}
