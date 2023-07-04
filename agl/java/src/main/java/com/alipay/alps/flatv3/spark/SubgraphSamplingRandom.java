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
import org.apache.spark.storage.StorageLevel;
import scala.Int;
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
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

public class SubgraphSamplingRandom {

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
        System.out.println("--------sampleCond:" + Arrays.toString(sampleCond));
        String subgraphSpec = arguments.get(Constants.SUBGRAPH_SPEC);

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
//                .repartition(col("seed")).sortWithinPartitions("node_id");
        Dataset<Row> seedLabels = Utils.aggregateConcatWs("seed", seedDS);
//        seedDS.show();
//        seedDS.printSchema();

        Dataset<Row> neighborDF = Utils.inputData(spark, inputEdge)
//                .repartition(col("node1_id")).sortWithinPartitions("node2_id")
                .groupBy("node1_id").agg(
                        collect_list("node2_id").as("collected_node2_id"),
                        collect_list("edge_id").as("collected_edge_id")).cache();
        neighborDF.show();
        neighborDF.printSchema();

        Dataset<Row> resultGraphElement = seedDS.map((MapFunction<Row, Row>) row -> {
            long nodeId = row.getLong(0);
            long seed = row.getLong(1);
            return RowFactory.create(seed, Constants.ROOT_INT, nodeId, nodeId, nodeId, null);
        }, RowEncoder.apply(new StructType()
                .add("seed", DataTypes.LongType)
                .add("entryType", DataTypes.IntegerType)
                .add("node1", DataTypes.LongType)
                .add("node2", DataTypes.LongType)
                .add("id", DataTypes.LongType)
                .add("feature", DataTypes.StringType)));
        ;
        Dataset<Row> seedAgg = seedDS.groupBy("node_id")
                .agg(collect_list("seed").as("collected_seed"));

        for (int hop = 0; hop < maxHop; hop++) {
            Dataset<Row> seedPropagationResults = neighborDF.joinWith(seedAgg, neighborDF.col("node1_id").equalTo(seedAgg.col("node_id")), "inner")
                    .flatMap(getPropergateFunction(otherOutputSchema, filter, sampleCond[hop], hop), RowEncoder.apply(new StructType()
                            .add("seed", DataTypes.LongType)
                            .add("entryType", DataTypes.IntegerType)
                            .add("node1", DataTypes.LongType)
                            .add("node2", DataTypes.LongType)
                            .add("id", DataTypes.LongType)
                            .add("feature", DataTypes.StringType)));
//            seedPropagationResults.show();
//            seedPropagationResults.printSchema();
            resultGraphElement = resultGraphElement.union(seedPropagationResults);
            if (hop + 1 < maxHop) {
                Dataset<Row> seedNodes = seedPropagationResults.filter("entryType = 0").distinct();
                seedAgg = seedNodes
                        .repartition(seedNodes.col("id")).sortWithinPartitions("seed")
                        .groupBy("id")
                        .agg(collect_list("seed").as("collected_seed"))
                        .withColumnRenamed("id", "node_id");
//                seedAgg.show();
//                seedAgg.printSchema();
            }
        }

        resultGraphElement = resultGraphElement.distinct().persist(StorageLevel.DISK_ONLY());
        Dataset<Row> nodeStructure = resultGraphElement.filter("entryType = 0 or entryType = 2");

//        Dataset<Row> seedCounts = nodeStructure.groupBy("id").count();
//        Utils.outputData(spark, seedCounts, outputResults);

        Dataset<Row> nodeFeatureDF = nodeStructure;
        if (inputNodeFeature.trim().length() > 0) {
            Dataset<Row> rawNodeFeatureDF = Utils.inputData(spark, inputNodeFeature);
            nodeFeatureDF = rawNodeFeatureDF
                    .join(nodeStructure, rawNodeFeatureDF.col("node_id").equalTo(nodeStructure.col("node1")), "inner")
                    .select("seed", "entryType", "node1", "node2", "node_id", "node_feature")
                    .map((MapFunction<Row, Row>) (Row row) -> {
                                Long seed = row.getLong(0);
                                Integer entryType = row.getInt(1);
//                                Long node1 = row.getLong(2);
//                                Long node2 = row.getLong(3);
                                Long id = row.getLong(4);
                                return RowFactory.create(seed, entryType, null, null, id, row.getString(5));
//                                return RowFactory.create(row.getLong(0), row.getInt(1), row.getLong(2), row.getLong(3), row.getLong(4), row.getString(5));
                            },
                            RowEncoder.apply(new StructType()
                                    .add("seed", DataTypes.LongType)
                                    .add("entryType", DataTypes.IntegerType)
                                    .add("node1", DataTypes.LongType)
                                    .add("node2", DataTypes.LongType)
                                    .add("id", DataTypes.LongType)
                                    .add("feature", DataTypes.StringType)));
        }

