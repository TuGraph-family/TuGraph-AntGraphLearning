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

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;

import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import com.alipay.alps.flatv3.lib.GNNSamplingLib;
import com.alipay.alps.flatv3.lib.spark.SparkSampling;
import com.alipay.alps.flatv3.spark.utils.Constants;
import com.alipay.alps.flatv3.spark.utils.DatasetUtils;
import com.antfin.agl.proto.graph_feature.DType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeLevelSampling {

  private static final Logger LOG = LoggerFactory.getLogger(NodeLevelSampling.class);

  // required parameters
  @Getter
  private String edgeTable;
  @Getter
  private String labelTable;
  @Getter
  private String outputTablePrefix;
  @Getter
  private SubGraphSpecs subGraphSpec;
  @Getter
  private int maxHop;
  private String nodeFeatureTable;
  private String trainFlag;
  private boolean removeEdgeAmongRoots = false;
  public SparkSampling sparkSampling = null;

  public NodeLevelSampling(String edgeTable, String labelTable, String outputTablePrefix,
      String subGraphSpec, int maxHop, String sampleCondition) throws Exception {
    this.edgeTable = edgeTable;
    this.labelTable = labelTable;
    this.outputTablePrefix = outputTablePrefix;
    this.subGraphSpec = new SubGraphSpecs(subGraphSpec);
    this.maxHop = maxHop;
    sparkSampling = new SparkSampling(new GNNSamplingLib(subGraphSpec, sampleCondition, maxHop));
  }

  public void setIndexMetas(String indexMetas) {
    this.sparkSampling.getGNNSamplingLib().setIndexMetas(indexMetas);
  }

  public void setFilterCondition(String filterCondition) {
    this.sparkSampling.getGNNSamplingLib().setFilterCondition(filterCondition);
  }

  public void notStoreID() {
    this.sparkSampling.getGNNSamplingLib().notStoreID();
  }

  public void setNodeTable(String nodeFeatureTable) {
    this.nodeFeatureTable = nodeFeatureTable;
  }

  public void setTrainFlag(String trainFlag) {
    this.trainFlag = trainFlag;
  }

  public void setRemoveEdgeAmongRoots(boolean removeEdgeAmongRoots) {
    this.removeEdgeAmongRoots = removeEdgeAmongRoots;
  }

  public static int getFieldIndex(String filedName) {
    return Constants.ELEMENT_FIELD_INDEX.get(filedName);
  }

  public String getEdgeTable() {
    return edgeTable;
  }

  public String getLabelTable() {
    return labelTable;
  }

  public String getOutputTablePrefix() {
    return outputTablePrefix;
  }

  public SubGraphSpecs getSubGraphSpec() {
    return subGraphSpec;
  }

  public int getMaxHop() {
    return maxHop;
  }

  public String getNodeFeatureTable() {
    return nodeFeatureTable;
  }

  public String getTrainFlag() {
    return trainFlag;
  }

  public boolean getRemoveEdgeAmongRoots() {
    return removeEdgeAmongRoots;
  }

  private DataType getDatatype(DType type) {
    switch (type) {
      case INT8:
      case UINT16:
      case INT16:
      case UINT32:
      case INT32:
      case UINT64:
      case INT64:
        return DataTypes.LongType;
      case FLOAT:
      case DOUBLE:
        return DataTypes.FloatType;
      case STR:
        return DataTypes.StringType;
      default:
        return DataTypes.NullType;
    }
  }

  public void setup() throws Exception {
    sparkSampling.setup();
  }

  public void runSamplingPipeline(SparkSession spark, Dataset<Row> seedDS, Dataset<Row> edgeDS,
      Dataset<Row> rawNodeFeatureDF) {
    seedDS = attrsCastDType(seedDS, getSubGraphSpec().getSeedAttrs());

    edgeDS = attrsCastDType(edgeDS, getSubGraphSpec().getEdgeAttrs());
    sparkSampling.setEdgeColumnIndex(DatasetUtils.getColumnIndex(edgeDS));

    Dataset<Row> edgeRemoveFeatureDS = edgeDS.drop("edge_feature");
    Dataset<Row> graphElementMultiLayer[] = propagateSubGraphStructure(seedDS, edgeRemoveFeatureDS);
    Dataset<Row> graphElement = modifySubGraphStructure(seedDS, graphElementMultiLayer);

    Dataset<Row> subgraph = buildSubgraphWithFeature(graphElement, rawNodeFeatureDF, edgeDS);

    Dataset<Row> subgraphWithLabel = subgraph.join(seedDS, Constants.ENTRY_SEED);

    sinkSubgraphWithLabel(spark, subgraphWithLabel);
  }

  public Dataset<Row> attrsCastDType(Dataset<Row> seedDS, Map<String, DType> seedAttrs) {
    for (String field : seedAttrs.keySet()) {
      seedDS = seedDS.withColumn(field, col(field).cast(getDatatype(seedAttrs.get(field))));
    }
    return seedDS;
  }

  public Dataset<Row> aggNeighborData(Dataset<Row> edgeDS) {
    String edgeColumnNames[] = edgeDS.columns();
    List<Column> aggColumns = new ArrayList<>();
    for (int i = 1; i < edgeColumnNames.length; i++) {
      aggColumns.add(collect_list(col(edgeColumnNames[i])).as(edgeColumnNames[i]));
    }
    Dataset<Row> neighborDS = edgeDS
        .repartition(col("node1_id")).sortWithinPartitions("edge_id")
        .groupBy("node1_id")
        .agg(aggColumns.get(0),
            aggColumns.subList(1, aggColumns.size()).toArray(new org.apache.spark.sql.Column[0]));

    neighborDS = sparkSampling.buildIndexes(neighborDS);

    if (sparkSampling.getNeighborColumnIndex() == null) {
      sparkSampling.setNeighborColumnIndex(DatasetUtils.getColumnIndex(neighborDS));
    }
    return neighborDS;
  }

  public Dataset<Row> aggSeedData(Dataset<Row> seedDS, Map<String, DType> seedAttrs) {
    List<Column> seedAggColumns = new ArrayList<>();
    for (String field : seedAttrs.keySet()) {
      seedAggColumns.add(collect_list(col(field)).as(field));
    }
    Dataset<Row> seedAgg = seedDS.groupBy("node_id")
        .agg(collect_list("seed").as("seed")
            , seedAggColumns.toArray(new org.apache.spark.sql.Column[0])
        );
    if (sparkSampling.getSeedColumnIndex() == null) {
      sparkSampling.setSeedColumnIndex(DatasetUtils.getColumnIndex(seedAgg));
    }
    return seedAgg;
  }

  public Dataset<Row> processRootNode(Dataset<Row> seedDS,
      ExpressionEncoder expressionEncoderWithOtherOutput) {
    return seedDS.flatMap(sparkSampling.getRootNode(), expressionEncoderWithOtherOutput);
  }

  public Dataset<Row> propagateOneHop(Dataset<Row> seedAggDS, Dataset<Row> neighborDS,
      ExpressionEncoder expressionEncoderWithOtherOutput, int hop) {
    return neighborDS
        .joinWith(seedAggDS, neighborDS.col("node1_id").equalTo(seedAggDS.col("node_id")), "inner")
        .flatMap(sparkSampling.getNeighborSelectionFunction(hop),
            expressionEncoderWithOtherOutput);
  }

  public Dataset<Row>[] propagateSubGraphStructure(Dataset<Row> seedDS, Dataset<Row> edgeDS) {
    Dataset<Row> seedAgg = aggSeedData(seedDS, subGraphSpec.getSeedAttrs());
    Dataset<Row> neighborDS = aggNeighborData(edgeDS);

    ExpressionEncoder expressionEncoderWithOtherOutput = sparkSampling
        .getExpressionEncoderWithOtherOutput();
    Dataset<Row> resultGraphElement[] = new Dataset[maxHop + 1];
    sparkSampling.setOriginSeedColumnIndex(DatasetUtils.getColumnIndex(seedDS));
    resultGraphElement[0] = processRootNode(seedDS, expressionEncoderWithOtherOutput);

    for (int hop = 1; hop <= maxHop; hop++) {
      resultGraphElement[hop] = propagateOneHop(seedAgg, neighborDS,
          expressionEncoderWithOtherOutput, hop);
      if (hop < maxHop) {
        Dataset<Row> seedNodes = resultGraphElement[hop]
            .filter(Constants.ENTRY_TYPE + " = " + Constants.NODE_INT).distinct();
        seedAgg = aggSeedData(seedNodes.withColumnRenamed("id", "node_id"),
            subGraphSpec.getSeedAttrs());
      }
    }
    return resultGraphElement;
  }

  public Dataset<Row> modifySubGraphStructure(Dataset<Row> labelDS,
      Dataset<Row> graphElementMultiLayer[]) {
    Dataset<Row> graphElement = graphElementMultiLayer[1];
    for (int hop = 2; hop <= maxHop; hop++) {
      graphElement = graphElement.union(graphElementMultiLayer[hop]);
    }
    graphElement = graphElement.distinct().cache();
    graphElement = graphElement.union(graphElementMultiLayer[0]);
    return graphElement;
  }

  private Dataset<Row> joinFeature(Dataset<Row> graphStructure, Dataset<Row> rawFeatureDF,
      String idColumnName, String featureColumnName) {
    if (rawFeatureDF != null) {
      graphStructure = rawFeatureDF
          .join(graphStructure, graphStructure.col("id").equalTo(rawFeatureDF.col(idColumnName)),
              "inner")
          .select(graphStructure.col(Constants.ENTRY_SEED),
              graphStructure.col(Constants.ENTRY_TYPE), graphStructure.col(Constants.ENTRY_NODE1),
              graphStructure.col(Constants.ENTRY_NODE2), graphStructure.col(Constants.ENTRY_ID),
              rawFeatureDF.col(featureColumnName), graphStructure.col(Constants.ENTRY_KIND));
    }
    return graphStructure;
  }

  public Dataset<Row> buildSubgraphWithFeature(Dataset<Row> resultGraphElement,
      Dataset<Row> rawNodeFeatureDF, Dataset<Row> rawEdgeFeatureDF) {
    Dataset<Row> nodeElement = resultGraphElement.filter(
        Constants.ENTRY_TYPE + " = " + Constants.NODE_INT + " or " + Constants.ENTRY_TYPE + " = "
            + Constants.ROOT_INT);
    if (rawNodeFeatureDF != null) {
      nodeElement = joinFeature(nodeElement, rawNodeFeatureDF, "node_id", "node_feature")
          .withColumnRenamed("node_feature", "feature");
    }

    Dataset<Row> edgeElement = resultGraphElement
        .filter(Constants.ENTRY_TYPE + " = " + Constants.EDGE_INT);
    if (sparkSampling.getEdgeColumnIndex().containsKey("edge_feature")) {
      edgeElement = joinFeature(edgeElement, rawEdgeFeatureDF, "edge_id", "edge_feature")
          .withColumnRenamed("edge_feature", "feature");
    }

    Dataset<Row> subgraph = nodeElement.union(edgeElement)
        .groupByKey((MapFunction<Row, String>) row -> row.getString(0), Encoders.STRING())
        .mapGroups(sparkSampling.getGenerateGraphFeatureFunc(getRemoveEdgeAmongRoots(), true),
            Encoders.tuple(Encoders.STRING(), Encoders.STRING()))
        .toDF(Constants.ENTRY_SEED, "graph_feature");
    return subgraph;
  }

  public void sinkSubgraphWithLabel(SparkSession spark, Dataset<Row> subgraph) {
    if (trainFlag != null && !trainFlag.trim().isEmpty()) {
      subgraph.cache();
      List<Row> trainFlagList = subgraph.select(trainFlag).distinct().collectAsList();
      for (Row trainFlagRow : trainFlagList) {
        String trainFlagVal = trainFlagRow.getString(0);
        DatasetUtils.outputData(spark,
            subgraph.filter(subgraph.col(trainFlag) + " = '" + trainFlagVal + "'"),
            outputTablePrefix + "_" + trainFlagVal);
      }
    } else {
      DatasetUtils.outputData(spark, subgraph, outputTablePrefix);
    }
  }

  public static void main(String[] args) throws Exception {
    String warehouseLocation = "spark-warehouse";
    SparkSession spark = SparkSession
        .builder()
        .appName("SparkSQL_Demo")
        .config("spark.sql.warehouse.dir", warehouseLocation)
        .enableHiveSupport()
        .getOrCreate();
    LOG.info("========================args: " + Arrays.toString(args));
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
        .addOption(Option.builder(Constants.STORE_IDS).hasArg().build());

    CommandLineParser parser = new DefaultParser();
    try {
      LOG.info("========================arguments: " + Arrays.toString(args));
      CommandLine arguments = parser.parse(options, args);

      NodeLevelSampling nodeLevelSampling = new NodeLevelSampling(
          arguments.getOptionValue(Constants.INPUT_EDGE),
          arguments.getOptionValue(Constants.INPUT_LABEL),
          arguments.getOptionValue(Constants.OUTPUT_RESULTS),
          arguments.getOptionValue(Constants.SUBGRAPH_SPEC),
          Integer.parseInt(arguments.getOptionValue(Constants.HOP)),
          arguments.getOptionValue(Constants.SAMPLE_COND));
      if (arguments.hasOption(Constants.INPUT_NODE_FEATURE)) {
        nodeLevelSampling.setNodeTable(arguments.getOptionValue(Constants.INPUT_NODE_FEATURE));
      }
      if (arguments.hasOption(Constants.INDEX_METAS)) {
        nodeLevelSampling.setIndexMetas(arguments.getOptionValue(Constants.INDEX_METAS));
      }
      if (arguments.hasOption(Constants.TRAIN_FLAG)) {
        nodeLevelSampling.setTrainFlag(arguments.getOptionValue(Constants.TRAIN_FLAG));
      }
      if (arguments.hasOption(Constants.FILTER_COND)) {
        nodeLevelSampling.setFilterCondition(arguments.getOptionValue(Constants.FILTER_COND));
      }
      if (!Boolean.parseBoolean(
          arguments.getOptionValue(Constants.STORE_IDS, Constants.STORE_IDS_DEFAULT))) {
        nodeLevelSampling.notStoreID();
      }
      nodeLevelSampling.setup();

      Dataset<Row> seedDS = DatasetUtils.inputData(spark, nodeLevelSampling.getLabelTable());
      Dataset<Row> edgeDS = DatasetUtils.inputData(spark, nodeLevelSampling.getEdgeTable());
      Dataset<Row> rawNodeFeatureDF = null;
      if (nodeLevelSampling.getNodeFeatureTable() != null) {
        rawNodeFeatureDF = DatasetUtils.inputData(spark, nodeLevelSampling.getNodeFeatureTable());
      }

      nodeLevelSampling.runSamplingPipeline(spark, seedDS, edgeDS, rawNodeFeatureDF);
    } catch (ParseException e) {
      LOG.error("Create Parser Failed", e);
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(NodeLevelSampling.class.getName(), options, true);
    }
  }

}
