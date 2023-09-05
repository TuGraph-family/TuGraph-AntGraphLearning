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

package com.alipay.alps.flatv3.spark;

import static org.junit.Assert.assertEquals;

import com.alipay.alps.flatv3.spark.utils.DatasetUtils;
import com.antfin.agl.proto.graph_feature.Edges;
import com.antfin.agl.proto.graph_feature.Features.SparseKVFeatures;
import com.antfin.agl.proto.graph_feature.GraphFeature;
import com.antfin.agl.proto.graph_feature.Nodes;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.Before;
import org.junit.Test;

public class LinkLevelSamplingMergeTest {

  private String outputFilePath = null;
  private String edgeFilePath = null;
  private String seedFilePath = null;

  @Before
  public void setUp() {
    String tmpDirPath = System.getProperty("java.io.tmpdir");
    String filePrefix = System.currentTimeMillis() + "_" + new Random().nextInt() + "_";
    File outputFile = new File(tmpDirPath + File.separator + filePrefix + "graph_feature_outputs");
    outputFilePath = outputFile.getAbsolutePath();

    String edgeContent = "node1_id,node2_id,edge_id,edge_feature,weight\n" +
        "0,3,0_3,0:3.0,0.7995198\n" +
        "1,4,1_4,1:4.0,0.7634423\n" +
        "0,7,0_7,0:7.0,0.0004714746\n" +
        "7,2,7_2,7:2.0,0.6721331\n" +
        "0,6,0_6,0:6.0,0.834609\n" +
        "3,5,3_5,3:5.0,0.9997682\n" +
        "0,1,0_1,0:1.0,0.1069209\n" +
        "1,2,1_2,1:2.0,0.6345752\n" +
        "1,3,1_3,1:3.0,0.427855\n";
    File edgeFile = new File(tmpDirPath + File.separator + filePrefix + "edge_table.csv");
    edgeFilePath = edgeFile.getAbsolutePath();
    try {
      FileWriter writer = new FileWriter(edgeFile);
      writer.write(edgeContent);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    String seedContent = "node1_id,node2_id,seed,label\n" +
        "0,1,0_1,1\n";
    File seedFile = new File(tmpDirPath + File.separator + filePrefix + "seed_table.csv");
    seedFilePath = seedFile.getAbsolutePath();
    try {
      FileWriter writer = new FileWriter(seedFile);
      writer.write(seedContent);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void sampleSmallGraph() throws Exception {
    SparkSession spark = SparkSession
        .builder()
        .appName("SparkSQL_Demo")
        .config("spark.master", "local")
        .getOrCreate();

    LinkLevelSamplingMerge LinkLevelSamplingMerge = new LinkLevelSamplingMerge(
        "file:///" + edgeFilePath,
        "file:///" + seedFilePath,
        "file:///" + outputFilePath,
        "{'node_spec':[{'node_name':'default','id_type':'string','features':[]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'feature','type':'sparse_kv','dim':20,'key':'int64','value':'float32'}]}],'edge_attr':[{'field':'weight','dtype':'float'}]}",
        2,
        "topk(by=weight, limit=2)");

    LinkLevelSamplingMerge.setup();
    LinkLevelSamplingMerge.setRemoveEdgeAmongRoots(true);

    Dataset<Row> seedDS = DatasetUtils.inputData(spark, LinkLevelSamplingMerge.getLabelTable());
    Dataset<Row> edgeDS = DatasetUtils.inputData(spark, LinkLevelSamplingMerge.getEdgeTable());
    Dataset<Row> rawNodeFeatureDF = null;
    if (LinkLevelSamplingMerge.getNodeFeatureTable() != null) {
      rawNodeFeatureDF = DatasetUtils
          .inputData(spark, LinkLevelSamplingMerge.getNodeFeatureTable());
    }
    LinkLevelSamplingMerge.runSamplingPipeline(spark, seedDS, edgeDS, rawNodeFeatureDF);

    Map<String, Set<String>> expectedNodes = new HashMap<>();
    expectedNodes.put("0_1", new HashSet<>(Arrays.asList("0", "1", "2", "3", "5", "7")));
    Map<String, Set<String>> expectedEdges = new HashMap<>();
    expectedEdges.put("0_1", new HashSet<>(Arrays.asList("1_2", "1_3", "3_5", "0_7", "7_2")));
    Map<String, List<String>> expectedRoots = new HashMap<>();
    expectedRoots.put("0_1", Arrays.asList("0", "1"));

    Map<String, Integer> columnIndex = new HashMap<>();
    ArrayList<String[]> content = Utils.readCSVGraphFeatureOutput(outputFilePath, columnIndex);
    String subgraphFeature = content.get(0)[columnIndex.get("graph_feature")];
    GraphFeature graphFeatureMessage = Utils.parseGraphFeature(subgraphFeature, true);
    Nodes nodes = graphFeatureMessage.getNodesOrThrow("default");
    List<String> nodeIds = Utils.getIdStrs(nodes.getNids());
    List<String> rootNodes = Utils.getRootIdStrs(nodeIds, graphFeatureMessage);
    Edges edges = graphFeatureMessage.getEdgesOrThrow("default");
    List<String> edgeIds = Utils.getIdStrs(edges.getEids());
    List<Long> expectedEdgeFeatureLen = new ArrayList<>();
    List<Long> expectedEdgeFeatureKey = new ArrayList<>();
    List<Float> expectedEdgeFeatureValue = new ArrayList<>();
    for (int i = 0; i < edgeIds.size(); i++) {
      String edge = edgeIds.get(i);
      String arrs[] = edge.split("_");
      expectedEdgeFeatureLen.add(Long.valueOf(i) + 1L);
      expectedEdgeFeatureKey.add(Long.valueOf(arrs[0]));
      expectedEdgeFeatureValue.add(Float.valueOf(arrs[1]));
    }
    SparseKVFeatures edgeFeature = graphFeatureMessage.getEdgesMap().get("default").getFeatures()
        .getSpKvsMap().get("feature");
    assertEquals(expectedEdgeFeatureLen, edgeFeature.getLens().getValueList());
    assertEquals(expectedEdgeFeatureKey, edgeFeature.getKeys().getValueList());
    assertEquals(expectedEdgeFeatureValue, edgeFeature.getF32S().getValueList());
    assertEquals(expectedRoots.get("0_1"), rootNodes);
    assertEquals(expectedNodes.get("0_1"), new HashSet<>(nodeIds));
    assertEquals(expectedEdges.get("0_1"), new HashSet<>(edgeIds));
  }
}
