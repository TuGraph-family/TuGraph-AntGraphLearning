package com.alipay.alps.flatv3.neighbor_selection;

import com.alipay.alps.flatv3.filter.Filter;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.sampler.AbstractSampler;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.sampler.SamplerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// this is a api class for users to propagate seeds to next hop neighbors by using the indexes, filters and samplers.
public class PropagateSeed {
    private Filter filter = null;
    private String sampleCond = null;
    private String otherOutputSchema;

    /*
     * Constructor of PropagateSeed.
     * @param otherOutputSchema: schema of output, e.g. "id:STRING,age:INT", which is used to
     *                           build the index for neighbor table.
     * @param Filter:            a filter object
     * @param sampleCond:        a sample condition, e.g. "topk(by=time, limit=5)".
     */
    public PropagateSeed(String otherOutputSchema, Filter filter, String sampleCond) throws Exception {
        this.otherOutputSchema = otherOutputSchema.replaceAll("\\s", "");
        this.filter = filter;
        this.sampleCond = sampleCond;
    }

    /*
     * This method will select a list of neighbor indices for each seed.
     * @param indexesMap: a map of indexes, e.g. {"age": ageIndex, "type": typeIndex}
     * @param neighborDataset: neighbor dataset
     * @param seeds:       a list of seeds, e.g. ["1", "2", "3"]
     * @param seedAttrs:   a list of seed attributes, e.g. ["1,18", "2,20", "3,25"]
     * @param frontierValues:  a map used to store the frontier values. e.g. {"age": 20}
     * @param neigborAttrs: a list of neighbor attributes. e.g. ["1,18", "2,20", "3,25"]
     * @return a SampleOutput object, which contains the sampled neighbor indices. e.g. [[1, 2, 3], [4, 5, 6], [2, 4]]
     */
    public SampleOtherOutput process(Map<String, BaseIndex> indexesMap, HeteroDataset neighborDataset,
                                     List<String> seeds, HeteroDataset seedAttrs,
                                     Map<String, Object> frontierValues, List<Object> neigborAttrs) throws Exception {
        SampleOtherOutput samplerOutput = new SampleOtherOutput(this.otherOutputSchema, neighborDataset, seeds, seedAttrs, frontierValues, neigborAttrs);
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleCond), neighborDataset);
        for (int i = 0; i < seeds.size(); i++) {
            AbstractResult indexResult = filter.filter(i, seedAttrs, neighborDataset, indexesMap);
            List<Integer> neighborIndices = sampler.sample(indexResult);
            samplerOutput.addSampledNeighbors(neighborIndices);
        }
        return samplerOutput;
    }

    public static void main(String[] args) throws Exception {
        // Create the list of seed IDs as strings.
        for (int i = 0; i < 10; i++) {
            List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
            int node2IDSize = 100;
            List<RangeUnit> sortedIntervals = new ArrayList<>();
            sortedIntervals.add(new RangeUnit(0, node2IDSize - 1));

            SampleCondition sampleCondition = new SampleCondition("random_sampler(limit=15, seed=34, replacement=false)");
            for (String seed : seedIds) {
                sampleCondition.setSeed(1);
                AbstractSampler sampler = SamplerFactory.createSampler(sampleCondition, null);
                List<Integer> neighborIndices = sampler.sample(new RangeResult(null, sortedIntervals));
                System.out.println("--i:" + i + " seed:" + seed + " sampledN2:" + Arrays.toString(neighborIndices.toArray()));
            }
        }
    }
}
