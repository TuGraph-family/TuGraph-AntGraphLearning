package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.graphfeature.GraphFeatureGenerator;
import com.alipay.alps.flatv3.graphfeature.SubGraphSpecs;
import com.alipay.alps.flatv3.spark.utils.Constants;
import com.alipay.alps.flatv3.spark.utils.DatasetUtils;
import com.alipay.alps.flatv3.spark.utils.FeaturesSum;
import com.alipay.alps.flatv3.spark.utils.FeaturesWeight;
import com.antfin.agl.proto.graph_feature.Features;
import com.google.common.io.BaseEncoding;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.spark.api.java.function.MapFunction;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.spark.sql.functions.callUDF;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.udaf;

public class SGC extends NodeLevelSampling {
    private static final Logger LOG = LoggerFactory.getLogger(SGC.class);

    public SGC(String edgeTable, String labelTable, String outputTablePrefix, String subGraphSpec, int maxHop, String sampleCondition) throws Exception {
        super(edgeTable, labelTable, outputTablePrefix, subGraphSpec, maxHop, sampleCondition);
    }

    public void runSamplingPipeline(SparkSession spark, Dataset<Row> seedDS, Dataset<Row> edgeDS, Dataset<Row> nodeDS) {
        SubGraphSpecs subGraphSpecs = getSubGraphSpec();
        nodeDS = nodeDS.map((MapFunction<Row, Row>) row -> {
            String n1 = row.getString(0);
            String nodeFeature = row.getString(1);
            return RowFactory.create(n1, subGraphSpecs.generateNodeFeaturesBase64(nodeFeature, "default"));
        }, RowEncoder.apply(new StructType()
                .add("node_id", DataTypes.StringType)
                .add("feature", DataTypes.StringType)));

        edgeDS = attrsCastDType(edgeDS, subGraphSpecs.getEdgeAttrs());
        Dataset<Row> edgeRemoveFeatureDS = edgeDS.drop("edge_feature");

        FeaturesSum featuresSum = new FeaturesSum();
        spark.udf().register("featuresSum", udaf(featuresSum, Encoders.bean(FeaturesWeight.class)));

        Dataset<Row> allNodeasSeedDS = nodeDS.withColumn("seed", col("node_id"));
        Dataset<Row> seedAgg = aggSeedData(allNodeasSeedDS, subGraphSpecs.getSeedAttrs());


        Dataset<Row> neighborDS = aggNeighborData(edgeDS);

        ExpressionEncoder expressionEncoderWithOtherOutput = sparkSampling.getRowEncoder(null);

        for (int hop = 1; hop <= getMaxHop(); hop++) {
            Dataset<Row> chosenEdges = propagateOneHop(seedAgg, neighborDS, expressionEncoderWithOtherOutput, hop).filter(Constants.ENTRY_TYPE + " = " + Constants.EDGE_INT);
            Dataset<Row> chosenEdgesWeight = chosenEdges.join(edgeDS, chosenEdges.col("id").equalTo(edgeDS.col("edge_id")), "inner")
                    .select(edgeDS.col("*"))
                    .withColumn("weight", functions.exp(col("weight")));

            Dataset<Row> isolatedNodeFeatures = nodeDS.join(chosenEdgesWeight, nodeDS.col("node_id").equalTo(chosenEdgesWeight.col("node1_id")), "leftanti");

            nodeDS = nodeDS.join(chosenEdgesWeight, nodeDS.col("node_id").equalTo(chosenEdgesWeight.col("node2_id")), "inner")
                    .select(col("node1_id").as("node_id"), col("feature"), col("weight"))
                    .groupBy("node_id")
                    .agg(callUDF("featuresSum", col("feature"), col("weight")).as("feature")
                            , functions.sum("weight").as("total_weight"))
                    .map(generateMapFunc0(), RowEncoder.apply(new StructType()
                            .add("node_id", DataTypes.StringType)
                            .add("feature", DataTypes.StringType)))
                    .union(isolatedNodeFeatures);
        }

        Dataset<Row> subgraph = nodeDS.map(generateMapFunc(subGraphSpecs)
                , Encoders.tuple(Encoders.STRING(), Encoders.STRING())).toDF("node_id", "subgraph");

        if (seedDS != null) {
            subgraph = subgraph.join(seedDS, Constants.ENTRY_SEED);
        }

        sinkSubgraphWithLabel(spark, subgraph);
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
                .addOption(Option.builder(Constants.FILTER_COND).hasArg().build());

        CommandLineParser parser = new DefaultParser();
        try {
            LOG.info("========================arguments: " + Arrays.toString(args));
            CommandLine arguments = parser.parse(options, args);

            SGC sgc = new SGC(arguments.getOptionValue(Constants.INPUT_EDGE), arguments.getOptionValue(Constants.INPUT_LABEL),
                    arguments.getOptionValue(Constants.OUTPUT_RESULTS), arguments.getOptionValue(Constants.SUBGRAPH_SPEC),
                    Integer.parseInt(arguments.getOptionValue(Constants.HOP)), arguments.getOptionValue(Constants.SAMPLE_COND));
            if (arguments.hasOption(Constants.INPUT_NODE_FEATURE)) {
                sgc.setNodeTable(arguments.getOptionValue(Constants.INPUT_NODE_FEATURE));
            }
            if (arguments.hasOption(Constants.INDEX_METAS)) {
                sgc.setIndexMetas(arguments.getOptionValue(Constants.INDEX_METAS));
            }
            if (arguments.hasOption(Constants.TRAIN_FLAG)) {
                sgc.setTrainFlag(arguments.getOptionValue(Constants.TRAIN_FLAG));
            }
            if (arguments.hasOption(Constants.FILTER_COND)) {
                sgc.setFilterCondition(arguments.getOptionValue(Constants.FILTER_COND));
            }
            sgc.setup();

            Dataset<Row> nodeDS = DatasetUtils.inputData(spark, sgc.getNodeFeatureTable());
            Dataset<Row> seedDS = null;
            Dataset<Row> edgeDS = DatasetUtils.inputData(spark, sgc.getEdgeTable());

            sgc.runSamplingPipeline(spark, seedDS, edgeDS, nodeDS);
        } catch (ParseException e) {
            LOG.error("Create Parser Failed", e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(NodeLevelSampling.class.getName(), options, true);
        }
    }

    public static MapFunction<Row, Row> generateMapFunc0() {
        return new MapFunction<Row, Row>() {
            @Override
            public Row call(Row row) throws Exception {
                String n1 = row.getString(0);
                String nodeFeature = row.getString(1);
                float totalWeight = (float) row.getDouble(2);
                Features features = FeaturesSum.updateFeature(Features.parseFrom(BaseEncoding.base64().decode(nodeFeature)), 1.0F / totalWeight);
                return RowFactory.create(n1, BaseEncoding.base64().encode(features.toByteArray()));
            }
        };
    }


    public static MapFunction<Row, Tuple2<String, String>> generateMapFunc(SubGraphSpecs subGraphSpecs) {
        return new MapFunction<Row, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> call(Row row) throws Exception {
                String n1 = row.getString(0);
                String nodeFeature = row.getString(1);

                GraphFeatureGenerator graphFeatureGenerator = new GraphFeatureGenerator(subGraphSpecs);
                graphFeatureGenerator.init();

                List<String> rootIds = new ArrayList<>();
                rootIds.add(n1);
                graphFeatureGenerator.addNodeFeaturesPB(n1, "default", Features.parseFrom(BaseEncoding.base64().decode(nodeFeature)));

                return new Tuple2<>(n1, graphFeatureGenerator.getGraphFeature(rootIds, false, true));
            }
        };
    }
}
