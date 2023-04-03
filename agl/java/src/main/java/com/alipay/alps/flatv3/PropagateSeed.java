package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.Filter;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.sampler.SampleCondition;
import com.alipay.alps.flatv3.sampler.Sampler;
import com.alipay.alps.flatv3.sampler.SamplerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropagateSeed {
    public Filter filter = null;
    private Sampler sampler = null;
    public String otherOutputSchema;

    public PropagateSeed(String otherOutputSchema, Map<String, BaseIndex> indexesMap, String filterCond, String sampleCond) {
        this.otherOutputSchema = otherOutputSchema.replaceAll("\\s", "");
        filter = new Filter(indexesMap, filterCond);
        sampler = SamplerFactory.createSampler(new SampleCondition(sampleCond), indexesMap);

    }

    public List<List<Integer>> process(List<String> seeds, List<List<Object>> seedAttrs,
                                       Map<String, Object> frontierValues, List<Object> neigborAttrs) throws Exception {
        List<List<Integer>> neighborList = new ArrayList<>();
        for (int i = 0; i < seeds.size(); i++) {
            IndexResult indexResult = filter.filter(seedAttrs.get(i));
            List<Integer> neighborIndices = sampler.sample(indexResult);
            neighborList.add(neighborIndices);
        }
        return neighborList;
    }
}
