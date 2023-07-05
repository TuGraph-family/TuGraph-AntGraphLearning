package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.graphfeature.GraphFeatureGenerator;
import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.neighbor_selection.PropagateSeed;
import com.alipay.alps.flatv3.neighbor_selection.SampleOtherOutput;
import com.antfin.agl.proto.sampler.VariableSource;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;
import scala.Tuple4;
import scala.collection.JavaConverters;
import scala.collection.mutable.WrappedArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.spark.sql.api.java.UDF1;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;
import static org.apache.spark.sql.functions.concat_ws;

public class SubGraph {
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
        Map<String, Integer> seedColumnIndex = Utils.getColumnIndex(seedDS);


        Dataset<Row> seedLabels = Utils.aggregateConcatWs("seed", seedDS);

        Dataset<Row> edgeDS = Utils.inputData(spark, inputEdge)
                .withColumn("time", col("time").cast(DataTypes.LongType)).cache();
        String edgeColumnNames[] = edgeDS.columns();
        List<Column> aggColumns = new ArrayList<>();
        for (int i = 1; i < edgeColumnNames.length; i++) {
            aggColumns.add(collect_list(col(edgeColumnNames[i])).as(edgeColumnNames[i]));
        }
        Dataset<Row> neighborDS = edgeDS
                .repartition(col("node1_id")).sortWithinPartitions("edge_id")
                .groupBy("node1_id")
                .agg(aggColumns.get(0), aggColumns.subList(1, aggColumns.size()).toArray(new org.apache.spark.sql.Column[0]));

        neighborDS.show();
        neighborDS.printSchema();
        for (String indexMeta : indexMetas) {
            String indexColumn = indexMeta.split(":")[1];
            neighborDS = neighborDS
                    .withColumn(indexColumn + "_index", functions.udf((WrappedArray aggValues) -> {
                        List valueList = (List) JavaConverters.seqAsJavaListConverter(aggValues).asJava();
                        HeteroDataset heteroDataset = new HeteroDataset(aggValues.size());
                        heteroDataset.addAttributeList(indexColumn, valueList);
                        BaseIndex indexMap = new IndexFactory().createIndex(indexMeta, heteroDataset);
                        return indexMap.dump();
                    }, DataTypes.BinaryType).apply(col(indexColumn)));

            neighborDS.show();
            neighborDS.printSchema();
        }
        Map<String, Integer> neighborColumnIndex = Utils.getColumnIndex(neighborDS);

        Dataset<SubGraphElement> resultGraphElement = seedDS.map((MapFunction<Row, SubGraphElement>) row -> {
            String nodeId = row.getString(0);
            String seed = row.getString(1);
            return new SubGraphElement(seed, Constants.ROOT, nodeId, nodeId, nodeId, "", "");
        }, Encoders.bean(SubGraphElement.class));
        Dataset<Row> seedAgg = seedDS.groupBy("node_id")
                .agg(collect_list("seed").as("collected_seed"),
                        collect_list("time").as("collected_time"));

