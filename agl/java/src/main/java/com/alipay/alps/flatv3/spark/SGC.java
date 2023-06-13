package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.apache.spark.sql.functions.callUDF;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.udaf;

public class SGC {
    public static void main(String[] args) throws Exception {
        String warehouseLocation = "spark-warehouse";
        SparkSession spark = SparkSession
                .builder()
                .appName("SparkSQL_Demo")
                .config("spark.sql.warehouse.dir", warehouseLocation)
                .enableHiveSupport()
                .getOrCreate();

        Map<String, String> arguments = Utils.populateArgumentMap(args);
        System.out.println("========================input arguments: " + Arrays.toString(arguments.entrySet().toArray()));

        // input data
        String inputEdge = arguments.getOrDefault(Constants.INPUT_EDGE, "");
        String inputNodeFeature = arguments.getOrDefault(Constants.INPUT_NODE_FEATURE, "");
        String outputResults = arguments.getOrDefault(Constants.OUTPUT_RESULTS, "");

        int maxHop = Integer.parseInt(arguments.get(Constants.HOP));
        String subgraphSpec = arguments.get(Constants.SUBGRAPH_SPEC);

        SubGraphSpecs subGraphSpecs = new SubGraphSpecs(subgraphSpec);

        Dataset<Row> nodeFeatures = Utils.inputData(spark, inputNodeFeature).map((MapFunction<Row, Tuple2<String, String>>) row -> {
            String n1 = row.getString(0);
            String nodeFeature = row.getString(1);
            return new Tuple2<>(n1, subGraphSpecs.generateNodeFeaturesBase64(nodeFeature, "default"));
        }, Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("node_id", "featureStr");


        Dataset<Row> edgeWeight = Utils.inputData(spark, inputEdge);
        Dataset<Row> isolatedNodeFeatures = nodeFeatures.join(edgeWeight, nodeFeatures.col("node_id").equalTo(edgeWeight.col("node1_id")), "leftanti");
        FeaturesSum featuresSum = new FeaturesSum();
        spark.udf().register("featuresSum", udaf(featuresSum, Encoders.bean(FeaturesWeight.class)));
        for (int hop = 0; hop < maxHop; hop++) {
            nodeFeatures = nodeFeatures.join(edgeWeight, nodeFeatures.col("node_id").equalTo(edgeWeight.col("node2_id")), "inner")
                    .select(col("node1_id").as("node_id"), col("featureStr"), col("weight"))
                    .groupBy("node_id")
                    .agg(callUDF("featuresSum", col("featureStr"), col("weight")).as("featureStr"))
                    .union(isolatedNodeFeatures);
        }
        Utils.outputData(spark, nodeFeatures, outputResults);
    }
}
