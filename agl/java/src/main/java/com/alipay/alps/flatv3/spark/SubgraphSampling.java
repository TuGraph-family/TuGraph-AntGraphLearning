package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.graphfeature.GraphFeatureGenerator;
import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import com.alipay.alps.flatv3.sampler.AbstractSampler;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.sampler.SamplerFactory;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import scala.Tuple4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.spark.sql.functions.broadcast;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;


public class SubgraphSampling {

    public static void main(String[] args) throws Exception {
        String warehouseLocation = "spark-warehouse";
        SparkSession spark = SparkSession
                .builder()
                .appName("SparkSQL_Demo")
                .config("spark.sql.warehouse.dir", warehouseLocation)
                .enableHiveSupport()
                .getOrCreate();
        Map<String, String> arguments = Utils.populateArgumentMap(args);
        System.out.println("========================arguments: " + Arrays.toString(arguments.entrySet().toArray()));

        int maxHop = Integer.parseInt(arguments.get(Constants.HOP));
        String sampleCond[] = new String[maxHop];
        String sampleCondStr[] = arguments.get(Constants.SAMPLE_COND).split(";");
        for (int i = 0; i < maxHop; i++) {
            sampleCond[i] = i < sampleCondStr.length ? sampleCondStr[i] : sampleCondStr[sampleCondStr.length-1];
        }
        String subgraphSpec = arguments.get(Constants.SUBGRAPH_SPEC);
        Boolean needIDs = Boolean.parseBoolean(arguments.getOrDefault(Constants.NEED_IDS, Constants.NEED_IDS_DEFAULT));

        // input data
        String inputLabel = arguments.getOrDefault(Constants.INPUT_LABEL, "");
        String inputEdge = arguments.getOrDefault(Constants.INPUT_EDGE, "");
        String inputNodeFeature = arguments.getOrDefault(Constants.INPUT_NODE_FEATURE, "");
        String inputEdgeFeature = arguments.getOrDefault(Constants.INPUT_EDGE_FEATURE, "");
        String outputResults = arguments.getOrDefault(Constants.OUTPUT_RESULTS, "");

        String indexMetasStr = "";
        String otherOutputSchema = "";
        String filterCond = "";
        List<String> indexMetas = Arrays.asList(indexMetasStr.split(","));
        Filter filter = new Filter(filterCond);

        SubGraphSpecs subGraphSpecs = new SubGraphSpecs(subgraphSpec);
        Dataset<Row> seedDS = Utils.inputData(spark, inputLabel);
        Dataset<Row> seedLabels = Utils.aggregateConcatWs("seed", seedDS);
        seedLabels.show();
        seedLabels.printSchema();

        Dataset<Row> neighborDF = Utils.inputData(spark, inputEdge)
                .repartition(col("node1_id")).sortWithinPartitions("node2_id")
                .groupBy("node1_id").agg(
                        collect_list("node2_id").as("collected_node2_id"),
                        collect_list("edge_id").as("collected_edge_id")).cache();

        Dataset<SubGraphElement> resultGraphElement = seedDS.map((MapFunction<Row, SubGraphElement>) row -> {
            String nodeId = row.getString(0);
            String seed = row.getString(1);
            return new SubGraphElement(seed, Constants.ROOT, nodeId, nodeId, nodeId, "", "");
        }, Encoders.bean(SubGraphElement.class));
;
        Dataset<Row> seedAgg = seedDS.groupBy("node_id")
                .agg(collect_list("seed").as("collected_seed"));

        for (int hop = 0; hop < maxHop; hop++) {
            Dataset<SubGraphElement> seedPropagationResults = neighborDF.joinWith(seedAgg, neighborDF.col("node1_id").equalTo(seedAgg.col("node_id")), "inner")
                    .flatMap(getPropergateFunction(otherOutputSchema, filter, sampleCond[hop], hop), Encoders.bean(SubGraphElement.class));
            seedPropagationResults.show();
            seedPropagationResults.printSchema();
            resultGraphElement = resultGraphElement.union(seedPropagationResults);
            if (hop + 1 < maxHop) {
                Dataset<SubGraphElement> seedNodes = seedPropagationResults.filter("entryType = 'node'").distinct();
                seedAgg = seedNodes
                        .repartition(seedNodes.col("node1")).sortWithinPartitions("seed")
                        .groupBy("node1")
                        .agg(collect_list("seed").as("collected_seed"))
                        .withColumnRenamed("node1", "node_id");
            }
        }

        resultGraphElement.distinct().cache();
        Dataset<SubGraphElement> nodeStructure = resultGraphElement.filter("entryType = 'node' or entryType = 'root'");
        Dataset<SubGraphElement> nodeFeatureDF = nodeStructure;
        if (inputNodeFeature.trim().length() > 0) {
            Dataset<Row> rawNodeFeatureDF = Utils.inputData(spark, inputNodeFeature);
            nodeFeatureDF = rawNodeFeatureDF
                    .join(nodeStructure, rawNodeFeatureDF.col("node_id").equalTo(nodeStructure.col("node1")), "inner")
                    .select("seed", "entryType", "node1", "node2", "node_id", "node_feature")
                    .map((MapFunction<Row, SubGraphElement>) (Row row) -> {
                                return new SubGraphElement(row.getString(0), row.getString(1), row.getString(2), row.getString(3), row.getString(4), row.getString(5), "");
                            },
                            Encoders.bean(SubGraphElement.class));
        }

        Dataset<SubGraphElement> edgeStructure = resultGraphElement.filter("entryType = 'edge'");
        Dataset<SubGraphElement> edgeFeatureDF = edgeStructure;
        if (inputEdgeFeature.trim().length() > 0) {
            Dataset<Row> rawEdgeFeatureDF = Utils.inputData(spark, inputEdgeFeature);
            edgeFeatureDF = rawEdgeFeatureDF.join(edgeStructure, edgeStructure.col("id").equalTo(rawEdgeFeatureDF.col("edge_id")), "inner")
                    .select("seed", "entryType", "node1", "node2", "edge_id", "edge_feature")
                    .map((MapFunction<Row, SubGraphElement>) row -> new SubGraphElement(row.getString(0), row.getString(1), row.getString(2), row.getString(3), row.getString(4), row.getString(5), ""),
                            Encoders.bean(SubGraphElement.class));
        }

        // group node features and edge features by seed
        Dataset<Row> subgraph = nodeFeatureDF.union(edgeFeatureDF).groupByKey((MapFunction<SubGraphElement, String>) row -> row.getSeed(), Encoders.STRING())
                .mapGroups(getGenerateGraphFeatureFunc(subGraphSpecs, needIDs), Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("seed", "graph_feature");
        subgraph.show();
        subgraph.printSchema();

        Dataset<Row> subgraphLabels = subgraph.join(broadcast(seedLabels), "seed");
        subgraphLabels.show();
        subgraphLabels.printSchema();
        Utils.outputData(spark, subgraphLabels, outputResults);
    }

    public static FlatMapFunction<Tuple2<Row, Row>, SubGraphElement> getPropergateFunction(String otherOutputSchema, Filter filter, String sampleCond, int hop) {
        return
                new FlatMapFunction<Tuple2<Row, Row>, SubGraphElement>() {
                    @Override
                    public Iterator<SubGraphElement> call(Tuple2<Row, Row> row) throws Exception {
                        // seeds:  node_id, seed
                        Row seedRow = row._2;
                        List<String> seedList = seedRow.getList(1);
                        // neighbors: node1_id, node2_ids, edge_ids
                        Row indexInfo = row._1;
                        List<String> node2IDs = indexInfo.getList(1);
                        List<String> edgeIDs = indexInfo.getList(2);
                        String node1_id = indexInfo.getString(0);

                        List<RangeUnit> sortedIntervals = new ArrayList<>();
                        sortedIntervals.add(new RangeUnit(0, node2IDs.size()-1));

                        List<SubGraphElement> ans = new ArrayList<>();
                        SampleCondition sampleCondition = new SampleCondition(sampleCond);
                        for (int seedIdx = 0; seedIdx < seedList.size(); seedIdx++) {
                            String seed = seedList.get(seedIdx);
                            sampleCondition.setSeed(1);
                            AbstractSampler sampler = SamplerFactory.createSampler(sampleCondition, null);
                            List<Integer> neighborIndices = sampler.sample(new RangeResult(null, sortedIntervals));
                            for (int i : neighborIndices) {
                                String node2ID = node2IDs.get(i);
                                String edgeID = edgeIDs.get(i);
                                SubGraphElement subGraphNodeElement = new SubGraphElement(seed, "node", node2ID, null, node2ID, null, null);
                                ans.add(subGraphNodeElement);
                                SubGraphElement subGraphEdgeElement = new SubGraphElement(seed, "edge", node1_id, node2ID, edgeID, null, null);
                                ans.add(subGraphEdgeElement);
                            }
                        }
                        return ans.iterator();
                    }
                };
    }

    public static MapGroupsFunction<String, SubGraphElement, Tuple2<String, String>> getGenerateGraphFeatureFunc(SubGraphSpecs subGraphSpecs, boolean needIDs) {
        return new MapGroupsFunction<String, SubGraphElement, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> call(String key, Iterator<SubGraphElement> values) throws Exception {
                String seed = key;
                GraphFeatureGenerator graphFeatureGenerator = new GraphFeatureGenerator(subGraphSpecs);
                graphFeatureGenerator.init();
                List<String> rootIds = new ArrayList<>();
                List<Tuple4<String, String, String, String>> edgeFeatures = new ArrayList<>();
                while (values.hasNext()) {
                    SubGraphElement row = values.next();
                    if (row.getEntryType().equals(Constants.ROOT)) {
                        rootIds.add(row.getID());
                        graphFeatureGenerator.addNodeInfo(row.getID(), "default", row.getFeature() == null ? "" : row.getFeature());
                    } else if (row.getEntryType().equals(Constants.NODE)) {
                        graphFeatureGenerator.addNodeInfo(row.getID(), "default", row.getFeature() == null ? "" : row.getFeature());
                    } else if (row.getEntryType().equals(Constants.EDGE)) {
                        edgeFeatures.add(new Tuple4<>(row.getNode1(), row.getNode2(), row.getID(), row.getFeature()));
                    } else {
                        throw new Exception("invalid entry type: " + row.getEntryType());
                    }
                }
                try {
                    for (Tuple4<String, String, String, String> edgeFeature : edgeFeatures) {
                        graphFeatureGenerator.addEdgeInfo(edgeFeature._1(), edgeFeature._2(), edgeFeature._3(), "default", edgeFeature._4() == null ? "" : edgeFeature._4());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("========failed for seed:" + seed + " nodeIndices:" + Arrays.toString(graphFeatureGenerator.nodeIndices.entrySet().toArray())
                            + "\nedgeIndices:" + Arrays.toString(graphFeatureGenerator.edgeIndices.entrySet().toArray())
                            + "\nnode2IDs:" + Arrays.toString(graphFeatureGenerator.node1Edges.toArray()), e);
                }
                Collections.sort(rootIds);
                return new Tuple2<>(seed, graphFeatureGenerator.getGraphFeature(rootIds, needIDs));
            }
        };
    }
}
