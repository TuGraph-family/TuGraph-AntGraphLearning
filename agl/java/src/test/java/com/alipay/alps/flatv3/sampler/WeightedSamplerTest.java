package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
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

public class WeightedSamplerTest {
    List<String> ids = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private List<Float> weights = Arrays.asList(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F);
    private List<Long> times = Arrays.asList(100L, 99L, 98L, 97L, 96L, 95L, 94L, 93L, 92L, 91L);
    private List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");

    @Test
    public void testSmallCandidateSize() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("weight", new ArrayList<>(weights));
        RangeIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(rangeIndex.getIndexColumn(), rangeIndex);
        String sampleMeta = "weighted_sampler(by=index.weight, limit=5, replacement=false)";
        WeightedSampler sampler = new WeightedSampler(new SampleCondition(sampleMeta), indexes);
        IndexResult indexResult = new RangeIndexResult(indexes, Collections.singletonList(new Range(0, 2)));
        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

    @Test
    public void testNoFilterSampleByWeightUnorderedWithoutReplacement() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("weight", new ArrayList<>(weights));
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(baseIndex.getIndexColumn(), baseIndex);
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        Range candidateRange = new Range(1, 9);
        IndexResult indexResult = new RangeIndexResult(indexes, Collections.singletonList(candidateRange));

        int numSamples = 4500000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
            }
        }
        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            double empiricalProb = freqWithReplacement[i] / (double) numSamples;
            assertEquals(3.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testNoFilterSampleByWeightUnorderedWithReplacementSmallSampleCount() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("weight", new ArrayList<>(weights));
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(baseIndex.getIndexColumn(), baseIndex);
        String sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        Range candidateRange = new Range(1, 9);
        IndexResult indexResult = new RangeIndexResult(indexes, Collections.singletonList(candidateRange));
    
        sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=false)";
        sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, indexes);
        indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(new Range(1, 9)));
        int [] freqWithoutReplacement = new int[10];
        int numSamples = 4500000;
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(2.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testTypeFilterSampleByWeightUnorderedWithReplacement() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("type", new ArrayList<>(types));
        BaseIndex hashIndex = new HashIndex("hash_index:type:string", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(hashIndex.getIndexColumn(), hashIndex);
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        Range candidateRange = new Range(1, 9);
        IndexResult indexResult = new RangeIndexResult(indexes, Collections.singletonList(candidateRange));

        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }

        int numSamples = 4500000;
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
            assertEquals(2.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testTypeFilterSampleByWeightOrderedWithoutReplacementSmallSampleCount() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("weight", new ArrayList<>(weights));
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(rangeIndex.getIndexColumn(), rangeIndex);
        String sampleMeta = "weighted_sampler(by=time, limit=2, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        Range candidateRange = new Range(1, 9);
        IndexResult indexResult = new RangeIndexResult(indexes, Collections.singletonList(candidateRange));

        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }

        int numSamples = 4500000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testTypeTimeFilterSampleByTimeOrderedWithReplacement() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("type", new ArrayList<>(types));
        BaseIndex hashIndex = new HashIndex("hash_index:type:string", neighborDataset);
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(hashIndex.getIndexColumn(), hashIndex);
        indexes.put(rangeIndex.getIndexColumn(), rangeIndex);
        String sampleMeta = "weighted_sampler(by=time, limit=4, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        List<Integer> candidateIndices = Arrays.asList(0, 2, 4, 5, 6, 9);
        IndexResult indexResult = new CommonIndexResult(indexes, candidateIndices);

        float totalWeight = 0.0F;
        for (int i : candidateIndices) {
            totalWeight += weights.get(i);
        }

        int numSamples = 4500000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(4.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }


    @Test
    public void testNoFilterByTimestampUnorderedWithoutReplacement() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("weight", new ArrayList<>(weights));
        BaseIndex rangeIndex = new RangeIndex("", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(rangeIndex.getIndexColumn(), rangeIndex);
        String sampleMeta = "weighted_sampler(by=time, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        Range candidateRange = new Range(0, 9);
        IndexResult indexResult = new RangeIndexResult(indexes, Collections.singletonList(candidateRange));


        int numSamples = 1000000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }


    @Test
    public void testTimeFilterByTimestampOrderedWithoutReplacement() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(rangeIndex.getIndexColumn(), rangeIndex);
        String sampleMeta = "weighted_sampler(by=time, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        Range candidateRange = new Range(2, 8);
        IndexResult indexResult = new RangeIndexResult(indexes, Collections.singletonList(candidateRange));


        int numSamples = 1000000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testTypeTimeFilterByTimestampOrderedWithoutReplacement() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(times));
        neighborDataset.addAttributes("type", new ArrayList<>(types));
        BaseIndex hashIndex = new HashIndex("hash_index:type:string", neighborDataset);
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(hashIndex.getIndexColumn(), hashIndex);
        indexes.put(rangeIndex.getIndexColumn(), rangeIndex);
        String sampleMeta = "weighted_sampler(by=time, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, indexes);
        List<Integer> candidateIndices = Arrays.asList(0, 2, 4, 5, 6, 9);
        IndexResult indexResult = new CommonIndexResult(indexes, candidateIndices);


        int numSamples = 1000000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        float totalWeight = 0.0F;
        for (int i : candidateIndices) {
            totalWeight += weights.get(i);
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }
}
