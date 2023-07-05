package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.graphfeature.GraphFeatureGenerator;
import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.neighbor_selection.PropagateSeed;
import com.alipay.alps.flatv3.neighbor_selection.SampleOtherOutput;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import scala.Tuple2;
import scala.Tuple4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;

public class DynamicGraph {
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
        // input data
        String inputLabel = arguments.getOrDefault(Constants.INPUT_LABEL, "");
        String inputEdge = arguments.getOrDefault(Constants.INPUT_EDGE, "");
        String inputNodeFeature = arguments.getOrDefault(Constants.INPUT_NODE_FEATURE, "");
        String inputEdgeFeature = arguments.getOrDefault(Constants.INPUT_EDGE_FEATURE, "");
        String outputResults = arguments.getOrDefault(Constants.OUTPUT_RESULTS, "");
        System.out.println("========================inputEdgeFeature: " + inputEdgeFeature);

        int maxHop = Integer.parseInt(arguments.get(Constants.HOP));
        String sampleCond = arguments.get(Constants.SAMPLE_COND);
        String subgraphSpec = arguments.get(Constants.SUBGRAPH_SPEC);

        String otherOutputSchema = arguments.getOrDefault(Constants.OTHER_OUTPUT_SCHEMA, "");
        String indexMetasStr = arguments.getOrDefault(Constants.INDEX_METAS, "");
        String filterCond = arguments.getOrDefault(Constants.FILTER_COND, "");
        List<String> indexMetas = Arrays.asList(indexMetasStr.split(","));
        Filter filter = new Filter(filterCond);

        SubGraphSpecs subGraphSpecs = new SubGraphSpecs(subgraphSpec);
        Dataset<Row> seedDS = Utils.inputData(spark, inputLabel)
                .withColumn("time", col("time").cast(DataTypes.LongType))
                .cache();

        Dataset<Row> seedLabels = Utils.aggregateConcatWs("seed", seedDS);

        Dataset<Row> edgeDF = Utils.inputData(spark, inputEdge)
                .withColumn("time", col("time").cast(DataTypes.LongType)).cache();
        Dataset<Row> neighborDF = edgeDF
                .repartition(col("node1_id")).sortWithinPartitions("edge_id")
                .groupBy("node1_id").agg(
                        collect_list("node2_id").as("collected_node2_id"),
                        collect_list("edge_id").as("collected_edge_id"),
                        collect_list("time").as("collected_time")).cache();

