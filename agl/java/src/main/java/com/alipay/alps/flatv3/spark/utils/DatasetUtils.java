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

package com.alipay.alps.flatv3.spark.utils;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;
import static org.apache.spark.sql.functions.concat_ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetUtils.class);

  public static Dataset<Row> aggregateConcatWs(String key, Dataset<Row> inputDataset) {
    String[] columnNames = inputDataset.columns();
    List<Column> aggColumns = new ArrayList<>();

    for (int i = 0; i < columnNames.length; i++) {
      if (columnNames[i].compareTo(key) == 0) {
        continue;
      }
      aggColumns
          .add(concat_ws("\t", collect_list(col(columnNames[i]))).alias(columnNames[i] + "_list"));
    }

    Dataset<Row> aggDataset = inputDataset
        .repartition(col(key)).sortWithinPartitions("node_id")
        .groupBy(col(key))
        .agg(aggColumns.get(0),
            aggColumns.subList(1, aggColumns.size()).toArray(new org.apache.spark.sql.Column[0]));
    return aggDataset;
  }

  public static void outputData(SparkSession spark, Dataset<Row> subgraph, String output) {
    if (output.startsWith("file:///")) {
      subgraph.repartition(1).write().mode(SaveMode.Overwrite).option("header", "true").csv(output);
    } else {
      spark.sql("DROP TABLE IF EXISTS " + output);
      subgraph.write().saveAsTable(output);
    }
  }

  public static Dataset<Row> inputData(SparkSession spark, String input) {
    if (input == null) {
      return null;
    }
    if (input.startsWith("file:///")) {
      if (input.substring("file:///".length()).trim().isEmpty()) {
        LOG.info("Input file is empty:" + input);
        return null;
      }
      return spark.read().option("header", "true").option("delimiter", ",").csv(input);
    }
    return spark.sql(input);
  }

  public static Map<String, Integer> getColumnIndex(Dataset<Row> dataset) {
    String seedColumnNames[] = dataset.columns();
    Map<String, Integer> columnIndex = new HashMap<>();
    for (int i = 0; i < seedColumnNames.length; i++) {
      columnIndex.put(seedColumnNames[i], i);
    }
    return columnIndex;
  }

}