        Dataset<Row> edgeStructure = resultGraphElement.filter("entryType = 1");
        Dataset<Row> edgeFeatureDF = edgeStructure;
        if (inputEdgeFeature.trim().length() > 0) {
            Dataset<Row> rawEdgeFeatureDF = Utils.inputData(spark, inputEdgeFeature);
            edgeFeatureDF = rawEdgeFeatureDF.join(edgeStructure, edgeStructure.col("eid").equalTo(rawEdgeFeatureDF.col("eid")), "inner")
                    .select("seed", "entryType", "node1", "node2", "eid", "edge_feature")
                    .map((MapFunction<Row, Row>) row -> RowFactory.create(row.getLong(0), row.getInt(1), row.getLong(2), row.getLong(3), row.getLong(4), row.getString(5)),
                            RowEncoder.apply(new StructType()
                                    .add("seed", DataTypes.LongType)
                                    .add("entryType", DataTypes.IntegerType)
                                    .add("node1", DataTypes.LongType)
                                    .add("node2", DataTypes.LongType)
                                    .add("id", DataTypes.LongType)
                                    .add("feature", DataTypes.StringType))); // 5
        }

        // group node features and edge features by seed
        Dataset<Row> subgraph = nodeFeatureDF.union(edgeFeatureDF).groupByKey((MapFunction<Row, Long>) row -> row.getLong(0), Encoders.LONG())
                .mapGroups(getGenerateGraphFeatureFunc(subGraphSpecs), Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("seed", "graph_feature");

//        subgraph.show();
//        subgraph.printSchema();
        Dataset<Row> subgraphLabels = subgraph.join(seedLabels, "seed");
        Utils.outputData(spark, subgraphLabels, outputResults);
        resultGraphElement.unpersist();
    }

    public static FlatMapFunction<Tuple2<Row, Row>, Row> getPropergateFunction(String otherOutputSchema, Filter filter, String sampleCond, int hop) {
        return
                new FlatMapFunction<Tuple2<Row, Row>, Row>() {
                    @Override
                    public Iterator<Row> call(Tuple2<Row, Row> row) throws Exception {
                        // seeds:  node_id, seed
                        Row seedRow = row._2;
                        List<Long> seedList = seedRow.getList(1);
//                        System.out.println("------hop:"+hop+" seedList:" + Arrays.toString(seedList.toArray()));
                        // neighbors: node1_id, node2_ids, edge_ids
                        Row indexInfo = row._1;
                        Long node1ID = indexInfo.getLong(0);
//                        List<Long> node2IDs = indexInfo.getList(1);
//                        List<Long> edgeIDs = indexInfo.getList(2);
                        List<Long> node2IDs = new ArrayList<>(indexInfo.getList(1));
                        List<Long> edgeIDs = new ArrayList<>(indexInfo.getList(2));
                        System.out.println("------before sort node1ID:"+node1ID+" edgeIDs:"+Arrays.toString(edgeIDs.toArray())+" node2IDs:" + Arrays.toString(node2IDs.toArray()));
                        int []originIndices = new int[node2IDs.size()];
                        for (int i = 0; i < node2IDs.size(); i++) {
                            originIndices[i] = i;
                        }
                        quicksort(originIndices, edgeIDs, 0, edgeIDs.size() - 1);
                        ArrayList<Integer> shuffleIndex = new ArrayList<Integer>(Collections.nCopies(originIndices.length, 0));
                        int candidateCount = originIndices.length;
                        for (int i = 0; i < candidateCount; i++) {
                            shuffleIndex.set(originIndices[i], i);
                        }
                        for (int i = 0; i < candidateCount; i++) {
                            while (shuffleIndex.get(i) != i) {
//                                Collections.swap(edgeIDs, i, shuffleIndex.get(i));
                                Collections.swap(node2IDs, i, shuffleIndex.get(i));
                                Collections.swap(shuffleIndex, i, shuffleIndex.get(i));
                            }
                        }
                        System.out.println("------node1ID:"+node1ID+" edgeIDs:"+Arrays.toString(edgeIDs.toArray())+" node2IDs:" + Arrays.toString(node2IDs.toArray()));

//                        List<RangeUnit> sortedIntervals = new ArrayList<>();
//                        sortedIntervals.add(new RangeUnit(0, node2IDs.size()-1));
                        List<Row> ans = new ArrayList<>();
                        SampleCondition sampleCondition = new SampleCondition(sampleCond);
                        for (int seedIdx = 0; seedIdx < seedList.size(); seedIdx++) {
                            Long seed = seedList.get(seedIdx);
//                            sampleCondition.setSeed(1);
//                            AbstractSampler sampler = SamplerFactory.createSampler(sampleCondition, null);
//                            List<Integer> neighborIndices = sampler.sample(new RangeResult(null, sortedIntervals));
                            List<Integer> neighborIndices = new ArrayList<>();
                            for (int k = 0; k < sampleCondition.getLimit() && k < node2IDs.size(); k++) {
                                neighborIndices.add(k);
                            }
                            for (int i : neighborIndices) {
                                Long node2ID = node2IDs.get(i);
                                Long edgeID = edgeIDs.get(i);
                                Row subGraphNodeElement = RowFactory.create(seed, Constants.NODE_INT, null, null, node2ID, null);
                                ans.add(subGraphNodeElement);
                                Row subGraphEdgeElement = RowFactory.create(seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null);
                                ans.add(subGraphEdgeElement);
                            }
                        }
                        return ans.iterator();
                    }
                };
    }

