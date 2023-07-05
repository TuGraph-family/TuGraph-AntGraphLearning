package com.alipay.alps.flatv3.neighbor_selection;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SampleOtherOutputTest {
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
        String otherOutputSchema = "node:index.time:seed.time, edge:index.type";

        // Create the PropagateSeed object.
        Filter filter = new Filter(filterCond);
        PropagateSeed propagateSeed = new PropagateSeed(otherOutputSchema, filter, sampleCond);

        // Run the algorithm.
        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(null, neighborDataset);
        SampleOtherOutput sampleOtherOutput = propagateSeed.process(indexMap, neighborDataset, seedIds, seedAttrs, null, null);

        String node1ID = "currentNode";
        List<Object[]> ans = new ArrayList<>();
        for (int seedIdx = 0; seedIdx < seedIds.size(); seedIdx++) {
            List<Integer> sampledNeighborIndex = sampleOtherOutput.getSampledNeighbors(seedIdx);
            Object[][] nodeOtherOutputs = sampleOtherOutput.getNodeOtherOutputs(seedIdx);
            Object[][] edgeOtherOutputs = sampleOtherOutput.getEdgeOtherOutputs(seedIdx);
            int nodeOutputWidth = sampleOtherOutput.getNodeOtherOutputLen();
            int edgeOutputWidth = sampleOtherOutput.getEdgeOtherOutputLen();
            String SeedId = seedIds.get(seedIdx);
            for (int j = 0; j < sampledNeighborIndex.size(); j++) {
                // fill ans[seedIdx]
                String node2ID = node2Ids.get(sampledNeighborIndex.get(j));
                String edgeID = edgeIds.get(sampledNeighborIndex.get(j));
                ans.add(new Object[]{SeedId, "node", node2ID, null, 0 < nodeOutputWidth ? nodeOtherOutputs[j][0] : null, 1 < nodeOutputWidth ? nodeOtherOutputs[j][1] : null});
                ans.add(new Object[]{SeedId, "edge", node1ID, node2ID, edgeID, 0 < edgeOutputWidth ? edgeOtherOutputs[j][0] : null});
            }
        }

        // Create the list of expected results.
        List<Object[]> expected = new ArrayList<>();
        expected.add(new Object[]{"s1", "node", "n0", null, 0L, 1L});
        expected.add(new Object[]{"s1", "edge", "currentNode", "n0", "e0", "item"});
        expected.add(new Object[]{"s1", "node", "n1", null, 2L, 1L});
        expected.add(new Object[]{"s1", "edge", "currentNode", "n1", "e1", "shop"});
        expected.add(new Object[]{"s2", "node", "n0", null, 0L, 2L});
        expected.add(new Object[]{"s2", "edge", "currentNode", "n0", "e0", "item"});
        expected.add(new Object[]{"s2", "node", "n1", null, 2L, 2L});
        expected.add(new Object[]{"s2", "edge", "currentNode", "n1", "e1", "shop"});
        expected.add(new Object[]{"s3", "node", "n0", null, 0L, 3L});
        expected.add(new Object[]{"s3", "edge", "currentNode", "n0", "e0", "item"});
        expected.add(new Object[]{"s3", "node", "n1", null, 2L, 3L});
        expected.add(new Object[]{"s3", "edge", "currentNode", "n1", "e1", "shop"});
        expected.add(new Object[]{"s4", "node", "n0", null, 0L, 4L});
        expected.add(new Object[]{"s4", "edge", "currentNode", "n0", "e0", "item"});
        expected.add(new Object[]{"s4", "node", "n1", null, 2L, 4L});
        expected.add(new Object[]{"s4", "edge", "currentNode", "n1", "e1", "shop"});
        expected.add(new Object[]{"s5", "node", "n0", null, 0L, 5L});
        expected.add(new Object[]{"s5", "edge", "currentNode", "n0", "e0", "item"});
        expected.add(new Object[]{"s5", "node", "n1", null, 2L, 5L});
        expected.add(new Object[]{"s5", "edge", "currentNode", "n1", "e1", "shop"});

        for (int i = 0; i < ans.size(); i++) {
            assertEquals(expected.get(i), ans.get(i));
        }

    }
}
