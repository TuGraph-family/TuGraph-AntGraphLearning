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

package com.alipay.alps.flatv3.spark.utils;

import java.io.Serializable;


public class FeaturesWeight implements Serializable {

  private String featureStr;
  private float weight;

  public FeaturesWeight() {
  }

  public String getFeatureStr() {
    return featureStr;
  }

  public void setFeatureStr(String featureStr) {
    this.featureStr = featureStr;
  }

  public float getWeight() {
    return weight;
  }

  public void setWeight(float weight) {
    this.weight = weight;
  }
}
