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

import com.alipay.alps.flatv3.spark.utils.Constants;
import com.alipay.alps.flatv3.spark.utils.DatasetUtils;
import com.antfin.agl.proto.graph_feature.Edges;
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

public class GraphLevelSamplingTest {

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
        "0,3,0_3,0:1.0 1:2.0,0.7995198\n" +
        "1,2,1_2,0:1.0 1:2.0,0.7634423\n" +
        "2,3,2_3,0:1.0 1:2.0,0.0004714746\n" +
        "7,4,7_4,0:1.0 1:2.0,0.6721331\n" +
        "5,6,5_6,0:1.0 1:2.0,0.834609\n" +
        "4,5,4_5,0:1.0 1:2.0,0.9997682\n";
    File edgeFile = new File(tmpDirPath + File.separator + filePrefix + "edge_table.csv");
    edgeFilePath = edgeFile.getAbsolutePath();
    try {
      FileWriter writer = new FileWriter(edgeFile);
      writer.write(edgeContent);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    String seedContent = "node_id,seed,label\n" +
        "0,g0,0\n" +
        "1,g0,1\n" +
        "2,g0,2\n" +
        "3,g0,3\n" +
        "4,g1,4\n" +
        "5,g1,5\n" +
        "6,g1,6\n" +
        "7,g1,7\n";
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

    GraphLevelSampling graphLevelSampling = new GraphLevelSampling(
        "file:///" + edgeFilePath,
        "file:///" + seedFilePath,
        "file:///" + outputFilePath,
        "{'node_spec':[{'node_name':'default','id_type':'string','features':[]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'feature','type':'sparse_kv','dim':2,'key':'int64','value':'float32'}]}]}",
        1,
        "topk(by=weight, limit=2)");

    graphLevelSampling.setLabelLevel(Constants.LABEL_LEVEL_ON_NODE);
    graphLevelSampling.setup();

    Dataset<Row> seedDS = DatasetUtils.inputData(spark, graphLevelSampling.getLabelTable());
    Dataset<Row> edgeDS = DatasetUtils.inputData(spark, graphLevelSampling.getEdgeTable());
    Dataset<Row> rawNodeFeatureDF = null;
    if (graphLevelSampling.getNodeFeatureTable() != null) {
      rawNodeFeatureDF = DatasetUtils.inputData(spark, graphLevelSampling.getNodeFeatureTable());
    }
    graphLevelSampling.runSamplingPipeline(spark, seedDS, edgeDS, rawNodeFeatureDF);

    Map<String, Set<String>> expectedNodes = new HashMap<>();
    expectedNodes.put("g0", new HashSet<>(Arrays.asList("0", "1", "2", "3")));
    expectedNodes.put("g1", new HashSet<>(Arrays.asList("4", "5", "6", "7")));
    Map<String, Set<String>> expectedEdges = new HashMap<>();
    expectedEdges.put("g0", new HashSet<>(Arrays.asList("0_3", "1_2", "2_3")));
    expectedEdges.put("g1", new HashSet<>(Arrays.asList("4_5", "5_6", "7_4")));
    Map<String, List<String>> expectedRoots = new HashMap<>();
    expectedRoots.put("g0", Arrays.asList("0", "1", "2", "3"));
    expectedRoots.put("g1", Arrays.asList("4", "5", "6", "7"));
    Map<String, String> expectedLabels = new HashMap<>();
    expectedLabels.put("g0", "0\t1\t2\t3");
    expectedLabels.put("g1", "4\t5\t6\t7");

    Map<String, Integer> columnIndex = new HashMap<>();
    ArrayList<String[]> content = Utils.readCSVGraphFeatureOutput(outputFilePath, columnIndex);
    for (int i = 0; i < content.size(); i++) {
      String subgraphFeature = content.get(i)[columnIndex.get("graph_feature")];
      GraphFeature graphFeatureMessage = Utils.parseGraphFeature(subgraphFeature, true);
      Nodes nodes = graphFeatureMessage.getNodesOrThrow("default");
      List<String> nodeIds = Utils.getIdStrs(nodes.getNids());
      List<String> rootNodes = Utils.getRootIdStrs(nodeIds, graphFeatureMessage);
      Edges edges = graphFeatureMessage.getEdgesOrThrow("default");
      List<String> edgeIds = Utils.getIdStrs(edges.getEids());
      String id = content.get(i)[columnIndex.get("seed")];
      assertEquals(expectedRoots.get(id), rootNodes);
      assertEquals(expectedNodes.get(id), new HashSet<>(nodeIds));
      assertEquals(expectedEdges.get(id), new HashSet<>(edgeIds));
      assertEquals(expectedLabels.get(id), content.get(i)[columnIndex.get("label_list")]);
      assertEquals(expectedLabels.get(id), content.get(i)[columnIndex.get("node_id_list")]);
    }
  }
}
