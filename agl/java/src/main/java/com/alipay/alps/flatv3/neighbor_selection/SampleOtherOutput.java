package com.alipay.alps.flatv3.neighbor_selection;

import com.alipay.alps.flatv3.index.HeteroDataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SampleOtherOutput contains two parts: fixed part and variable part.
// The fixed part for nodes contains the following fields:
//   sample_id, 'node', node2_id 
//   (it means node2_id is a newly expanded neighbor of sample_id)
// The fixed part for edges contains the following fields:
//   sample_id, 'edge', node1_id, node2_id, edge_id
//   (it means node2_id is expanded using this edge from node1_id to node2_id)
// otherOutputSchema is node:source.$i:source.$k, edge:source.$j,
// the variable part of nodes contains the following fields: source.$i, source.$k.
// and the variable part of edges contains the following fields: source.$j.
// source can take any value from [index|seeds|frontier|neighbor].
// i,j,k can be any integer or string.
// For example, if otherOutputSchema is node:index.timestamp:source.1, edge:index.weight,
// the variable part of nodes contains: timestamp of node2_id and first value of seedAttrs.
// the variable part of edges contains: weight of node2_id.
public class SampleOtherOutput {
    private static final String OUTPUT_NODE = "node";
    private static final String OUTPUT_EDGE = "edge";
    private static final String SOURCE_INDEX = "index";
    private static final String SOURCE_SEEDS = "seed";
    private static final int SOURCE_INDEX_ID = 0;
    private static final int SOURCE_SEEDS_ID = 1;
    private int[] otherNodeOutputSource;
    private int[] otherEdgeOutputSource;
    private List<Object>[] otherNodeOutputList;
    private List<Object>[] otherEdgeOutputList;
    private List<List<Integer>> sampledNeighbors = new ArrayList<List<Integer>>();
    private String otherOutputSchema;

    private HeteroDataset neighborDataset;
    private List<String> seeds;
    private HeteroDataset seedAttrs;
    private Map<String, Object> frontierValues;
    private List<Object> neigborAttrs;

    public SampleOtherOutput(String otherOutputSchema, HeteroDataset neighborDataset,
                             List<String> seeds, HeteroDataset seedAttrs,
                             Map<String, Object> frontierValues, List<Object> neigborAttrs) {
        this.otherOutputSchema = otherOutputSchema;
        this.neighborDataset = neighborDataset;
        this.seeds = seeds;
        this.seedAttrs = seedAttrs;
        this.frontierValues = frontierValues;
        this.neigborAttrs = neigborAttrs;

        // split otherOutputSchema and remove empty strings
        String[] parts = otherOutputSchema.split(",");
        for (String part : parts) {
            String arrs[] = part.split(":");
            if (arrs.length <= 1) {
                continue;
            }
            switch (arrs[0]) {
                case OUTPUT_NODE:
                    otherNodeOutputList = new List[arrs.length - 1];
                    otherNodeOutputSource = new int[arrs.length - 1];
                    parseOutputSchema(otherNodeOutputList, otherNodeOutputSource, part.split(":"));
                    break;
                case OUTPUT_EDGE:
                    otherEdgeOutputList = new List[arrs.length - 1];
                    otherEdgeOutputSource = new int[arrs.length - 1];
                    parseOutputSchema(otherEdgeOutputList, otherEdgeOutputSource, part.split(":"));
                    break;
                default:
                    throw new RuntimeException("schema is wrong");
            }
        }
    }

    private void parseOutputSchema(List<Object>[] otherOutputList, int[] otherOutputSource, String[] t) {
        for (int j = 1; j < t.length; j++) {
            String[] ts = t[j].split("\\.");
            assert ts.length == 2;
            switch (ts[0]) {
                case SOURCE_INDEX:
                    if (neighborDataset == null || neighborDataset.getAttributeList(ts[1]) == null) {
                        throw new RuntimeException("index output schema is wrong: " + (neighborDataset == null ? "neighborDataset is null" : "neighborDataset keyset:" + neighborDataset + " missing key:" + ts[1]));
                    }
                    otherOutputList[j - 1] = (List) neighborDataset.getAttributeList(ts[1]);
                    otherOutputSource[j - 1] = SOURCE_INDEX_ID;
                    break;
                case SOURCE_SEEDS:
                    if (seedAttrs == null || seedAttrs.getAttributeList(ts[1]) == null) {
                        throw new RuntimeException("seed output schema is wrong");
                    }
                    otherOutputList[j - 1] = (List) seedAttrs.getAttributeList(ts[1]);
                    otherOutputSource[j - 1] = SOURCE_SEEDS_ID;
                    break;
                default:
                    throw new RuntimeException("schema is wrong");
            }
        }
    }

    public List<Integer> getSampledNeighbors(int seedIdx) {
        return sampledNeighbors.get(seedIdx);
    }

    public void addSampledNeighbors(List<Integer> neighbors) {
        sampledNeighbors.add(neighbors);
    }

    public int getNodeOtherOutputLen() {
        return otherNodeOutputList == null ? 0 : otherNodeOutputList.length;
    }

    public int getEdgeOtherOutputLen() {
        return otherEdgeOutputList == null ? 0 : otherEdgeOutputList.length;
    }

    public Object[][] getNodeOtherOutputs(int seedIdx) {
        return getOtherOutputs(seedIdx, otherNodeOutputList, otherNodeOutputSource);
    }

    public Object[][] getEdgeOtherOutputs(int seedIdx) {
        return getOtherOutputs(seedIdx, otherEdgeOutputList, otherEdgeOutputSource);
    }

    private Object[][] getOtherOutputs(int seedIdx, List<Object>[] otherOutputList, int[] otherOutputSource) {
        if (otherOutputList == null) {
            return null;
        }
        List<Integer> neighborsList = sampledNeighbors.get(seedIdx);
        int len = neighborsList.size();
        Object[][] ans = new Object[len][otherOutputList.length];

        for (int col = 0; col < otherOutputList.length; col++) {
            List<Object> colList = otherOutputList[col];
            switch (otherOutputSource[col]) {
                case SOURCE_INDEX_ID:
                    for (int row = 0; row < len; row++) {
                        ans[row][col] = colList.get(neighborsList.get(row));
                    }
                    break;
                case SOURCE_SEEDS_ID:
                    Object v = colList.get(seedIdx);
                    for (int row = 0; row < len; row++) {
                        ans[row][col] = v;
                    }
                    break;
                default:
                    throw new RuntimeException("schema is wrong");
            }
        }
        return ans;
    }
}