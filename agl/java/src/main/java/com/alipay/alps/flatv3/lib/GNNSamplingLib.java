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

package com.alipay.alps.flatv3.lib;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.graphfeature.GraphFeatureGenerator;
import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.neighbor_selection.NeighborSelector;
import com.alipay.alps.flatv3.neighbor_selection.OtherOutput;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.spark.utils.Constants;
import com.antfin.agl.proto.graph_feature.DType;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import scala.Tuple4;

public class GNNSamplingLib implements Serializable {

  private String filterCondition;
  private String labelLevel;
  @Getter
  private SubGraphSpecs subGraphSpec;
  @Getter
  private int maxHop;
  @Getter
  private List<Filter> filters;
  private SampleCondition[] sampleConditions;
  private List<String> indexMetas;
  @Getter
  private List<String> otherOutputSchemaList;

  private boolean storeIDorNot = true;
  private String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss")
      .format(new java.util.Date());

  public GNNSamplingLib(String subGraphSpec, String sampleCondition, int maxHop) throws Exception {
    this.subGraphSpec = new SubGraphSpecs(subGraphSpec);

    sampleConditions = new SampleCondition[maxHop];
    String sampleCondStr[] = sampleCondition.split(";");
    for (int i = 0; i < maxHop; i++) {
      sampleConditions[i] = new SampleCondition(
          i < sampleCondStr.length ? sampleCondStr[i] : sampleCondStr[sampleCondStr.length - 1]);
    }
    this.maxHop = maxHop;
  }

  public void setup() throws Exception {
    parseFilterAndOtherOutputSchema();
  }

  public void setIndexMetas(String indexMetas) {
    if (indexMetas.trim().isEmpty()) {
      return;
    }
    this.indexMetas = Arrays.asList(indexMetas.split(","));
  }

  public void setFilterCondition(String filterCondition) {
    this.filterCondition = filterCondition;
  }

  public void notStoreID() {
    this.storeIDorNot = false;
  }

  public void setLabelLevel(String labelLevel) {
    this.labelLevel = labelLevel;
  }

  public Filter getFilter(int hop) {
    if (filters == null || filters.isEmpty()) {
      return null;
    }
    return filters.get(hop);
  }

  public List<Filter> getFilters() {
    return filters;
  }

  public SampleCondition getSampleCondition(int hop) {
    return sampleConditions[hop];
  }

  public SampleCondition[] getSampleConditions() {
    return sampleConditions;
  }

  public List<String> getIndexMetas() {
    return indexMetas;
  }

  public String getOtherOutputSchema(int hop) {
    if (otherOutputSchemaList == null || otherOutputSchemaList.isEmpty()) {
      return "";
    }
    return otherOutputSchemaList.get(hop % (maxHop - 1));
  }

  public List<String> getOtherOutputSchemaList() {
    return otherOutputSchemaList;
  }

  public String getFilterCondition() {
    return filterCondition;
  }

  public boolean isStoreIDorNot() {
    return storeIDorNot;
  }

  // 注释描述清晰
  public void parseFilterAndOtherOutputSchema() throws Exception {
    if (filterCondition != null && !filterCondition.trim().isEmpty()) {
      List<String> filterCondList = new ArrayList<>(Arrays.asList(filterCondition.split(";")));
      while (filterCondList.size() < maxHop) {
        filterCondList.add(filterCondList.get(0));
      }
      filters = new ArrayList<>();
      otherOutputSchemaList = new ArrayList<>();
      for (int i = 0; i < filterCondList.size(); i++) {
        String filterCond = filterCondList.get(i);
        filterCond = filterCond.replaceAll("neighbor_" + (i + 1) + ".", "index."); // current hop
        filterCond = filterCond.replaceAll("neighbor.", "index.");
        Set<String> fieldFromLastHop = new HashSet<>();
        String lastHop = "neighbor_" + i + ".";  // last hop
        int pos = -1;
        while ((pos = filterCond.indexOf(lastHop)) != -1) {
          String fieldName = "";
          pos += lastHop.length();
          while (!Character.isSpaceChar(filterCond.charAt(pos))) {
            fieldName += filterCond.charAt(pos++);
          }
          fieldFromLastHop.add(fieldName);
          filterCond = filterCond.replaceFirst("neighbor_" + i + ".", "seed.");
        }
        if (filterCond.contains("neighbor")) {
          throw new RuntimeException("filter condition error:" + filterCondList.get(i));
        }
        Filter filter = new Filter(filterCond);
        filters.add(filter);
        if (i > 0) {
          String otherOutputSchema = "";
          Map<VariableSource, Set<String>> referColumns = filter.getReferSourceAndColumns();
          if (referColumns.containsKey(VariableSource.SEED)) {
            for (String column : referColumns.get(VariableSource.SEED)) {
              if (fieldFromLastHop.contains(column)) {
                otherOutputSchema +=
                    ":index." + column + "." + getDtypeStr(subGraphSpec.getEdgeAttrs().get(column));
              } else {
                otherOutputSchema +=
                    ":seed." + column + "." + getDtypeStr(subGraphSpec.getSeedAttrs().get(column));
              }
            }
          }
          otherOutputSchemaList.add("node" + otherOutputSchema);
        }
      }
    }
  }

