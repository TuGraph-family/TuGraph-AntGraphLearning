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
import java.util.Arrays;
import lombok.Getter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HEGNN extends NodeLevelSampling {

  private static final Logger LOG = LoggerFactory.getLogger(HEGNN.class);

  @Getter
  private String hegnnMode = Constants.HEGNN_TWO_END;

  public HEGNN(String edgeTable, String labelTable, String outputTablePrefix, String subGraphSpec,
      int maxHop, String sampleCondition) throws Exception {
    super(edgeTable, labelTable, outputTablePrefix, subGraphSpec, maxHop, sampleCondition);
  }

  public void setHegnnMode(String mode) {
    hegnnMode = mode;
  }

  @Override
  public Dataset<Row> modifySubGraphStructure(Dataset<Row> labelDS,
      Dataset<Row> graphElementMultiLayer[]) {
    Dataset<Row> graphElement = graphElementMultiLayer[0];
    if (hegnnMode.compareToIgnoreCase(Constants.HEGNN_PATH) == 0) {
      for (int hop = 1; hop <= getMaxHop(); hop++) {
        graphElement = graphElement.union(graphElementMultiLayer[hop]);
      }
    } else {
      graphElement = graphElement.union(graphElementMultiLayer[getMaxHop()]
          .filter(Constants.ENTRY_TYPE + " = " + Constants.NODE_INT));
    }
    return graphElement;
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
        .addOption(Option.builder(Constants.HEGNN_MODE).hasArg().build());

    CommandLineParser parser = new DefaultParser();
    try {
      LOG.info("========================arguments: " + Arrays.toString(args));
      CommandLine arguments = parser.parse(options, args);

      HEGNN gnn = new HEGNN(arguments.getOptionValue(Constants.INPUT_EDGE),
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
      if (arguments.hasOption(Constants.HEGNN_MODE)) {
        gnn.setHegnnMode(arguments.getOptionValue(Constants.HEGNN_MODE));
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
