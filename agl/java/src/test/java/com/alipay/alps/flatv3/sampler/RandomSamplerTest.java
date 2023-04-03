package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.Filter;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("weight", weight);
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(baseIndex.getIndexType(), baseIndex);
        Filter filter = new Filter(indexMap, "");
        List<Object> seedData = Arrays.asList(0.1);
        IndexResult indexResult = filter.filter(seedData);
        
        String sampleMeta = "random_sampler(limit=5, replacement=False)";
        Sampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), indexMap);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
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
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("weight", weight);
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.weight < 0.9 + seed.1");
        List<Object> seedData = Arrays.asList(0.1);
        IndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=2, replacement=False)";
        Sampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), indexMap);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
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
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("weight", weight);
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.weight < 0.8 + seed.1");
        List<Object> seedData = Arrays.asList(0.1);
        IndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=20, replacement=False)";
        Sampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), indexMap);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
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
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("weight", weight);
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.weight < 0.8 + seed.1");
        List<Object> seedData = Arrays.asList(0.1);
        IndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=6, replacement=False)";
        Sampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), indexMap);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
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
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("node_type", typeList);
        neighborDataset.addAttributes("weight", weight);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);
        BaseIndex typeIndex = new HashIndex("hash_index:node_type:string", neighborDataset);
        indexMap.put(typeIndex.getIndexType(), typeIndex);
        Filter filter = new Filter(indexMap, "index.weight - seed.1 >= 0.1 and index.node_type in (user, shop)");
        List<Object> seedData = Arrays.asList(0.1);
        IndexResult indexResult = filter.filter(seedData);

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=2, replacement=True)";
        Sampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), indexMap);

        int numSamples = 40000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
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
