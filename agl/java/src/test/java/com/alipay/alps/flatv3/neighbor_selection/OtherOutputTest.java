package com.alipay.alps.flatv3.neighbor_selection;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.spark.utils.Constants;
import com.antfin.agl.proto.sampler.Element;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class OtherOutputTest {
    private HeteroDataset neighborDataset;
    List<Integer> indices = new ArrayList<>();
    List<String> node2Ids = new ArrayList<>();
    List<String> edgeIds = new ArrayList<>();
    List<Long> times = new ArrayList<>();
    List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");

    @Before
    public void setUp() {
        for (int i = 0; i < 10; i++) {
            node2Ids.add("n" + String.valueOf(i));
            edgeIds.add("e" + String.valueOf(i));
            times.add(2L * i);
            indices.add(i);
        }
        neighborDataset = new HeteroDataset(node2Ids.size());
        neighborDataset.addAttributeList("time", times);
        neighborDataset.addAttributeList("type", types);
    }

    @Test
    public void testNoFilterTopkSampler() throws Exception {
        // Create the list of seed IDs as strings.
        List<String> seedIds = Arrays.asList("s1", "s2", "s3", "s4", "s5");

        // Create the list of seed attributes as lists of objects.
        HeteroDataset seedAttrs = new HeteroDataset(seedIds.size());
        List<Long> seedTimes = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedTimes.add((i + 1) * 1L);
        }
        seedAttrs.addAttributeList("time", seedTimes);

        // Create the filter condition.
        String filterCond = "";

        // Create the sample condition.
        String sampleCond = "topk(by=time, limit=2)";

        // Other Output Schema
        String otherOutputSchema = "node:index.time.long:seed.time.long";

        // Create the PropagateSeed object.
        Filter filter = new Filter(filterCond);
        NeighborSelector neighborSelector = new NeighborSelector(otherOutputSchema, filter, new SampleCondition(sampleCond));

        // Run the algorithm.
        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(null, neighborDataset);
        String node1ID = "currentNode";
        List<Object[]> ans = new ArrayList<>();
        for (int seedIdx = 0; seedIdx < seedIds.size(); seedIdx++) {
            String seed = seedIds.get(seedIdx);
            Map<String, Element.Number> seedVariableMap = seedAttrs.fillVariables(seedIdx);
            List<List<Integer>> sampledNeighbors = neighborSelector.process(indexMap, neighborDataset, seedIds, seedAttrs);
            OtherOutput otherOutput = new OtherOutput.Builder()
                    .otherOutputSchema(otherOutputSchema)
                    .neighborDataset(neighborDataset)
                    .seedAttrs(seedVariableMap)
                    .build();
            int nodeOutputWidth = otherOutput.getNodeOtherOutputLen();
            List<Integer> sampledNeighborIndex = sampledNeighbors.get(seedIdx);
            for (int i = 0; i < sampledNeighborIndex.size(); i++) {
                String node2ID = node2Ids.get(sampledNeighborIndex.get(i));
                String edgeID = edgeIds.get(sampledNeighborIndex.get(i));
                int neighborIdx = sampledNeighborIndex.get(i);
                if (nodeOutputWidth == 0) {
                    ans.add(new Object[]{seed, Constants.NODE_INT, null, null, node2ID, null, null});
                    ans.add(new Object[]{seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, null});
                } else if (nodeOutputWidth == 1) {
                    ans.add(new Object[]{seed, Constants.NODE_INT, null, null, node2ID, null, null, otherOutput.getNodeOtherOutput(0, neighborIdx)});
                    ans.add(new Object[]{seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, null, null});
                } else if (nodeOutputWidth == 2) {
                    ans.add(new Object[]{seed, Constants.NODE_INT, null, null, node2ID, null, null, otherOutput.getNodeOtherOutput(0, neighborIdx), otherOutput.getNodeOtherOutput(1, neighborIdx)});
                    ans.add(new Object[]{seed, Constants.EDGE_INT, node1ID, node2ID, edgeID, null, null, null, null});
                }
            }
        }

        // Create the list of expected results.
        List<Object[]> expected = new ArrayList<>();
        expected.add(new Object[]{"s1", Constants.NODE_INT, null, null, "n0", null, null, 0L, 1L});
        expected.add(new Object[]{"s1", Constants.EDGE_INT, "currentNode", "n0", "e0", null, null, null, null});
        expected.add(new Object[]{"s1", Constants.NODE_INT, null, null, "n1", null, null, 2L, 1L});
        expected.add(new Object[]{"s1", Constants.EDGE_INT, "currentNode", "n1", "e1", null, null, null, null});
        expected.add(new Object[]{"s2", Constants.NODE_INT, null, null, "n0", null, null, 0L, 2L});
        expected.add(new Object[]{"s2", Constants.EDGE_INT, "currentNode", "n0", "e0", null, null, null, null});
        expected.add(new Object[]{"s2", Constants.NODE_INT, null, null, "n1", null, null, 2L, 2L});
        expected.add(new Object[]{"s2", Constants.EDGE_INT, "currentNode", "n1", "e1", null, null, null, null});
        expected.add(new Object[]{"s3", Constants.NODE_INT, null, null, "n0", null, null, 0L, 3L});
        expected.add(new Object[]{"s3", Constants.EDGE_INT, "currentNode", "n0", "e0", null, null, null, null});
        expected.add(new Object[]{"s3", Constants.NODE_INT, null, null, "n1", null, null, 2L, 3L});
        expected.add(new Object[]{"s3", Constants.EDGE_INT, "currentNode", "n1", "e1", null, null, null, null});
        expected.add(new Object[]{"s4", Constants.NODE_INT, null, null, "n0", null, null, 0L, 4L});
        expected.add(new Object[]{"s4", Constants.EDGE_INT, "currentNode", "n0", "e0", null, null, null, null});
        expected.add(new Object[]{"s4", Constants.NODE_INT, null, null, "n1", null, null, 2L, 4L});
        expected.add(new Object[]{"s4", Constants.EDGE_INT, "currentNode", "n1", "e1", null, null, null, null});
        expected.add(new Object[]{"s5", Constants.NODE_INT, null, null, "n0", null, null, 0L, 5L});
        expected.add(new Object[]{"s5", Constants.EDGE_INT, "currentNode", "n0", "e0", null, null, null, null});
        expected.add(new Object[]{"s5", Constants.NODE_INT, null, null, "n1", null, null, 2L, 5L});
        expected.add(new Object[]{"s5", Constants.EDGE_INT, "currentNode", "n1", "e1", null, null, null, null});

        for (int i = 0; i < ans.size(); i++) {
            assertEquals(expected.get(i), ans.get(i));
        }

    }
}
