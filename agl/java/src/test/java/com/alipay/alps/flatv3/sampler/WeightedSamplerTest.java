package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WeightedSamplerTest {
    List<String> ids = new ArrayList<>();
    List<Float> weight = new ArrayList<>();
    List<Long> time = new ArrayList<>();

    @Before
    public void setUp() {
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(1.0F * i);
            time.add(100L - i);
        }
    }

    @Test
    public void testSample_withSmallSampleSize_withoutReplacement() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(time));
        neighborDataset.addAttributes("weight", new ArrayList<>(weight));
        RangeIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        String sampleMeta = "weighted_sampler(by=index.weight, limit=5, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(0, 2)));
        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

    @Test
    public void testSample_AliasMethod() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(time));
        neighborDataset.addAttributes("weight", new ArrayList<>(weight));
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);
        String sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, baseIndex);
        Range candidateRange = new Range(1, 9);
        IndexResult indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(candidateRange));

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
            totalWeight += weight.get(i);
        }
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            double empiricalProb = freqWithReplacement[i] / (double) numSamples;
            assertEquals(2.0F * i / totalWeight, empiricalProb, 0.01);
        }

        sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=false)";
        sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, baseIndex);
        indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(new Range(1, 9)));
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = neighborDataset.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(2.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testSample_PrefixSum() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(time));
        neighborDataset.addAttributes("weight", new ArrayList<>(weight));
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, rangeIndex);
        Range candidateRange = new Range(1, 9);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(candidateRange));

        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weight.get(i);
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

        sampleMeta = "weighted_sampler(by=index.weight, limit=2, replacement=false)";
        sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 9)));
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
    public void testSample_OrderStatisticTree() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(new ArrayList<>(ids), null);
        neighborDataset.addAttributes("time", new ArrayList<>(time));
        neighborDataset.addAttributes("weight", new ArrayList<>(weight));
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, rangeIndex);
        Range candidateRange = new Range(1, 9);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(candidateRange));


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
            totalWeight += weight.get(i);
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }
}
