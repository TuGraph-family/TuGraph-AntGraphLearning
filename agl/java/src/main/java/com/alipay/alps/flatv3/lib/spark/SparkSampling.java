package com.alipay.alps.flatv3.lib.spark;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.lib.GNNSamplingLib;
import com.alipay.alps.flatv3.lib.WriteSubGraphElement;
import com.alipay.alps.flatv3.neighbor_selection.OtherOutput;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.spark.utils.Constants;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.collection.mutable.WrappedArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.spark.sql.functions.col;

public class SparkSampling implements WriteSubGraphElement, Serializable {
    private Map<String, Integer> edgeColumnIndex;
    private Map<String, Integer> neighborColumnIndex;
    private Map<String, Integer> seedColumnIndex;
    private Map<String, Integer> seedOriginColumnIndex;
    private GNNSamplingLib gnnSamplingLib;
    private List<Row> subGraphElements = new ArrayList<>();

    public SparkSampling(GNNSamplingLib gnnSamplingLib) {
        this.gnnSamplingLib = gnnSamplingLib;
    }

    public void setup() throws Exception {
        gnnSamplingLib.setup();
    }

    public GNNSamplingLib getGNNSamplingLib() {
        return gnnSamplingLib;
    }

    public void setEdgeColumnIndex(Map<String, Integer> edgeColumnIndex) {
        this.edgeColumnIndex = edgeColumnIndex;
    }

    public void setNeighborColumnIndex(Map<String, Integer> neighborColumnIndex) {
        this.neighborColumnIndex = neighborColumnIndex;
    }

    public void setSeedColumnIndex(Map<String, Integer> seedColumnIndex) {
        this.seedColumnIndex = seedColumnIndex;
    }

    public void setOriginSeedColumnIndex(Map<String, Integer> seedOriginColumnIndex) {
        this.seedOriginColumnIndex = seedOriginColumnIndex;
    }

    public Map<String, Integer> getEdgeColumnIndex() {
        return edgeColumnIndex;
    }

    public Map<String, Integer> getNeighborColumnIndex() {
        return neighborColumnIndex;
    }

    public Map<String, Integer> getSeedColumnIndex() {
        return seedColumnIndex;
    }

    public Dataset<Row> buildIndexes(Dataset<Row> neighborDS) {
        if (gnnSamplingLib.getIndexMetas() == null) {
            return neighborDS;
        }
        for (String indexMeta : gnnSamplingLib.getIndexMetas()) {
            String indexColumn = indexMeta.split(":")[1];
            neighborDS = neighborDS
                    .withColumn(indexColumn + "_index", functions.udf((WrappedArray aggValues) -> {
                        List valueList = (List) JavaConverters.seqAsJavaListConverter(aggValues).asJava();
                        return gnnSamplingLib.buildIndex(indexMeta, valueList);
                    }, DataTypes.BinaryType).apply(col(indexColumn)));

        }
        return neighborDS;
    }

    @Override
    public void write(Object... values) {
        subGraphElements.add(RowFactory.create(values));
    }

    public ExpressionEncoder getRowEncoder(OtherOutput otherOutput) {
        StructType structType = new StructType()
                .add(Constants.ENTRY_SEED, DataTypes.StringType)
                .add(Constants.ENTRY_TYPE, DataTypes.IntegerType)
                .add(Constants.ENTRY_NODE1, DataTypes.StringType)
                .add(Constants.ENTRY_NODE2, DataTypes.StringType)
                .add(Constants.ENTRY_ID, DataTypes.StringType)
                .add(Constants.ENTRY_FEATURE, DataTypes.StringType)
                .add(Constants.ENTRY_KIND, DataTypes.StringType);
        if (otherOutput != null && otherOutput.getNodeOtherOutputLen() > 0) {
            for (int i = 0; i < otherOutput.getNodeOtherOutputLen(); i++) {
                String fieldName = otherOutput.getNodeOutputField(i);
                String fieldType = otherOutput.getNodeOutputType(i);
                if (fieldType.compareToIgnoreCase("float") == 0) {
                    structType = structType.add(fieldName, DataTypes.FloatType);
                } else if (fieldType.compareToIgnoreCase("long") == 0) {
                    structType = structType.add(fieldName, DataTypes.LongType);
                } else if (fieldType.compareToIgnoreCase("string") == 0) {
                    structType = structType.add(fieldName, DataTypes.StringType);
                }
            }
        }
        return RowEncoder.apply(structType);
    }

    public ExpressionEncoder getExpressionEncoderWithOtherOutput() {
        String otherOutputSchema = gnnSamplingLib.getOtherOutputSchema(0);
        OtherOutput otherOutput = new OtherOutput.Builder().otherOutputSchema(otherOutputSchema).build();
        return getRowEncoder(otherOutput);
    }

