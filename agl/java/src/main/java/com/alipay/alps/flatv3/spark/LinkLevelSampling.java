package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.spark.utils.Constants;
import com.alipay.alps.flatv3.spark.utils.DatasetUtils;
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

import java.util.Arrays;

import static org.apache.spark.sql.functions.col;

public class LinkLevelSampling extends NodeLevelSampling {
    private static final Logger LOG = LoggerFactory.getLogger(LinkLevelSampling.class);

    public LinkLevelSampling(String edgeTable, String labelTable, String outputTablePrefix, String subGraphSpec, int maxHop, String sampleCondition) throws Exception {
        super(edgeTable, labelTable, outputTablePrefix, subGraphSpec, maxHop, sampleCondition);
    }

    @Override
    public Dataset<Row> modifySubGraphStructure(Dataset<Row> linkNodeDS, Dataset<Row> graphElementMultiLayer[]) {
        return super.modifySubGraphStructure(linkNodeDS, graphElementMultiLayer);
    }

    @Override
    public void runSamplingPipeline(SparkSession spark, Dataset<Row> linkDS, Dataset<Row> edgeDS, Dataset<Row> rawNodeFeatureDF) {
        edgeDS = attrsCastDType(edgeDS, getSubGraphSpec().getEdgeAttrs());
        sparkSampling.setEdgeColumnIndex(DatasetUtils.getColumnIndex(edgeDS));
        Dataset<Row> edgeRemoveFeatureDS = edgeDS.drop("edge_feature");
        edgeRemoveFeatureDS.show();
        edgeRemoveFeatureDS.printSchema();

        linkDS = attrsCastDType(linkDS, getSubGraphSpec().getSeedAttrs());
        linkDS.show();
        linkDS.printSchema();
        Dataset<Row> node1DS = linkDS.withColumnRenamed("node1_id", "node_id").select("node_id");
        Dataset<Row> node2DS = linkDS.withColumnRenamed("node2_id", "node_id").select("node_id");
        Dataset<Row> seedDS = node1DS.union(node2DS).withColumn(Constants.ENTRY_SEED, col("node_id")).select("node_id", Constants.ENTRY_SEED).distinct();

        Dataset<Row> graphElementMultiLayer[] = propagateSubGraphStructure(seedDS, edgeRemoveFeatureDS);

        Dataset<Row> linkNode1DS = linkDS.withColumnRenamed("node1_id", "node_id").select("node_id", "seed");
        Dataset<Row> linkNode2DS = linkDS.withColumnRenamed("node2_id", "node_id").select("node_id", "seed");
        Dataset<Row> linkNodeDS = linkNode1DS.union(linkNode2DS);
        linkNodeDS.show();
        linkNodeDS.printSchema();
        Dataset<Row> linkSubgraphDS = modifySubGraphStructure(linkNodeDS, graphElementMultiLayer);
        linkSubgraphDS.show();
        linkSubgraphDS.printSchema();

        Dataset<Row> linkSubgraph = buildSubgraphWithFeature(linkSubgraphDS, rawNodeFeatureDF, edgeDS);

        linkDS = linkDS.withColumnRenamed("seed", "link");
        linkDS = linkDS.join(linkSubgraph, linkDS.col("node1_id").equalTo(linkSubgraph.col(Constants.ENTRY_SEED)))
                .select("node1_id", "node2_id", "link", "label", "train_flag", "graph_feature");
        linkSubgraph = linkSubgraph.withColumnRenamed("graph_feature", "graph_feature_2");
        Dataset<Row> linkSubgraphWithLabel = linkDS.join(linkSubgraph, linkDS.col("node2_id").equalTo(linkSubgraph.col(Constants.ENTRY_SEED)))
                .select("node1_id", "node2_id", "link", "label", "train_flag", "graph_feature", "graph_feature_2");

        sinkSubgraphWithLabel(spark, linkSubgraphWithLabel);
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
                .addOption(Option.builder(Constants.REMOVE_EDGE_AMONG_ROOTS).hasArg().build());

        CommandLineParser parser = new DefaultParser();
        try {
            LOG.info("========================arguments: " + Arrays.toString(args));
            CommandLine arguments = parser.parse(options, args);

            LinkLevelSampling gnn = new LinkLevelSampling(arguments.getOptionValue(Constants.INPUT_EDGE), arguments.getOptionValue(Constants.INPUT_LABEL),
                    arguments.getOptionValue(Constants.OUTPUT_RESULTS), arguments.getOptionValue(Constants.SUBGRAPH_SPEC),
                    Integer.parseInt(arguments.getOptionValue(Constants.HOP)), arguments.getOptionValue(Constants.SAMPLE_COND));
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
            if (!Boolean.parseBoolean(arguments.getOptionValue(Constants.STORE_IDS, Constants.STORE_IDS_DEFAULT))) {
                gnn.notStoreID();
            }
            if (arguments.hasOption(Constants.REMOVE_EDGE_AMONG_ROOTS)) {
                gnn.setRemoveEdgeAmongRoots(Boolean.parseBoolean(arguments.getOptionValue(Constants.REMOVE_EDGE_AMONG_ROOTS)));
            }
            gnn.setup();

            Dataset<Row> linkDS = DatasetUtils.inputData(spark, gnn.getLabelTable());
            Dataset<Row> edgeDS = DatasetUtils.inputData(spark, gnn.getEdgeTable());
            Dataset<Row> rawNodeFeatureDF = DatasetUtils.inputData(spark, gnn.getNodeFeatureTable());

            gnn.runSamplingPipeline(spark, linkDS, edgeDS, rawNodeFeatureDF);
        } catch (ParseException e) {
            LOG.error("Create Parser Failed", e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(NodeLevelSampling.class.getName(), options, true);
        }
    }
}
