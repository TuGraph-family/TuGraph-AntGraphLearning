package com.alipay.alps.flatv3.spark;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.SaveMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;
import static org.apache.spark.sql.functions.concat_ws;

public class Utils {

    public static Dataset<Row> aggregateConcatWs(String key, Dataset<Row> inputDataset) {
        String[] columnNames = inputDataset.columns();
        List<Column> aggColumns = new ArrayList<>();

        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].compareTo(key) == 0) {
                continue;
            }
            aggColumns.add(concat_ws("\t", collect_list(col(columnNames[i]))).alias(columnNames[i]+"_list"));
        }

        Dataset<Row> aggDataset = inputDataset
//                .repartition(col(key)).sortWithinPartitions("node_id")
                .groupBy(col(key))
                .agg(aggColumns.get(0), aggColumns.subList(1, aggColumns.size()).toArray(new org.apache.spark.sql.Column[0]));
        return aggDataset;
    }

    public static Map<String, String> populateArgumentMap(String[] args) {
        Map<String, String> arguments = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            int equalityPos = args[i].indexOf("=");
            arguments.put(args[i].substring(0, equalityPos), args[i].substring(equalityPos + 1));
        }
        return arguments;
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
        if (input.startsWith("file:///")) {
            return spark.read().option("header", "true").option("delimiter", ",").csv(input);
        }
        return spark.sql(input);
    }
}
