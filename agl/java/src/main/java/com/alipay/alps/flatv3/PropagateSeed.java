package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.index.Filter;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.sampler.AbstractSampler;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.sampler.SamplerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// this is a api class for users to propagate seeds to next hop neighbors by using the indexes, filters and samplers.
public class PropagateSeed {
    public Filter filter = null;
    private AbstractSampler sampler = null;
    public String otherOutputSchema;

    /*
     * Constructor of PropagateSeed.
     * @param otherOutputSchema: schema of output, e.g. "id:STRING,age:INT", which is used to
     *                           build the index for neighbor table.
     * @param indexesMap:        a map of indexName -> index.
     * @param filterCond:        a filter condition, e.g. "age>=18 and age<=35".
     * @param sampleCond:        a sample condition, e.g. "topk(by=time, limit=5)".
     */
    public PropagateSeed(String otherOutputSchema, List<String> indexMetas, NeighborDataset neighborDataset, String filterCond, String sampleCond) throws Exception {
        this.otherOutputSchema = otherOutputSchema.replaceAll("\\s", "");
        filter = new Filter(indexMetas, filterCond, neighborDataset);
        sampler = SamplerFactory.createSampler(new SampleCondition(sampleCond), neighborDataset);

    }

    /*
     * This method will select a list of neighbor indices for each seed.
     * @param seeds:       a list of seeds, e.g. ["1", "2", "3"]
     * @param seedAttrs:   a list of seed attributes, e.g. ["1,18", "2,20", "3,25"]
     * @param frontierValues:  a map used to store the frontier values. e.g. {"age": 20}
     * @param neigborAttrs: a list of neighbor attributes. e.g. ["1,18", "2,20", "3,25"]
     * @return a list of neighbor indices for each seed. e.g. [[1, 2, 3], [4, 5, 6], [2, 4]]
     */
    public List<List<Integer>> process(List<String> seeds, List<List<Object>> seedAttrs,
                                       Map<String, Object> frontierValues, List<Object> neigborAttrs) throws Exception {
        List<List<Integer>> neighborList = new ArrayList<>();
        for (int i = 0; i < seeds.size(); i++) {
            AbstractIndexResult indexResult = filter.filter(seedAttrs.get(i));
            List<Integer> neighborIndices = sampler.sample(indexResult);
            neighborList.add(neighborIndices);
        }
        return neighborList;
    }
}
