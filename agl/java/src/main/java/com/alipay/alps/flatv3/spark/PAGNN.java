package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.graphfeature.GraphFeatureGenerator;
import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import com.alipay.alps.flatv3.sampler.AbstractSampler;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.sampler.SamplerFactory;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;

public class PAGNN {
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

        String sampleConds = arguments.get(Constants.SAMPLE_COND);
        String otherOutputSchema = arguments.get(Constants.OTHER_OUTPUT_SCHEMA);
        int maxHop = Integer.parseInt(arguments.get(Constants.HOP));
        String subgraphSpec = arguments.get(Constants.SUBGRAPH_SPEC);
        SubGraphSpecs subGraphSpecs = new SubGraphSpecs(subgraphSpec);

        // input data
        String linkSqlScript = arguments.getOrDefault(Constants.INPUT_LINK, "");
        String inputEdge = arguments.getOrDefault(Constants.INPUT_EDGE, "");
        String inputNodeFeature = arguments.getOrDefault(Constants.INPUT_NODE_FEATURE, "");
        String inputEdgeFeature = arguments.getOrDefault(Constants.INPUT_EDGE_FEATURE, "");
        String outputResults = arguments.getOrDefault(Constants.OUTPUT_RESULTS, "");

        Dataset<Row> linkDS = Utils.inputData(spark, linkSqlScript);
        linkDS.show();
        linkDS.printSchema();
        Dataset<Row> seedDS = linkDS.select("node_id").distinct().withColumn("seed", col("node_id"));
        seedDS.show();
        seedDS.printSchema();
        Dataset<Row> neighborDF = Utils.inputData(spark, inputEdge)
                .repartition(col("node1_id")).sortWithinPartitions("node2_id")
                .groupBy("node1_id").agg(
                        collect_list("node2_id").as("collected_node2_id"),
                        collect_list("edge_id").as("collected_edge_id")).cache();
        neighborDF.show();
        neighborDF.printSchema();

        Dataset<SubGraphElement> resultGraphElement = seedDS.map((MapFunction<Row, SubGraphElement>) row -> {
            String nodeId = row.getString(0);
            String seed = row.getString(1);
            return new SubGraphElement(seed, Constants.ROOT, nodeId, nodeId, nodeId, "", "");
        }, Encoders.bean(SubGraphElement.class));
        Dataset<Row> seedAgg = seedDS.groupBy("node_id")
                .agg(collect_list("seed").as("collected_seed"));

        for (int hop = 0; hop < maxHop; hop++) {
            Dataset<SubGraphElement> seedPropagationResults = neighborDF.joinWith(seedAgg, neighborDF.col("node1_id").equalTo(seedAgg.col("node_id")), "inner")
                    .flatMap(getPropergateFunction(otherOutputSchema, sampleConds, hop), Encoders.bean(SubGraphElement.class));//.cache();
            resultGraphElement = resultGraphElement.union(seedPropagationResults).distinct();
            if (hop + 1 < maxHop) {
                seedAgg = seedPropagationResults.filter("entryType = 'node'")
                        .distinct()
                        .groupBy("node1")
                        .agg(collect_list("seed").as("collected_seed"))
                        .withColumnRenamed("node1", "node_id");
            }
        }

        Dataset<SubGraphElement> linkSubgraphDS = linkDS.joinWith(resultGraphElement, linkDS.col("node_id").equalTo(resultGraphElement.col("seed")), "inner")
                .groupByKey((MapFunction<Tuple2<Row, SubGraphElement>, String>) t -> t._1().getString(1), Encoders.STRING())
                .flatMapGroups(getGeneratePathFunc(maxHop), Encoders.bean(SubGraphElement.class));
        linkSubgraphDS.show();
        linkSubgraphDS.printSchema();

        Dataset<SubGraphElement> nodeStructure = linkSubgraphDS.filter("entryType = 'node' or entryType = 'root'");
        Dataset<SubGraphElement> nodeFeatureDF = nodeStructure;
        if (inputNodeFeature.trim().length() > 0) {
            Dataset<Row> rawNodeFeatureDF = Utils.inputData(spark, inputNodeFeature);
            nodeFeatureDF = nodeStructure
                    .join(rawNodeFeatureDF, nodeStructure.col("node1").equalTo(rawNodeFeatureDF.col("node_id")), "inner")
                    .select("seed", "entryType", "node1", "node2", "node_id", "node_feature")
                    .map((MapFunction<Row, SubGraphElement>) (Row row) -> {
                                return new SubGraphElement(row.getString(0), row.getString(1), row.getString(2), row.getString(3), row.getString(4), row.getString(5), "");
                            },
                            Encoders.bean(SubGraphElement.class));
        }