  private String getDtypeStr(DType type) {
    switch (type) {
      case INT8:
      case UINT16:
      case INT16:
      case UINT32:
      case INT32:
      case UINT64:
      case INT64:
        return "long";
      case FLOAT:
      case DOUBLE:
        return "float";
      case STR:
        return "string";
      default:
        return "string";
    }
  }

  public byte[] buildIndex(String indexMeta, List valueList) {
    HeteroDataset heteroDataset = new HeteroDataset(valueList.size());
    String indexColumn = indexMeta.split(":")[1];
    heteroDataset.addAttributeList(indexColumn, valueList);
    BaseIndex indexMap = new IndexFactory().createIndex(indexMeta, heteroDataset);
    return indexMap.dump();
  }

  public void processRootNodeOrder(String nodeId, String seed, int order,
      WriteSubGraphElement writer) {
    OtherOutput otherOutput = new OtherOutput.Builder()
        .otherOutputSchema(getOtherOutputSchema(0))
        .build();
    int otherOutputLen = otherOutput.getNodeOtherOutputLen();
    if (otherOutputLen == 0) {
      writer.write(seed, Constants.ROOT_INT, nodeId, String.format("%010d", order), nodeId, null,
          null);
    } else if (otherOutputLen == 1) {
      writer.write(seed, Constants.ROOT_INT, nodeId, String.format("%010d", order), nodeId, null,
          null, null);
    } else {
      writer.write(seed, Constants.ROOT_INT, nodeId, String.format("%010d", order), nodeId, null,
          null, null, null);
    }
  }

  public void processRootNode(String nodeIds, String seed, WriteSubGraphElement writer) {
    OtherOutput otherOutput = new OtherOutput.Builder()
        .otherOutputSchema(getOtherOutputSchema(0))
        .build();
    String nodeIdArray[] = nodeIds.split(" ");
    int otherOutputLen = otherOutput.getNodeOtherOutputLen();
    for (int i = 0; i < nodeIdArray.length; i++) {
      String nodeId = nodeIdArray[i];
      if (otherOutputLen == 0) {
        writer
            .write(seed, Constants.ROOT_INT, nodeId, String.format("%010d", i), nodeId, null, null);
      } else if (otherOutputLen == 1) {
        writer
            .write(seed, Constants.ROOT_INT, nodeId, String.format("%010d", i), nodeId, null, null,
                null);
      } else {
        writer
            .write(seed, Constants.ROOT_INT, nodeId, String.format("%010d", i), nodeId, null, null,
                null, null);
      }
    }
  }

  public void chooseNeighbor(String node1ID, List<String> node2IDs, List<String> edgeIDs,
      Map<String, BaseIndex> indexesMap,
      HeteroDataset neighborDataset, String seed, Map<String, Element.Number> seedVariableMap,
      int hop, WriteSubGraphElement writer) throws Exception {
    Filter filter = getFilter(hop);
    SampleCondition sampleCond = new SampleCondition(getSampleConditions()[hop]);
    if (sampleCond.getRandomSeed() == 0) {
      sampleCond.setRandomSeed((seed + hop + timeStamp).hashCode());
    }
    NeighborSelector neighborSelector = new NeighborSelector(filter, sampleCond);
    neighborSelector.initSampler(neighborDataset);
    List<Integer> sampledNeighborIndex = neighborSelector
        .chooseNeighbor(indexesMap, neighborDataset, seedVariableMap);
    OtherOutput otherOutput = new OtherOutput.Builder()
        .otherOutputSchema(getOtherOutputSchema(hop))
        .neighborDataset(neighborDataset)
        .seedAttrs(seedVariableMap)
        .build();
    int nodeOutputWidth = otherOutput.getNodeOtherOutputLen();
    for (int i = 0; i < sampledNeighborIndex.size(); i++) {
      int neighborIdx = sampledNeighborIndex.get(i);
      String node2ID = node2IDs.get(neighborIdx);
      String edgeID = edgeIDs.get(neighborIdx);
      if (nodeOutputWidth == 0) {
        writer.write(seed, Constants.NODE_INT, null, null, node2ID, null, null);
        writer.write(seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, null);
      } else if (nodeOutputWidth == 1) {
        writer.write(seed, Constants.NODE_INT, null, null, node2ID, null, null,
            otherOutput.getNodeOtherOutput(0, neighborIdx));
        writer.write(seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, null, null);
      } else if (nodeOutputWidth == 2) {
        writer.write(seed, Constants.NODE_INT, null, null, node2ID, null, null,
            otherOutput.getNodeOtherOutput(0, neighborIdx),
            otherOutput.getNodeOtherOutput(1, neighborIdx));
        writer.write(seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, null, null, null);
      }
    }
  }