    public FlatMapFunction<Row, Row> getRootNode() {
        return new FlatMapFunction<Row, Row>() {
            @Override
            public Iterator<Row> call(Row row) throws Exception {
                subGraphElements.clear();
                String nodeId = row.getString(seedOriginColumnIndex.get("node_id"));
                String seed = row.getString(seedOriginColumnIndex.get(Constants.ENTRY_SEED));
                if (seedOriginColumnIndex.containsKey("nodes_order")) {
                    String nodeOrders[] = row.getString(seedOriginColumnIndex.get("nodes_order")).split(" ");
                    int order = 0;
                    for (; order < nodeOrders.length; order++) {
                        if (nodeOrders[order].compareTo(nodeId) == 0) {
                            break;
                        }
                    }
                    gnnSamplingLib.processRootNodeOrder(nodeId, seed, order, SparkSampling.this);
                } else {
                    gnnSamplingLib.processRootNode(nodeId, seed, SparkSampling.this);
                }
                return subGraphElements.iterator();
            }
        };
    }

    public FlatMapFunction<Tuple2<Row, Row>, Row> getNeighborSelectionFunction(int hop) {
        return new FlatMapFunction<Tuple2<Row, Row>, Row>() {
            @Override
            public Iterator<Row> call(Tuple2<Row, Row> row) throws Exception {
                // neighbors: node1_id, node2_ids, edge_ids
                Row neighborInfo = row._1;
                String node1ID = neighborInfo.getString(0);
                List<String> node2IDs = neighborInfo.getList(1);
                List<String> edgeIDs = neighborInfo.getList(2);

                // seeds:  node_id, seed
                Row seedRow = row._2;
                List<String> seedList = seedRow.getList(1);

                HeteroDataset seedAttrs = new HeteroDataset(seedList.size());
                HeteroDataset neighborAttrs = new HeteroDataset(node2IDs.size());
                Map<String, BaseIndex> indexes = new HashMap<>();

                Filter filter = gnnSamplingLib.getFilter(hop - 1);
                SampleCondition sampleCond = gnnSamplingLib.getSampleConditions()[hop - 1];
                Map<VariableSource, Set<String>> referColumns = filter == null ? new HashMap<>() : filter.getReferSourceAndColumns();
                if (referColumns.containsKey(VariableSource.SEED)) {
                    for (String column : referColumns.get(VariableSource.SEED)) {
                        seedAttrs.addAttributeList(column, seedRow.getList(seedColumnIndex.get(column)));
                    }
                }

                Set<String> neighborColumns = new HashSet<>();
                if (referColumns.containsKey(VariableSource.INDEX)) {
                    neighborColumns.addAll(referColumns.get(VariableSource.INDEX));
                }
                if (sampleCond != null && sampleCond.getKey() != null) {
                    neighborColumns.add(sampleCond.getKey());
                }

                List<String> indexMetas = gnnSamplingLib.getIndexMetas();
                for (String column : neighborColumns) {
                    neighborAttrs.addAttributeList(column, neighborInfo.getList(neighborColumnIndex.get(column)));
                    String indexColumn = column + "_index";
                    if (neighborColumnIndex.containsKey(indexColumn)) {
                        String indexMeta = indexMetas.get(0);
                        for (int i = 0; i < indexMetas.size(); i++) {
                            indexMeta = indexMetas.get(i);
                            if (indexMeta.contains(":" + column + ":")) {
                                break;
                            }
                        }
                        BaseIndex index = new IndexFactory().loadIndex(indexMeta, neighborInfo.getAs(neighborColumnIndex.get(indexColumn)));
                        indexes.put(index.getIndexColumn(), index);
                    }
                }

                subGraphElements.clear();
                for (int seedIdx = 0; seedIdx < seedList.size(); seedIdx++) {
                    String seed = seedList.get(seedIdx);
                    Map<String, Element.Number> seedVariableMap = seedAttrs.fillVariables(seedIdx);
                    gnnSamplingLib.chooseNeighbor(node1ID, node2IDs, edgeIDs, indexes, neighborAttrs, seed, seedVariableMap, hop - 1, SparkSampling.this);
                }
                return subGraphElements.iterator();
            }
        };
    }

    public MapGroupsFunction<String, Row, Tuple2<String, String>> getGenerateGraphFeatureFunc(boolean removeEdgeAmongRoots, boolean needIDs) {
        return new MapGroupsFunction<String, Row, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> call(String key, Iterator<Row> values) throws Exception {
                String seed = key;
                SparkIterator sparkIterator = new SparkIterator(values);
                return new Tuple2<>(seed, gnnSamplingLib.generateGraphFeature(sparkIterator, removeEdgeAmongRoots));
            }
        };
    }

}
