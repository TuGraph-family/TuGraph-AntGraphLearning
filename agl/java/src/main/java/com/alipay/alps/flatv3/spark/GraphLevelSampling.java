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

import com.alipay.alps.flatv3.spark.utils.Constants;
import com.alipay.alps.flatv3.spark.utils.DatasetUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphLevelSampling extends NodeLevelSampling {

  private static final Logger LOG = LoggerFactory.getLogger(GraphLevelSampling.class);

  private String labelLevel = Constants.LABEL_LEVEL_ON_GRAPH;

  public GraphLevelSampling(String edgeTable, String labelTable, String outputTablePrefix,
      String subGraphSpec, int maxHop, String sampleCondition) throws Exception {
    super(edgeTable, labelTable, outputTablePrefix, subGraphSpec, maxHop, sampleCondition);
  }

  public void setLabelLevel(String labelLevel) {
    this.labelLevel = labelLevel;
  }

  public void sinkSubgraphWithLabel(SparkSession spark, Dataset<Row> subgraphWithLabel) {
    if (labelLevel.compareToIgnoreCase(Constants.LABEL_LEVEL_ON_NODE) == 0) {
      DatasetUtils.outputData(spark, subgraphWithLabel, getOutputTablePrefix());
    } else {
      super.sinkSubgraphWithLabel(spark, subgraphWithLabel);
    }
  }

  @Override
  public void runSamplingPipeline(SparkSession spark, Dataset<Row> seedDS, Dataset<Row> edgeDS,
      Dataset<Row> rawNodeFeatureDF) {
    if (labelLevel.compareToIgnoreCase(Constants.LABEL_LEVEL_ON_GRAPH) == 0) {
      seedDS = seedDS.flatMap((FlatMapFunction<Row, Row>) row -> {
        String nodeList = row.getString(0);
        String graphID = row.getString(1);
        List<Row> ans = new ArrayList<>();
        for (String nodeID : nodeList.split(" ")) {
          ans.add(RowFactory.create(graphID, nodeID));
        }
        return ans.iterator();
      }, RowEncoder.apply(new StructType()
          .add("seed", DataTypes.StringType)
          .add("node_id", DataTypes.StringType)));
    }
    seedDS = attrsCastDType(seedDS, getSubGraphSpec().getSeedAttrs());
    seedDS.show();
    seedDS.printSchema();

    edgeDS = attrsCastDType(edgeDS, getSubGraphSpec().getEdgeAttrs());
    sparkSampling.setEdgeColumnIndex(DatasetUtils.getColumnIndex(edgeDS));
    Dataset<Row> edgeRemoveFeatureDS = edgeDS.drop("edge_feature");
    edgeRemoveFeatureDS.show();
    edgeRemoveFeatureDS.printSchema();

    Dataset<Row> graphElementMultiLayer[] = propagateSubGraphStructure(seedDS, edgeRemoveFeatureDS);
    Dataset<Row> graphElement = modifySubGraphStructure(seedDS, graphElementMultiLayer);

    Dataset<Row> subGraph = buildSubgraphWithFeature(graphElement, rawNodeFeatureDF, edgeDS);

    if (labelLevel.compareToIgnoreCase(Constants.LABEL_LEVEL_ON_NODE) == 0) {
      seedDS = DatasetUtils.aggregateConcatWs(Constants.ENTRY_SEED, seedDS);
    }
    Dataset<Row> subGraphWithLabel = subGraph.join(seedDS, Constants.ENTRY_SEED);
    sinkSubgraphWithLabel(spark, subGraphWithLabel);
  }

  public static void main(String[] args) throws Exception {
    String warehouseLocation = "spark-warehouse";
    SparkSession spark = SparkSession
        .builder()
        .appName("SparkSQL_Demo")
        .config("spark.sql.warehouse.dir", warehouseLocation)
        .enableHiveSupport()
        .getOrCreate();

    Options options = new Options();
    options.addOption(Option.builder(Constants.INPUT_EDGE).required().hasArg().build())
        .addOption(Option.builder(Constants.INPUT_LABEL).required().hasArg().build())
        .addOption(Option.builder(Constants.OUTPUT_RESULTS).required().hasArg().build())
        .addOption(Option.builder(Constants.SUBGRAPH_SPEC).required().hasArg().build())
        .addOption(Option.builder(Constants.HOP).required().hasArg().build())
        .addOption(Option.builder(Constants.SAMPLE_COND).required().hasArg().build())
        .addOption(Option.builder(Constants.INPUT_NODE_FEATURE).hasArg().build())
        .addOption(Option.builder(Constants.INDEX_METAS).hasArg().build())
        .addOption(Option.builder(Constants.TRAIN_FLAG).hasArg().build())
        .addOption(Option.builder(Constants.FILTER_COND).hasArg().build())
        .addOption(Option.builder(Constants.STORE_IDS).hasArg().build())
        .addOption(Option.builder(Constants.LABEL_LEVEL).hasArg().build());

    CommandLineParser parser = new DefaultParser();
    try {
      LOG.info("========================arguments: " + Arrays.toString(args));
      CommandLine arguments = parser.parse(options, args);

      GraphLevelSampling gnn = new GraphLevelSampling(
          arguments.getOptionValue(Constants.INPUT_EDGE),
          arguments.getOptionValue(Constants.INPUT_LABEL),
          arguments.getOptionValue(Constants.OUTPUT_RESULTS),
          arguments.getOptionValue(Constants.SUBGRAPH_SPEC),
          Integer.parseInt(arguments.getOptionValue(Constants.HOP)),
          arguments.getOptionValue(Constants.SAMPLE_COND));
      if (arguments.hasOption(Constants.INPUT_NODE_FEATURE)) {
        gnn.setNodeTable(arguments.getOptionValue(Constants.INPUT_NODE_FEATURE));
      }
      if (arguments.hasOption(Constants.INDEX_METAS)) {
        gnn.setIndexMetas(arguments.getOptionValue(Constants.INDEX_METAS));
      }
      if (arguments.hasOption(Constants.TRAIN_FLAG)) {
        gnn.setTrainFlag(arguments.getOptionValue(Constants.TRAIN_FLAG));
      }
      if (arguments.hasOption(Constants.FILTER_COND)) {
        gnn.setFilterCondition(arguments.getOptionValue(Constants.FILTER_COND));
      }
      if (!Boolean.parseBoolean(
          arguments.getOptionValue(Constants.STORE_IDS, Constants.STORE_IDS_DEFAULT))) {
        gnn.notStoreID();
      }
      if (arguments.hasOption(Constants.LABEL_LEVEL)) {
        gnn.setLabelLevel(arguments.getOptionValue(Constants.LABEL_LEVEL));
      }
      gnn.setup();

      Dataset<Row> seedDS = DatasetUtils.inputData(spark, gnn.getLabelTable());
      Dataset<Row> edgeDS = DatasetUtils.inputData(spark, gnn.getEdgeTable());
      Dataset<Row> rawNodeFeatureDF = DatasetUtils.inputData(spark, gnn.getNodeFeatureTable());

      gnn.runSamplingPipeline(spark, seedDS, edgeDS, rawNodeFeatureDF);
    } catch (ParseException e) {
      LOG.error("Create Parser Failed", e);
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(NodeLevelSampling.class.getName(), options, true);
    }
  }

}