        Dataset<SubGraphElement> edgeStructure = linkSubgraphDS.filter("entryType = 'edge'");
        Dataset<SubGraphElement> edgeFeatureDF = edgeStructure;
        if (inputEdgeFeature.trim().length() > 0) {
            Dataset<Row> rawEdgeFeatureDF = Utils.inputData(spark, inputEdgeFeature);
            edgeFeatureDF = rawEdgeFeatureDF.join(edgeStructure, edgeStructure.col("eid").equalTo(rawEdgeFeatureDF.col("eid")), "inner")
                    .select("seed", "entryType", "node1", "node2", "eid", "edge_feature")
                    .map((MapFunction<Row, SubGraphElement>) row -> new SubGraphElement(row.getString(0), row.getString(1), row.getString(2), row.getString(3), row.getString(4), row.getString(5), ""),
                            Encoders.bean(SubGraphElement.class));
        }

        // group node features and edge features by seed
        Dataset<Row> subgraph = nodeFeatureDF.union(edgeFeatureDF).groupByKey((MapFunction<SubGraphElement, String>) row -> row.getSeed(), Encoders.STRING())
                .mapGroups(getGenerateGraphFeatureFunc(subGraphSpecs), Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("seed", "graph_feature");
        Utils.outputData(spark, subgraph, outputResults);

    }


