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

package com.alipay.alps.flatv3.neighbor_selection;

import com.alipay.alps.flatv3.index.HeteroDataset;
import com.antfin.agl.proto.sampler.Element;
import java.util.List;
import java.util.Map;

// SampleOutput contains two parts: fixed part and variable part.
// The fixed part for nodes contains the following fields:
//   seed, 'node', node2_id
//   (it means node2_id is a newly expanded neighbor of seed)
// The fixed part for edges contains the following fields:
//   seed, 'edge', node1_id, node2_id, edge_id
//   (it means node2_id is expanded using this edge from node1_id to node2_id)
// otherOutputSchema is node:source.$i:source.$k
// the variable part of nodes contains the following fields: source.$i, source.$k.
// and the variable part of edges contains the following fields: source.$j.
// source can take any value from [index|seeds].
// i,j,k can be any integer or string.
// For example, if otherOutputSchema is node:index.timestamp.long:source.weight.float
// the variable part of nodes contains: timestamp of node2_id and weight of related seed.
public class OtherOutput {

  private static final String OUTPUT_NODE = "node";
  private static final String OUTPUT_EDGE = "edge";
  private static final String SOURCE_INDEX = "index";
  private static final String SOURCE_SEEDS = "seed";
  private static final int SOURCE_INDEX_ID = 0;
  private static final int SOURCE_SEEDS_ID = 1;
  private int[] otherNodeOutputSource;
  private String[] otherNodeOutputType;
  private String[] otherNodeOutputField;
  private List[] otherNodeOutputList;
  private HeteroDataset neighborDataset;
  private Map<String, Element.Number> seedVariableMap;

  public int getNodeOtherOutputLen() {
    return otherNodeOutputList == null ? 0 : otherNodeOutputList.length;
  }

  public String getNodeOutputField(int outputIdx) {
    return otherNodeOutputField[outputIdx];
  }

  public String getNodeOutputType(int outputIdx) {
    return otherNodeOutputType[outputIdx];
  }

  public <T> T getNodeOtherOutput(int outputIdx, int neighborIdx) {
    if (otherNodeOutputSource[outputIdx] == SOURCE_INDEX_ID) {
      return (T) otherNodeOutputList[outputIdx].get(neighborIdx);
    }
    Element.Number number = seedVariableMap.get(otherNodeOutputField[outputIdx]);
    if (otherNodeOutputType[outputIdx].compareToIgnoreCase("long") == 0) {
      return (T) Long.valueOf(number.getI());
    } else if (otherNodeOutputType[outputIdx].compareToIgnoreCase("float") == 0) {
      return (T) Float.valueOf(number.getF());
    } else {
      return (T) number.getS();
    }
  }

  private OtherOutput(Builder builder) {
    this.neighborDataset = builder.neighborDataset;
    this.seedVariableMap = builder.seedVariableMap;
    this.otherNodeOutputSource = builder.otherNodeOutputSource;
    this.otherNodeOutputType = builder.otherNodeOutputType;
    this.otherNodeOutputField = builder.otherNodeOutputField;
    this.otherNodeOutputList = builder.otherNodeOutputList;
  }

  public static class Builder {

    private String otherOutputSchema;
    private HeteroDataset neighborDataset;
    private Map<String, Element.Number> seedVariableMap;

    private int[] otherNodeOutputSource;
    private String[] otherNodeOutputType;
    private String[] otherNodeOutputField;
    private List[] otherNodeOutputList;

    public Builder otherOutputSchema(String otherOutputSchema) {
      this.otherOutputSchema = otherOutputSchema;
      return this;
    }

    public Builder neighborDataset(HeteroDataset neighborDataset) {
      this.neighborDataset = neighborDataset;
      return this;
    }

    public Builder seedAttrs(Map<String, Element.Number> seedVariableMap) {
      this.seedVariableMap = seedVariableMap;
      return this;
    }

    public OtherOutput build() {
      // split otherOutputSchema and remove empty strings
      String[] parts = otherOutputSchema.split(",");
      for (String part : parts) {
        String arrs[] = part.split(":");
        if (arrs.length <= 1) {
          continue;
        }
        switch (arrs[0]) {
          case OUTPUT_NODE:
            otherNodeOutputList = new List[arrs.length - 1];
            otherNodeOutputSource = new int[arrs.length - 1];
            otherNodeOutputType = new String[arrs.length - 1];
            otherNodeOutputField = new String[arrs.length - 1];
            parseOutputSchema(otherNodeOutputList, otherNodeOutputSource, otherNodeOutputType,
                otherNodeOutputField, part.split(":"));
            break;
          default:
            throw new RuntimeException("schema is wrong :" + otherOutputSchema
                + ", we only support output other node attr.");
        }
      }
      OtherOutput otherOutput = new OtherOutput(this);
      return otherOutput;
    }

    private void parseOutputSchema(List[] otherOutputList, int[] otherOutputSource,
        String[] otherNodeOutputType, String[] otherNodeOutputField, String[] t) {
      for (int j = 1; j < t.length; j++) {
        String[] ts = t[j].split("\\.");
        assert ts.length == 3;
        switch (ts[0]) {
          case SOURCE_INDEX:
            if (neighborDataset != null) {
              if (neighborDataset.getAttributeList(ts[1]) == null) {
                throw new RuntimeException(
                    "index output schema is wrong: " + (neighborDataset == null
                        ? "neighborDataset is null"
                        : "neighborDataset :" + neighborDataset + " missing key:" + ts[1]));
              }
              otherOutputList[j - 1] = (List) neighborDataset.getAttributeList(ts[1]);
            }
            otherOutputSource[j - 1] = SOURCE_INDEX_ID;
            otherNodeOutputField[j - 1] = ts[1];
            otherNodeOutputType[j - 1] = ts[2];
            break;
          case SOURCE_SEEDS:
            otherOutputSource[j - 1] = SOURCE_SEEDS_ID;
            otherNodeOutputField[j - 1] = ts[1];
            otherNodeOutputType[j - 1] = ts[2];
            break;
          default:
            throw new RuntimeException("schema is wrong");
        }
      }
    }
  }
}