        Dataset<IndexInfo> indexDS = neighborDF.map(new MapFunction<Row, IndexInfo>() {
            @Override
            public IndexInfo call(Row row) throws Exception {
                String node1_id = row.getString(0);
                List<String> node2_id = row.getList(1);
                List<String>  edge_id = row.getList(2);
                List<Long>  time = row.getList(3);
                IndexInfo indexInfo = new IndexInfo();
                indexInfo.setNode1_id(node1_id);
                indexInfo.setNode2_id(node2_id);
                indexInfo.setEdge_id(edge_id);
                indexInfo.setTime(time);
                HeteroDataset heteroDataset = new HeteroDataset(node2_id.size());
                heteroDataset.addAttributeList("time", time);
                Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, heteroDataset);
                IndexInfo.setAllIndexes(indexMap, indexInfo);
                return indexInfo;
            }
        }, Encoders.bean(IndexInfo.class));

        Dataset<SubGraphElement> resultGraphElement = seedDS.map((MapFunction<Row, SubGraphElement>) row -> {
            String nodeId = row.getString(0);
            String seed = row.getString(1);
            return new SubGraphElement(seed, Constants.ROOT, nodeId, nodeId, nodeId, "", "");
        }, Encoders.bean(SubGraphElement.class));
        Dataset<Row> seedAgg = seedDS.groupBy("node_id")
                .agg(collect_list("seed").as("collected_seed"),
                     collect_list("time").as("collected_time"));

        for (int hop = 0; hop < maxHop; hop++) {
            Dataset<SubGraphElement> seedPropagationResults = indexDS.joinWith(seedAgg, indexDS.col("node1_id").equalTo(seedAgg.col("node_id")), "inner")
                    .flatMap(getPropergateFunction(otherOutputSchema, filter, sampleCond, hop), Encoders.bean(SubGraphElement.class));
            resultGraphElement = resultGraphElement.union(seedPropagationResults);
            if (hop + 1 < maxHop) {
                Dataset<SubGraphElement> seedNodes = seedPropagationResults.filter("entryType = 'node'").distinct();
                seedAgg = seedNodes
                        .repartition(seedNodes.col("node1")).sortWithinPartitions("seed", "other1Long")
                        .groupBy("node1")
                        .agg(collect_list("seed").as("collected_seed"),
                             collect_list("other1Long").as("collect_list"))
                        .withColumnRenamed("node1", "node_id");
            }
        }

        resultGraphElement.distinct().cache();
        Dataset<SubGraphElement> nodeStructure = resultGraphElement.filter("entryType = 'node' or entryType = 'root'");
        Dataset<SubGraphElement> nodeFeatureDF = nodeStructure;
        if (inputNodeFeature.trim().length() > 0) {
            Dataset<Row> rawNodeFeatureDF = Utils.inputData(spark, inputNodeFeature);
            nodeFeatureDF = nodeFeatureDF
                    .join(rawNodeFeatureDF, nodeFeatureDF.col("id").equalTo(rawNodeFeatureDF.col("node_id")), "inner")
                    .select("seed", "entryType", "node1", "node2", "id", "node_feature")
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
                    .select("seed", "entryType", "node1", "node2", "id", "edge_feature")
                    .map((MapFunction<Row, SubGraphElement>) row -> new SubGraphElement(row.getString(0), row.getString(1), row.getString(2), row.getString(3), row.getString(4), row.getString(5), ""),
                            Encoders.bean(SubGraphElement.class));
        }
        Dataset<Row> subgraph = nodeFeatureDF.union(edgeFeatureDF).groupByKey((MapFunction<SubGraphElement, String>) row -> row.getSeed(), Encoders.STRING())
                .mapGroups(getGenerateGraphFeatureFunc(subGraphSpecs), Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("seed", "graph_feature");
        Dataset<Row> subgraphx = subgraph.join(seedLabels, "seed");
        Utils.outputData(spark, subgraphx, outputResults);
    }

    public static FlatMapFunction<Tuple2<IndexInfo, Row>, SubGraphElement> getPropergateFunction(String otherOutputSchema, Filter filter, String sampleCond, int hop) {
        return
                new FlatMapFunction<Tuple2<IndexInfo, Row>, SubGraphElement>() {
                    @Override
                    public Iterator<SubGraphElement> call(Tuple2<IndexInfo, Row> row) throws Exception {
                        // seeds:  node_id, seed, time
                        Row seedRow = row._2;
                        List<String> seedList = seedRow.getList(1);
                        List originTimeList = seedRow.getList(2);
                        // neighbors: node1_id, node2_ids, edge_ids
                        IndexInfo indexInfo = row._1;
                        List<String> node2IDs = indexInfo.getNode2_id();
                        List<String> edgeIDs = indexInfo.getEdge_id();
                        String node1_id = indexInfo.getNode1_id();
                        Map<String, BaseIndex> indexes = IndexInfo.getAllIndexes(indexInfo);

                        HeteroDataset seedAttrs = new HeteroDataset(seedList.size());
                        seedAttrs.addAttributeList("time", originTimeList);
                        HeteroDataset neighborAttrs = new HeteroDataset(node2IDs.size());
                        neighborAttrs.addAttributeList("time", indexInfo.getTime());

                        PropagateSeed propagateSeed = new PropagateSeed(otherOutputSchema, filter, sampleCond);
                        SampleOtherOutput sampleOtherOutput = propagateSeed.process(indexes, neighborAttrs, seedList, seedAttrs, null, null);
                        List<SubGraphElement> ans = new ArrayList<>();
                        for (int seedIdx = 0; seedIdx < seedList.size(); seedIdx++) {
                            String seed = seedList.get(seedIdx);
                            List<Integer> sampledNeighborIndex = sampleOtherOutput.getSampledNeighbors(seedIdx);
                            Object[][] nodeOtherOutputs = sampleOtherOutput.getNodeOtherOutputs(seedIdx);
                            Object[][] edgeOtherOutputs = sampleOtherOutput.getEdgeOtherOutputs(seedIdx);
                            int nodeOutputWidth = sampleOtherOutput.getNodeOtherOutputLen();
                            int edgeOutputWidth = sampleOtherOutput.getEdgeOtherOutputLen();
                            for (int i = 0; i < sampledNeighborIndex.size(); i++) {
                                int neighborIdx = sampledNeighborIndex.get(i);
                                String node2ID = node2IDs.get(neighborIdx);
                                String edgeID = edgeIDs.get(neighborIdx);
                                SubGraphElement subGraphNodeElement = new SubGraphElement(seed, "node", node2ID, node2ID, node2ID, nodeOutputWidth > 0 ? (long)(nodeOtherOutputs[i][0]) : null, nodeOutputWidth > 1 ? (Long)(nodeOtherOutputs[i][1]) : null);
                                ans.add(subGraphNodeElement);
                                SubGraphElement subGraphEdgeElement = new SubGraphElement(seed, "edge", node1_id, node2ID, edgeID, edgeOutputWidth > 0 ? (long)(edgeOtherOutputs[i][0]) : null, edgeOutputWidth > 1 ? (Long)(edgeOtherOutputs[i][1]) : null);
                                ans.add(subGraphEdgeElement);
                            }
                        }
                        return ans.iterator();
                    }
                };
    }

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
                Collections.sort(rootIds);
                return new Tuple2<>(seed, graphFeatureGenerator.getGraphFeature(rootIds));
            }
        };
    }
}