        for (int hop = 0; hop < maxHop; hop++) {
            Dataset<SubGraphElement> seedPropagationResults = neighborDS.joinWith(seedAgg, neighborDS.col("node1_id").equalTo(seedAgg.col("node_id")), "inner")
                    .flatMap(getPropergateFunction(otherOutputSchema, filter, sampleCond, seedColumnIndex, neighborColumnIndex, hop, indexMetas.get(0)),
                            getRowEncoder(otherOutputSchema));
            resultGraphElement = resultGraphElement.union(seedPropagationResults);
            if (hop + 1 < maxHop) {
                Dataset<SubGraphElement> seedNodes = seedPropagationResults.filter("entryType = 'node'").distinct();
                seedAgg = seedNodes
                        .repartition(seedNodes.col("node1")).sortWithinPartitions("seed", "other1Long")
                        .groupBy("node1")
                        .agg(collect_list("seed").as("seed"),
                                collect_list("other1Long").as("other1Long"))
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

    // seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, edgeOtherOutputs[i][0]
    public static ExpressionEncoder getRowEncoder(String otherOutputSchema) {
        return RowEncoder.apply(new StructType()
                .add("seed", DataTypes.StringType)
                .add("entryType", DataTypes.IntegerType)
                .add("node1", DataTypes.LongType)
                .add("node2", DataTypes.LongType)
                .add("id", DataTypes.LongType)
                .add("feature", DataTypes.StringType)
                .add("time", DataTypes.LongType));
    };

    public static FlatMapFunction<Tuple2<Row, Row>, Row> getPropergateFunction(String otherOutputSchema, Filter filter, String sampleCond,
                                                                               Map<String, Integer> seedColumnIndex, Map<String, Integer> neighborColumnIndex,
                                                                               int hop, String indexMeta) {
        return
                new FlatMapFunction<Tuple2<Row, Row>, Row>() {
                    @Override
                    public Iterator<Row> call(Tuple2<Row, Row> row) throws Exception {
                        // neighbors: node1_id, node2_ids, edge_ids
                        Row indexInfo = row._1;
                        String node1ID = indexInfo.getString(0);
                        List<String> node2IDs = indexInfo.getList(1);
                        List<String> edgeIDs = indexInfo.getList(2);

                        // seeds:  node_id, seed
                        Row seedRow = row._2;
                        List<String> seedList = seedRow.getList(1);

                        Map<VariableSource, Set<String>> referColumns = filter.getReferColumns();
                        HeteroDataset seedAttrs = new HeteroDataset(seedList.size());
                        if (referColumns.containsKey(VariableSource.SEED)) {
                            for (String column : referColumns.get(VariableSource.SEED)) {
                                seedAttrs.addAttributeList(column, seedRow.getList(seedColumnIndex.get(column)));
                            }
                        }

                        HeteroDataset neighborAttrs = new HeteroDataset(node2IDs.size());
                        Map<String, BaseIndex> indexes = new HashMap<>();
                        if (referColumns.containsKey(VariableSource.INDEX)) {
                            for (String column : referColumns.get(VariableSource.INDEX)) {
                                neighborAttrs.addAttributeList(column, indexInfo.getList(neighborColumnIndex.get(column)));
                                String indexColumn = column + "_index";
                                if (neighborColumnIndex.containsKey(indexColumn)) {
                                    BaseIndex index = new IndexFactory().loadIndex(indexMeta, indexInfo.getAs(neighborColumnIndex.get(indexColumn)));
                                    indexes.put(index.getIndexColumn(), index);
                                }
                            }
                        }

                        PropagateSeed propagateSeed = new PropagateSeed(otherOutputSchema, filter, sampleCond);
                        SampleOtherOutput sampleOtherOutput = propagateSeed.process(indexes, neighborAttrs, seedList, seedAttrs, null, null);
                        List<Row> ans = new ArrayList<>();
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
                                if (nodeOutputWidth == 0) {
                                    Row subGraphNodeElement = RowFactory.create(seed, Constants.NODE_INT, null, null, node2ID, null);
                                    ans.add(subGraphNodeElement);
                                } else if (nodeOutputWidth == 1) {
                                    Row subGraphNodeElement = RowFactory.create(seed, Constants.NODE_INT, null, null, node2ID, null, nodeOtherOutputs[i][0]);
                                    ans.add(subGraphNodeElement);
                                } else if (nodeOutputWidth == 2) {
                                    Row subGraphNodeElement = RowFactory.create(seed, Constants.NODE_INT, null, null, node2ID, null, nodeOtherOutputs[i][0], nodeOtherOutputs[i][1]);
                                    ans.add(subGraphNodeElement);
                                }
                                if (edgeOutputWidth == 0) {
                                    Row subGraphEdgeElement = RowFactory.create(seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null);
                                    ans.add(subGraphEdgeElement);
                                } else if (edgeOutputWidth == 1) {
                                    Row subGraphEdgeElement = RowFactory.create(seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, edgeOtherOutputs[i][0]);
                                    ans.add(subGraphEdgeElement);
                                } else if (edgeOutputWidth == 2) {
                                    Row subGraphEdgeElement = RowFactory.create(seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, edgeOtherOutputs[i][0], edgeOtherOutputs[i][1]);
                                    ans.add(subGraphEdgeElement);
                                }
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