    public static FlatMapFunction<Tuple2<Row, Row>, SubGraphElement> getPropergateFunction(String otherOutputSchema, String sampleCond, int hop) {
        return
                new FlatMapFunction<Tuple2<Row, Row>, SubGraphElement>() {
                    @Override
                    public Iterator<SubGraphElement> call(Tuple2<Row, Row> row) throws Exception {
                        // seeds:  node_id, seed
                        Row seedRow = row._2;
                        // List<String> seedList = seedRow.getList(1);
                        List<String> seedList = new ArrayList<>();
                        for (int i = 0; i < seedRow.getList(1).size(); i++) {
                            seedList.add(seedRow.getList(1).get(i).toString());
                        }
                        Collections.sort(seedList);

                        // neighbors: node1_id, node2_ids, edge_ids
                        Row indexInfo = row._1;
                        List<String> node2IDs = indexInfo.getList(1);
                        List<String> edgeIDs = indexInfo.getList(2);
                        String node1_id = indexInfo.getString(0);
                        List<RangeUnit> sortedIntervals = new ArrayList<>();
                        sortedIntervals.add(new RangeUnit(0, node2IDs.size()-1));
                        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleCond), null);
                        List<SubGraphElement> ans = new ArrayList<>();
                        for (int seedIdx = 0; seedIdx < seedList.size(); seedIdx++) {
                            String seed = seedList.get(seedIdx);
                            List<Integer> neighborIndices = sampler.sample(new RangeResult(null, sortedIntervals));
                            for (int i : neighborIndices) {
                                String node2ID = node2IDs.get(i);
                                String edgeID = edgeIDs.get(i);
                                SubGraphElement subGraphNodeElement = new SubGraphElement(seed, "node", node2ID, null, null, null, null);
                                ans.add(subGraphNodeElement);
                                SubGraphElement subGraphEdgeElement = new SubGraphElement(seed, "edge", node1_id, node2ID, edgeID, null, null);
                                ans.add(subGraphEdgeElement);
                            }
                        }
                        return ans.iterator();
                    }
                };
    }

    public static FlatMapGroupsFunction<String, Tuple2<Row, SubGraphElement>, SubGraphElement> getGeneratePathFunc(int maxHop) {
        return new FlatMapGroupsFunction<String, Tuple2<Row, SubGraphElement>, SubGraphElement>() {
            @Override
            public Iterator<SubGraphElement> call(String key, Iterator<Tuple2<Row, SubGraphElement>> values)
                    throws Exception {
                List<SubGraphElement>[] subGraphs = new List[2];
                Set<String>[] nodeIDs = new Set[2];
                Map<String, Map<String, String>>[] node1IDToNode2IDs = new Map[2];
                for (int i = 0; i < 2; i++) {
                    subGraphs[i] = new ArrayList<>();
                    nodeIDs[i] = new HashSet<>();
                    node1IDToNode2IDs[i] = new HashMap<>();
                }
                HashMap<String, Integer> seedIDToIndex = new HashMap<>();
                while (values.hasNext()) {
                    Tuple2<Row, SubGraphElement> value = values.next();
                    SubGraphElement subGraphElement = value._2;
                    String seed = subGraphElement.getSeed();
                    if (!seedIDToIndex.containsKey(seed)) {
                        seedIDToIndex.put(seed, seedIDToIndex.size());
                    }
                    int seedIndex = seedIDToIndex.get(seed);
                    subGraphs[seedIndex].add(subGraphElement);
                }
                String rootA = getRootIDNodesAndEdges(subGraphs[0], node1IDToNode2IDs[0]);
                String rootB = getRootIDNodesAndEdges(subGraphs[1], node1IDToNode2IDs[1]);
                removeDirectedEdgeBetweenRoots(rootA, rootB, nodeIDs[0], node1IDToNode2IDs[0]);
                removeDirectedEdgeBetweenRoots(rootB, rootA, nodeIDs[1], node1IDToNode2IDs[1]);
                Map<String, Set<String>> paths = new HashMap<>();
                findPathFromLeftToRight(rootA, node1IDToNode2IDs[0], nodeIDs[1], node1IDToNode2IDs[1], paths, maxHop);
                findPathFromLeftToRight(rootB, node1IDToNode2IDs[1], nodeIDs[0], node1IDToNode2IDs[0], paths, maxHop);
                // generate result graph from paths
                List<SubGraphElement> resultGraph = new ArrayList<>();
                Set<String> resultNodes = new HashSet<>();
                for (String node1 : paths.keySet()) {
                    resultNodes.add(node1);
                    for (String node2 : paths.get(node1)) {
                        String edgeId = null;
                        if (node1IDToNode2IDs[0].containsKey(node1) && node1IDToNode2IDs[0].get(node1).containsKey(node2)) {
                            edgeId = node1IDToNode2IDs[0].get(node1).get(node2);
                        }
                        if (edgeId == null && node1IDToNode2IDs[1].containsKey(node1) && node1IDToNode2IDs[1].get(node1).containsKey(node2)) {
                            edgeId = node1IDToNode2IDs[1].get(node1).get(node2);
                        }
                        resultGraph.add(new SubGraphElement(key, Constants.EDGE, node1, node2, edgeId, "", ""));
                        resultNodes.add(node2);
                    }
                }
                for (String node : resultNodes) {
                    resultGraph.add(new SubGraphElement(key, Constants.NODE, node, "", "", "", ""));
                }
                resultGraph.add(new SubGraphElement(key, Constants.ROOT, rootA, "", rootA, rootB, ""));
                resultGraph.add(new SubGraphElement(key, Constants.ROOT, rootB, "", rootA, rootB, ""));
                return resultGraph.iterator();
            }
        };
    }

    static private void removeDirectedEdgeBetweenRoots(String nodeA, String nodeB, Set<String> nodeIDA, Map<String, Map<String, String>> node1IDToNode2IDA) {
        if (node1IDToNode2IDA.containsKey(nodeA) && node1IDToNode2IDA.get(nodeA).containsKey(nodeB)) {
            node1IDToNode2IDA.get(nodeA).remove(nodeB);
        }
        if (node1IDToNode2IDA.containsKey(nodeB) && node1IDToNode2IDA.get(nodeB).containsKey(nodeA)) {
            node1IDToNode2IDA.get(nodeB).remove(nodeA);
        }
        if (node1IDToNode2IDA.containsKey(nodeB)) {
            node1IDToNode2IDA.remove(nodeB);
        }
        // add all nodes in node1IDToNode2IDA to nodeIDA
        nodeIDA.clear();
        for (Map.Entry<String, Map<String, String>> entry : node1IDToNode2IDA.entrySet()) {
            nodeIDA.add(entry.getKey());
            for (Map.Entry<String, String> entry2 : entry.getValue().entrySet()) {
                nodeIDA.add(entry2.getKey());
            }
        }
    }

    static private String getRootIDNodesAndEdges(List<SubGraphElement> subGraphElements, Map<String, Map<String, String>> node1IDToNode2IDs) {
        String rootID = null;
        for (SubGraphElement element : subGraphElements) {
            if (element.getEntryType().compareTo(Constants.ROOT) == 0) {
                rootID = element.getNode1();
            } else if (element.getEntryType().compareTo(Constants.EDGE) == 0) {
                if (!node1IDToNode2IDs.containsKey(element.getNode1())) {
                    node1IDToNode2IDs.put(element.getNode1(), new HashMap<String, String>());
                }
                node1IDToNode2IDs.get(element.getNode1()).put(element.getNode2(), element.getID());
            }
        }
        return rootID;
    }

    static private void findPathFromLeftToRight(String rootLeft, Map<String, Map<String, String>> node1IDToNode2IDsLeft, Set<String> nodeIDsRight, Map<String, Map<String, String>> node1IDToNode2IDsRight, Map<String, Set<String>> paths, int maxHop) {
        // use bfs to find path between nodeA and nodeB, add necessary edges and nodes into resultGraph
        Stack<Tuple3<String, String, Integer>> edgeStack = new Stack<>();
        Set<String> visited = new HashSet<>();
        visited.add(rootLeft);
        edgeStack.add(new Tuple3<>(rootLeft, null, 0));
        String previousAddedNode2 = null;
        while (!edgeStack.isEmpty()) {
            Tuple3<String, String, Integer> currentParentHop = edgeStack.pop();
            if (currentParentHop != null) {
                edgeStack.add(currentParentHop);
                edgeStack.add(null);
                if (currentParentHop._3() >= maxHop) {
                    continue;
                }
                if (node1IDToNode2IDsLeft.containsKey(currentParentHop._1())) {
                    for (Map.Entry<String, String> entry : node1IDToNode2IDsLeft.get(currentParentHop._1()).entrySet()) {
                        String nodeNextLayer = (String) entry.getKey();
                        String edgeID = (String) entry.getValue();
                        if (visited.contains(edgeID)) {
                            continue;
                        }
                        visited.add(edgeID);
                        // if this edge exists both in left graph and right graph, do not add it into resultGraph
                        if (node1IDToNode2IDsRight.containsKey(currentParentHop._1()) && node1IDToNode2IDsRight.get(currentParentHop._1()).containsKey(nodeNextLayer)) {
//                            System.out.println("=======skip nodeNextLayer:" + nodeNextLayer + " parent:" + currentParentHop._1());
                            continue;
                        }
                        edgeStack.add(new Tuple3<>(nodeNextLayer, currentParentHop._1(), currentParentHop._3() + 1));
                    }
                }
            } else {
                Tuple3<String, String, Integer> q = edgeStack.pop();
                assert q != null;
                if (q._2() == null) {
                    continue;
                }
                String nodeNextLayer = q._1();
                String n1 = q._2();
                if (nodeIDsRight.contains(nodeNextLayer) || nodeNextLayer == previousAddedNode2 /*??*/) {
                    // find path
                    if (!paths.containsKey(n1)) {
                        paths.put(n1, new HashSet<>());
                    }
                    paths.get(n1).add(nodeNextLayer);
                    // System.out.println("find path: " + n1 + " -> " + nodeNextLayer);
                    previousAddedNode2 = n1;
                }
            }
        }
    }

    // s.seed, s.entryType, r.edge_id, r.edge_feature, r.node1_id, r.node2_id
    public static MapGroupsFunction<String, SubGraphElement, Tuple2<String, String>> getGenerateGraphFeatureFunc(SubGraphSpecs subGraphSpecs) {
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
                // return new Tuple2<>(seed, "");
                return new Tuple2<>(seed, graphFeatureGenerator.getGraphFeature(rootIds));

            }
        };
    }

    public static String printMap(Map<String, Map<String, String>> node1IDToNode2ID) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> entry : node1IDToNode2ID.entrySet()) {
            sb.append(entry.getKey() + ": ");
            for (Map.Entry<String, String> entry2 : entry.getValue().entrySet()) {
                sb.append(entry2.getKey() + " ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String printPaths(Map<String, Set<String>> paths) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : paths.entrySet()) {
            sb.append(entry.getKey() + ": ");
            for (String node : entry.getValue()) {
                sb.append(node + " ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}
