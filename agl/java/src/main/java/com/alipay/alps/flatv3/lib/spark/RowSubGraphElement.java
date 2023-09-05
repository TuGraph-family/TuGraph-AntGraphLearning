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

package com.alipay.alps.flatv3.lib.spark;

import com.alipay.alps.flatv3.lib.SubGraphElement;
import com.alipay.alps.flatv3.spark.utils.Constants;
import org.apache.spark.sql.Row;

public class RowSubGraphElement implements SubGraphElement {

  private Row currentInputRow = null;

  public RowSubGraphElement() {
  }

  public RowSubGraphElement(Row row) {
    this.currentInputRow = row;
  }

  public void setCurrentInput(Row row) {
    this.currentInputRow = row;
  }

  @Override
  public String getString(String column) {
    return currentInputRow.getString(Constants.ELEMENT_FIELD_INDEX.get(column));
  }

  @Override
  public int getInt(String column) {
    int i = Constants.ELEMENT_FIELD_INDEX.get(column);
    return currentInputRow.getInt(i);
  }

  @Override
  public float getFloat(String column) {
    return currentInputRow.getFloat(Constants.ELEMENT_FIELD_INDEX.get(column));
  }
}
