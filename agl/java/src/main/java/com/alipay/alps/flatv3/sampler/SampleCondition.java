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

package com.alipay.alps.flatv3.sampler;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * This class represents a condition for sampling data, which is constructed based on a string representation.
 * It contains method name, key to sample by, sample limit, replacement flag, and reverse flag.
 */
public class SampleCondition implements Serializable {

  private final String EQ = "=";
  private final String BY = "by";
  private final String LIMIT = "limit";
  private final String REPLACE = "replacement";
  private final String REVERSE = "reverse";
  private final String SEED = "seed";
  private String method;
  private String key;
  private int limit;
  private boolean replacement = false;
  private boolean reverse = false;
  private int randomSeed = 0;

  public String getMethod() {
    return method;
  }

  public String getKey() {
    return key;
  }

  public int getLimit() {
    return limit;
  }

  public boolean isReplacement() {
    return replacement;
  }

  public boolean isReverse() {
    return reverse;
  }

  public void setRandomSeed(int randomSeed) {
    this.randomSeed = randomSeed;
  }

  public int getRandomSeed() {
    return randomSeed;
  }

  public SampleCondition(SampleCondition a) {
    this.method = a.method;
    this.key = a.key;
    this.limit = a.limit;
    this.replacement = a.replacement;
    this.reverse = a.reverse;
    this.randomSeed = a.randomSeed;
  }

  /**
   * Constructs a SampleCondition object.
   *
   * @param method      The name of the sampling method.
   * @param key         The key to sample by.
   * @param limit       The sample limit.
   * @param replacement Whether or not replacement is allowed.
   * @param reverse     Whether or not to reverse the sample.
   */
  public SampleCondition(String method, String key, int limit, boolean replacement, boolean reverse,
      int randomSeed) {
    this.method = method;
    this.key = key;
    this.limit = limit;
    this.replacement = replacement;
    this.reverse = reverse;
    this.randomSeed = randomSeed;
  }

  /**
   * Constructs a SampleCondition object from a string representation.
   *
   * @param sampleMeta The string representation of the SampleCondition object.
   */
  public SampleCondition(String sampleMeta) {
    // Remove all whitespace characters from the string representation.
    sampleMeta = sampleMeta.replaceAll("\\s", "");
    // Tokenize the string representation.
    String delimitor = "(),";
    StringTokenizer x = new StringTokenizer(sampleMeta, delimitor, true);

    // Parse the string representation and set the fields of the SampleCondition object.
    while (x.hasMoreTokens()) {
      String p = x.nextToken();
      if (!delimitor.contains(p)) {
        int sepPos = p.indexOf(EQ);
        if (sepPos == -1) {
          this.method = p;
        } else {
          String a1 = p.substring(0, sepPos);
          String a2 = p.substring(sepPos + 1);
          switch (a1) {
            case BY:
              this.key = a2;
              break;
            case LIMIT:
              this.limit = Integer.valueOf(a2);
              break;
            case REPLACE:
              this.replacement = Boolean.valueOf(a2);
              break;
            case REVERSE:
              this.reverse = Boolean.valueOf(a2);
              break;
            case SEED:
              this.randomSeed = Integer.valueOf(a2);
            default:
              break;
          }
        }
      }
    }
  }

  /**
   * Returns a string representation of the SampleCondition object.
   *
   * @return A string representation of the SampleCondition object.
   */
  @Override
  public String toString() {
    return "SampleCondition{" +
        "method='" + method + '\'' +
        ", key='" + key + '\'' +
        ", limit=" + limit +
        ", replacement=" + replacement +
        ", reverse=" + reverse +
        ", randomSeed=" + randomSeed +
        '}';
  }
}
