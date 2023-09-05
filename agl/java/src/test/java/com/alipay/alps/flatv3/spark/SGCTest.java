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

public class SGCTest {

  private String outputFilePath = null;
  private String nodeFilePath = null;
  private String edgeFilePath = null;
  private String seedFilePath = "";

  @Before
  public void setUp() {
    String tmpDirPath = System.getProperty("java.io.tmpdir");
    String filePrefix = System.currentTimeMillis() + "_" + new Random().nextInt() + "_";
    File outputFile = new File(tmpDirPath + File.separator + filePrefix + "graph_feature_outputs");
    outputFilePath = outputFile.getAbsolutePath();

    String nodeContent = "node_id,node_feature\n" +
        "0,1.0 1.0\n" +
        "1,2.0 2.0\n" +
        "2,4.0 4.0\n" +
        "3,1.0 2.0\n" +
        "4,2.0 2.0\n" +
        "5,3.0 2.0\n" +
        "6,1.0 5.0\n" +
        "7,2.0 2.0\n";
    File nodeFile = new File(tmpDirPath + File.separator + filePrefix + "node_table.csv");
    nodeFilePath = nodeFile.getAbsolutePath();
    try {
      FileWriter writer = new FileWriter(nodeFile);
      writer.write(nodeContent);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    String edgeContent = "node1_id,node2_id,edge_id,weight\n" +
        "0,1,0_1,1\n" +
        "0,3,0_3,2\n" +
        "0,5,3_5,1\n" +
        "1,2,1_2,2\n" +
        "1,3,1_3,3\n" +
        "1,4,1_4,2\n" +
        "2,6,2_6,2\n";
    File edgeFile = new File(tmpDirPath + File.separator + filePrefix + "edge_table.csv");
    edgeFilePath = edgeFile.getAbsolutePath();
    try {
      FileWriter writer = new FileWriter(edgeFile);
      writer.write(edgeContent);
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

    SGC sgc = new SGC(
        "file:///" + edgeFilePath,
        "file:///" + seedFilePath,
        "file:///" + outputFilePath,
        "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'feature','type':'dense','dim':2,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}]}",
        1,
        "random_sampler(limit=50, replacement=false)");

    sgc.setNodeTable("file:///" + nodeFilePath);
    sgc.setup();

    Dataset<Row> nodeDS = DatasetUtils.inputData(spark, sgc.getNodeFeatureTable());
    Dataset<Row> seedDS = null;
    Dataset<Row> edgeDS = DatasetUtils.inputData(spark, sgc.getEdgeTable());
    sgc.runSamplingPipeline(spark, seedDS, edgeDS, nodeDS);

    Set<String> expectedNodes = new HashSet<>(Arrays.asList("0"));
    List<Float> expectedNodeFeatures = Arrays.asList(1.6358246F, 2.0F);

    Map<String, Integer> columnIndex = new HashMap<>();
    ArrayList<String[]> content = Utils.readCSVGraphFeatureOutput(outputFilePath, columnIndex);
    for (int i = 0; i < content.size(); i++) {
      if (content.get(i)[columnIndex.get("node_id")].compareTo("0") != 0) {
        continue;
      }
      String subgraphFeature = content.get(i)[columnIndex.get("subgraph")];
      GraphFeature graphFeatureMessage = Utils.parseGraphFeature(subgraphFeature, true);
      Nodes nodes = graphFeatureMessage.getNodesOrThrow("default");
      List<String> nodeId2Indices = Utils.getIdStrs(nodes.getNids());
      assertEquals(expectedNodes, new HashSet<>(nodeId2Indices));
      List<Float> nodeFeatures = graphFeatureMessage.getNodesMap().get("default").getFeatures()
          .getDfsMap().get("feature").getF32S().getValueList();
      assertEquals(nodeFeatures, expectedNodeFeatures);
    }
  }
}
