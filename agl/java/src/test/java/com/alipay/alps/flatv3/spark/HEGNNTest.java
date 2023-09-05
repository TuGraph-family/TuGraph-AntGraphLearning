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

public class HEGNNTest {

  private String outputFilePath = null;
  private String edgeFilePath = null;
  private String seedFilePath = null;

  @Before
  public void setUp() {
    String tmpDirPath = System.getProperty("java.io.tmpdir");
    String filePrefix = System.currentTimeMillis() + "_" + new Random().nextInt() + "_";
    File outputFile = new File(tmpDirPath + File.separator + filePrefix + "graph_feature_outputs");
    outputFilePath = outputFile.getAbsolutePath();

    String edgeContent = "node1_id,node2_id,edge_id,type\n" +
        "0,3,0_3,user\n" +
        "1,4,1_4,shop\n" +
        "0,7,0_7,shop\n" +
        "7,2,7_2,item\n" +
        "0,6,0_6,user\n" +
        "3,5,3_5,shop\n" +
        "0,1,0_1,item\n" +
        "1,2,1_2,item\n" +
        "1,3,1_3,user\n";
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
        "0,0,1\n";
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

    HEGNN hegnn = new HEGNN(
        "file:///" + edgeFilePath,
        "file:///" + seedFilePath,
        "file:///" + outputFilePath,
        "{'node_spec':[{'node_name':'default','id_type':'string','features':[]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'type','dtype':'string'}]}",
        2,
        "random_sampler(limit=50, replacement=false)");
    hegnn.setIndexMetas("hash_index:type:string");
    hegnn.setFilterCondition("neighbor.type in (user, item);neighbor.type in (item)");
    hegnn.setHegnnMode(Constants.HEGNN_PATH);
    hegnn.setup();

    Dataset<Row> seedDS = DatasetUtils.inputData(spark, hegnn.getLabelTable());
    Dataset<Row> edgeDS = DatasetUtils.inputData(spark, hegnn.getEdgeTable());
    Dataset<Row> rawNodeFeatureDF = DatasetUtils.inputData(spark, hegnn.getNodeFeatureTable());

    hegnn.runSamplingPipeline(spark, seedDS, edgeDS, rawNodeFeatureDF);

    Map<String, Set<String>> expectedNodes = new HashMap<>();
    expectedNodes.put("0", new HashSet<>(Arrays.asList("0", "1", "2", "3", "6")));
    Map<String, Set<String>> expectedEdges = new HashMap<>();
    expectedEdges.put("0", new HashSet<>(Arrays.asList("0_1", "0_3", "0_6", "1_2")));
    Map<String, List<String>> expectedRoots = new HashMap<>();
    expectedRoots.put("0", Arrays.asList("0"));

    Map<String, Integer> columnIndex = new HashMap<>();
    ArrayList<String[]> content = Utils.readCSVGraphFeatureOutput(outputFilePath, columnIndex);
    String subgraphFeature = content.get(0)[columnIndex.get("graph_feature")];
    GraphFeature graphFeatureMessage = Utils.parseGraphFeature(subgraphFeature, true);
    Nodes nodes = graphFeatureMessage.getNodesOrThrow("default");
    List<String> nodeIds = Utils.getIdStrs(nodes.getNids());
    List<String> rootNodes = Utils.getRootIdStrs(nodeIds, graphFeatureMessage);
    Edges edges = graphFeatureMessage.getEdgesOrThrow("default");
    List<String> edgeIds = Utils.getIdStrs(edges.getEids());
    assertEquals(expectedRoots.get("0"), rootNodes);
    assertEquals(expectedNodes.get("0"), new HashSet<>(nodeIds));
    assertEquals(expectedEdges.get("0"), new HashSet<>(edgeIds));
  }
}
