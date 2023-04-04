package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class WeightedSamplerTest {
    private BaseIndex baseIndex;
    private HashIndex typeIndex;
    private RangeIndex timeIndex;
    private RangeIndex weightIndex;
    private NeighborDataset neighborDataset;
    List<Float> weights = Arrays.asList(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F);

    @Before
    public void setUp() {
        List<String> ids = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        List<Long> times = Arrays.asList(100L, 99L, 98L, 97L, 96L, 95L, 94L, 93L, 92L, 91L);
        List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");
        neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("weight", weights);
        neighborDataset.addAttributeList("time", times);
        neighborDataset.addAttributeList("type", types);
        baseIndex = new BaseIndex("", neighborDataset);
        typeIndex = new HashIndex("range_index:type:string", neighborDataset);
        timeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        weightIndex = new RangeIndex("range_index:weight:float", neighborDataset);
    }

    @Test
    public void testSmallCandidateSize() {
        String sampleMeta = "weighted_sampler(by=index.weight, limit=5, replacement=false)";
        WeightedSampler sampler = new WeightedSampler(new SampleCondition(sampleMeta), neighborDataset);
        AbstractIndexResult indexResult = new RangeIndexResult(weightIndex, Collections.singletonList(new Range(0, 2)));
        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

    @Test
    public void testNoFilterSampleByWeightUnorderedWithoutReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        Range candidateRange = new Range(1, 9);
        AbstractIndexResult indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(candidateRange));

        int numSamples = 4500000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithReplacement[index]++;
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
        String sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        Range candidateRange = new Range(1, 9);
        AbstractIndexResult indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(candidateRange));
    
        sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=false)";
        sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, neighborDataset);
        indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(new Range(1, 9)));
        int [] freqWithoutReplacement = new int[10];
        int numSamples = 4500000;
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithoutReplacement[index]++;
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
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        Range candidateRange = new Range(1, 9);
        AbstractIndexResult indexResult = new RangeIndexResult(typeIndex, Collections.singletonList(candidateRange));

        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }

        int numSamples = 4500000;
        int [] freqWithReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithReplacement[i] / (float) numSamples;
            assertEquals(2.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testTypeFilterSampleByWeightOrderedWithoutReplacementSmallSampleCount() {
        String sampleMeta = "weighted_sampler(by=time, limit=2, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        Range candidateRange = new Range(1, 9);
        AbstractIndexResult indexResult = new RangeIndexResult(timeIndex, Collections.singletonList(candidateRange));

        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }

        int numSamples = 4500000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithoutReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testTypeTimeFilterSampleByTimeOrderedWithReplacement() {
        String sampleMeta = "weighted_sampler(by=time, limit=4, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        List<Integer> candidateIndices = Arrays.asList(0, 2, 4, 5, 6, 9);
        AbstractIndexResult indexResult = new CommonIndexResult(typeIndex, candidateIndices);

        float totalWeight = 0.0F;
        for (int i : candidateIndices) {
            totalWeight += weights.get(i);
        }

        int numSamples = 4500000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithoutReplacement[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(4.0F * i / totalWeight, empiricalProb, 0.01);
        }
    }


    @Test
    public void testNoFilterByTimestampUnorderedWithoutReplacement() {
        String sampleMeta = "weighted_sampler(by=time, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        Range candidateRange = new Range(0, 9);
        AbstractIndexResult indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(candidateRange));


        int numSamples = 1000000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithoutReplacement[index]++;
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
        String sampleMeta = "weighted_sampler(by=time, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        Range candidateRange = new Range(2, 8);
        AbstractIndexResult indexResult = new RangeIndexResult(timeIndex, Collections.singletonList(candidateRange));


        int numSamples = 1000000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithoutReplacement[index]++;
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
        String sampleMeta = "weighted_sampler(by=time, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        List<Integer> candidateIndices = Arrays.asList(0, 2, 4, 5, 6, 9);
        AbstractIndexResult indexResult = new CommonIndexResult(typeIndex, candidateIndices);


        int numSamples = 1000000;
        int [] freqWithoutReplacement = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                freqWithoutReplacement[index]++;
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