  public String generateGraphFeature(SubGraphElementIterator subGraphElementIterator,
      boolean removeEdgeAmongRoots) throws Exception {
    GraphFeatureGenerator graphFeatureGenerator = new GraphFeatureGenerator(subGraphSpec);
    graphFeatureGenerator.init();
    List<ImmutablePair<Integer, String>> rootIds = new ArrayList<>();
    List<Tuple4<String, String, String, String>> edgeFeatures = new ArrayList<>();
    while (subGraphElementIterator.hasNext()) {
      SubGraphElement entry = subGraphElementIterator.getNext();
      if (entry.getInt(Constants.ENTRY_TYPE) == Constants.ROOT_INT) {
        rootIds.add(new ImmutablePair<>(Integer.valueOf(entry.getString(Constants.ENTRY_NODE2)),
            entry.getString(Constants.ENTRY_ID)));
        graphFeatureGenerator.addNodeInfo(entry.getString(Constants.ENTRY_ID), "default",
            entry.getString(Constants.ENTRY_FEATURE) == null ? ""
                : entry.getString(Constants.ENTRY_FEATURE));
      } else if (entry.getInt(Constants.ENTRY_TYPE) == Constants.NODE_INT) {
        graphFeatureGenerator.addNodeInfo(entry.getString(Constants.ENTRY_ID), "default",
            entry.getString(Constants.ENTRY_FEATURE) == null ? ""
                : entry.getString(Constants.ENTRY_FEATURE));
      } else if (entry.getInt(Constants.ENTRY_TYPE) == Constants.EDGE_INT) {
        edgeFeatures.add(new Tuple4<>(entry.getString(Constants.ENTRY_NODE1),
            entry.getString(Constants.ENTRY_NODE2), entry.getString(Constants.ENTRY_ID),
            entry.getString(Constants.ENTRY_FEATURE)));
      } else {
        throw new Exception("invalid entry type: " + entry.getInt(Constants.ENTRY_TYPE));
      }
    }
    try {
      Set<String> rootIdSet = new HashSet<>();
      if (removeEdgeAmongRoots) {
        for (ImmutablePair<Integer, String> p : rootIds) {
          rootIdSet.add(p.getValue());
        }
      }
      for (Tuple4<String, String, String, String> edgeFeature : edgeFeatures) {
        if (removeEdgeAmongRoots && rootIdSet.contains(edgeFeature._1()) && rootIdSet
            .contains(edgeFeature._2()) && edgeFeature._1().compareTo(edgeFeature._2()) != 0) {
          continue;
        }
        graphFeatureGenerator
            .addEdgeInfo(edgeFeature._1(), edgeFeature._2(), edgeFeature._3(), "default",
                edgeFeature._4() == null ? "" : edgeFeature._4());
      }
    } catch (Exception e) {
      throw new RuntimeException("========failed for nodeIndices:" + Arrays
          .toString(graphFeatureGenerator.nodeIndices.entrySet().toArray())
          + "\nedgeIndices:" + Arrays.toString(edgeFeatures.toArray())
          + "\nrootIds:" + Arrays.toString(rootIds.toArray()), e);
    }
    Collections.sort(rootIds, new Comparator<ImmutablePair<Integer, String>>() {
      @Override
      public int compare(ImmutablePair<Integer, String> o1, ImmutablePair<Integer, String> o2) {
        if (o1.getKey() == o2.getKey()) {
          return o1.getValue().compareTo(o2.getValue());
        }
        return o1.getKey() - o2.getKey();
      }
    });

    List<String> sortedRootId = new ArrayList<>();
    for (int i = 0; i < rootIds.size(); i++) {
      sortedRootId.add(rootIds.get(i).getValue());
    }
    return graphFeatureGenerator.getGraphFeature(sortedRootId, true, storeIDorNot);
  }
}