    public static MapGroupsFunction<Long, Row, Tuple2<String, String>> getGenerateGraphFeatureFunc(SubGraphSpecs subGraphSpecs) {
        return new MapGroupsFunction<Long, Row, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> call(Long key, Iterator<Row> values) throws Exception {
                Long seed = key;
                GraphFeatureGenerator graphFeatureGenerator = new GraphFeatureGenerator(subGraphSpecs);
                graphFeatureGenerator.init();
                List<String> rootIds = new ArrayList<>();
                List<Tuple4<String, String, String, String>> edgeFeatures = new ArrayList<>();
                while (values.hasNext()) {
                    Row row = values.next();
                    if (row.getInt(1) == Constants.ROOT_INT) {
                        rootIds.add(String.valueOf(row.getLong(4)));
                        graphFeatureGenerator.addNodeInfo(String.valueOf(row.getLong(4)), "default", row.getString(5) == null ? "" : row.getString(5));
                    } else if (row.getInt(1) == Constants.NODE_INT) {
                        graphFeatureGenerator.addNodeInfo(String.valueOf(row.getLong(4)), "default", row.getString(5) == null ? "" : row.getString(5));
                    } else if (row.getInt(1) == Constants.EDGE_INT) {
                        edgeFeatures.add(new Tuple4<>(String.valueOf(row.getLong(2)), String.valueOf(row.getLong(3)), String.valueOf(row.getLong(4)), row.getString(5)));
                    } else {
                        throw new Exception("invalid entry type: " + row.getInt(1));
                    }
                }
                try {
                    for (Tuple4<String, String, String, String> edgeFeature : edgeFeatures) {
                        graphFeatureGenerator.addEdgeInfo(edgeFeature._1(), edgeFeature._2(), edgeFeature._3(), "default", edgeFeature._4() == null ? "1:0" : edgeFeature._4());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("========failed for seed:" + seed + " nodeIndices:" + Arrays.toString(graphFeatureGenerator.nodeIndices.entrySet().toArray())
                            + "\nedgeIndices:" + Arrays.toString(graphFeatureGenerator.edgeIndices.entrySet().toArray())
                            + "\nnode2IDs:" + Arrays.toString(graphFeatureGenerator.node1Edges.toArray()), e);
                }
                Collections.sort(rootIds);
                return new Tuple2<>(String.valueOf(seed), graphFeatureGenerator.getGraphFeature(rootIds));
            }
        };
    }

    public static void quicksort(int[] indices, List<Long> weights, int left, int right) {
        if (left < right) {
            int pivotIndex = partition(indices, weights, left, right);
            quicksort(indices, weights, left, pivotIndex - 1);
            quicksort(indices, weights, pivotIndex + 1, right);
        }
    }

    private static int partition(int[] indices, List<Long> weights, int left, int right) {
        Long pivotWeight = weights.get(right);
        int i = left - 1;
        for (int j = left; j < right; j++) {
            if (weights.get(j).compareTo(pivotWeight) < 0) {
                i++;
                swap(indices, i, j);
                Collections.swap(weights, i, j);
            }
        }
        swap(indices, i + 1, right);
        Collections.swap(weights, i + 1, right);
        return i + 1;
    }

    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    // 1578727, 1578728, 1578734, 1578730, 1578731, 1578732, 1578729, 1578733
    // 53750, 54684, 54849, 55077, 55874, 56622, 56857, 56915
}
