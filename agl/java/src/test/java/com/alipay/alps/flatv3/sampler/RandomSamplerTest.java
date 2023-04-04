package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.Filter;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.RangeIndex;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RandomSamplerTest {
    // test sampling with replacement based on no filter results
    @Test
    public void testSample_withSmallSampleSize() throws Exception {
        List<String> ids = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(1.0F * i);
        }
        NeighborDataset neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("weight", weight);
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(baseIndex.getIndexColumn(), baseIndex);
        Filter filter = new Filter(indexMap, "");
        List<Object> seedData = Arrays.asList(0.1);
        AbstractIndexResult indexResult = filter.filter(seedData);
        
        String sampleMeta = "random_sampler(limit=5, replacement=False)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithReplacement[i] / (float) numSamples;
            assertEquals(5.0F * 1 / 10, empiricalProb, 0.01);
        }
    }

    // test sampling without replacemnt based on range filter results,
    // but the sample size is less than the number of filtered results * sampleCountToCandidateCountRatio
    @Test
    public void testSample_withSmallSampleSize_withFilter() throws Exception {
        List<String> ids = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(0.1F * i);
        }
        NeighborDataset neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("weight", weight);
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexColumn(), rangeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.weight < 0.9 + seed.1");
        List<Object> seedData = Arrays.asList(0.1);
        AbstractIndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=2, replacement=False)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithReplacement[i] / (float) numSamples;
            if (i >= 2) {
                assertEquals(2.0F * 1 / 8, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }

    // test sampling without replacement based on type filter results, 
    // but the sample size is larger than the number of filtered results
    @Test
    public void testSample_withLargeSampleSize_withFilter() throws Exception {
        List<String> ids = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(0.1F * i);
        }
        NeighborDataset neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("weight", weight);
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexColumn(), rangeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.weight < 0.8 + seed.1");
        List<Object> seedData = Arrays.asList(0.1);
        AbstractIndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=20, replacement=False)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithReplacement[i] / (float) numSamples;
            if (i >= 2 && i < 9) {
                assertEquals(1.0F, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }

    // test sampling without replacement based on type filter results, 
    // but the sample size is larger than the number of filtered results * sampleCountToCandidateCountRatio
    @Test
    public void testSample_withLargeSampleSize_withFilter_2() throws Exception {
        List<String> ids = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(0.1F * i);
        }
        NeighborDataset neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("weight", weight);
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexColumn(), rangeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.weight < 0.8 + seed.1");
        List<Object> seedData = Arrays.asList(0.1);
        AbstractIndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=6, replacement=False)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithReplacement[i] / (float) numSamples;
            if (i >= 2 && i < 9) {
                assertEquals(1.0F, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }

    // test sampling with replacement based on type filter and range filter results
    @Test
    public void testSample_withReplacement_withFilter() throws Exception {
        List<String> ids = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(0.1F * i);
        }
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");
        NeighborDataset neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("node_type", typeList);
        neighborDataset.addAttributeList("weight", weight);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        indexMap.put(rangeIndex.getIndexColumn(), rangeIndex);
        BaseIndex typeIndex = new HashIndex("hash_index:node_type:string", neighborDataset);
        indexMap.put(typeIndex.getIndexColumn(), typeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.node_type in (user, shop)");
        List<Object> seedData = Arrays.asList(0.1);
        AbstractIndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=2, replacement=True)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithReplacement[i] / (float) numSamples;
            if (i >= 2 && i < 9) {
                assertEquals(2.0F * 1 / 7, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }
}
