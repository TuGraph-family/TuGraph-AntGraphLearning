package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.CommonResult;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WeightedSamplerTest {
    private BaseIndex baseIndex;
    private BaseIndex typeIndex;
    private BaseIndex weightIndex;
    private HeteroDataset neighborDataset;
    List<Float> weights = Arrays.asList(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F);

    @Before
    public void setUp() {
        List<String> ids = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");
        neighborDataset = new HeteroDataset(ids.size());
        neighborDataset.addAttributeList("weight", weights);
        neighborDataset.addAttributeList("type", types);
        baseIndex = new IndexFactory().createIndex("", neighborDataset);
        typeIndex = new IndexFactory().createIndex("range_index:type:string", neighborDataset);
        weightIndex = new IndexFactory().createIndex("range_index:weight:float", neighborDataset);
    }

    @Test
    public void testSmallCandidateSizeWithoutReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=5, replacement=false)";
        WeightedSampler sampler = new WeightedSampler(new SampleCondition(sampleMeta), neighborDataset);
        AbstractResult indexResult = new RangeResult(weightIndex, Collections.singletonList(new RangeUnit(0, 2)));
        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

    @Test
    public void testNoFilterSampleByWeightWithReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        RangeUnit candidateRange = new RangeUnit(0, 9);
        AbstractResult indexResult = new RangeResult(baseIndex, Collections.singletonList(candidateRange));

        int numSamples = 5500000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        float totalWeight = 0.0F;
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            totalWeight += weights.get(i);
        }
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            double empiricalProb = frequence[i] / (double) numSamples;
            assertEquals(3 * weights.get(i) / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testNoFilterSampleByWeightWithoutReplacementSmallSampleCount() {
        String sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        RangeUnit candidateRange = new RangeUnit(0, 9);
        AbstractResult indexResult = new RangeResult(baseIndex, Collections.singletonList(candidateRange));

        int numSamples = 5500000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        float[] expected = {0.019155F, 0.038025F, 0.058005F, 0.075135F, 0.09279F, 0.110305F, 0.12704F, 0.143865F, 0.16043F, 0.17525F};
        for (int i = candidateRange.getLow(); i <= candidateRange.getHigh(); i++) {
            double empiricalProb = frequence[i] / (double) numSamples / 2.0;
            assertEquals(expected[i], empiricalProb, expected[i] * 0.1);
        }
    }


    @Test
    public void testTypeFilterSampleByWeightWithReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        AbstractResult indexResult = new RangeResult(typeIndex, Arrays.asList(new RangeUnit(0, 3), new RangeUnit(6, 9)));

        int numSamples = 4500000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        float totalWeight = 46.0F;
        for (int i = 0; i < 10; i++) {
            if (i == 1 || i == 6) {
                continue;
            }
            double empiricalProb = frequence[i] / (float) numSamples;
            assertEquals(3.0F * weights.get(i) / totalWeight, empiricalProb, 0.01);
        }
    }

    @Test
    public void testWeightFilterSampleByWeightWithoutReplacementSmallSampleCount() {
        String sampleMeta = "weighted_sampler(by=weight, limit=2, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        RangeUnit candidateRange = new RangeUnit(1, 8);
        AbstractResult indexResult = new RangeResult(weightIndex, Collections.singletonList(candidateRange));

        int numSamples = 100000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        float[] expected = {0.0F, 0.04765F, 0.072515F, 0.094685F, 0.115405F, 0.13747F, 0.157795F, 0.1783F, 0.19618F, 0.0F};
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (double) numSamples / 2.0;
            assertEquals(expected[i], empiricalProb, expected[i] * 0.1);
        }
    }

    @Test
    public void testTypeWeightFilterSampleByWeightWithReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=4, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        List<Integer> candidateIndices = Arrays.asList(0, 2, 4, 5, 6, 9);
        AbstractResult indexResult = new CommonResult(weightIndex, candidateIndices);

        float totalWeight = 0.0F;
        for (int i : candidateIndices) {
            if (i == 0 || i == 2 || i == 4 || i == 5 || i == 6 || i == 9) {
                totalWeight += weights.get(i);
            }
        }

        int numSamples = (int) (totalWeight * 100000);
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (double) numSamples;
            if (i == 0 || i == 2 || i == 4 || i == 5 || i == 6 || i == 9) {
                assertEquals(4.0F * weights.get(i) / totalWeight, empiricalProb, 0.01);
            } else {
                assertEquals(0.0F, empiricalProb, 0.0001);
            }
        }
    }

    @Test
    public void testNoFilterByWeightWithoutReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        RangeUnit candidateRange = new RangeUnit(0, 9);
        AbstractResult indexResult = new RangeResult(baseIndex, Collections.singletonList(candidateRange));


        int numSamples = 1000000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        float[] expected = {0.020533F, 0.040367F, 0.060497F, 0.078057F, 0.095427F, 0.11189F, 0.127143F, 0.142127F, 0.15579F, 0.16817F};
        for (int i = 0; i < frequence.length; i++) {
            double empiricalProb = frequence[i] / (double) numSamples / 3.0;
            assertEquals(expected[i], empiricalProb, expected[i] * 0.1);
        }
    }


    @Test
    public void testWeightFilterByWeightWithoutReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=3, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        RangeUnit candidateRange = new RangeUnit(2, 8);
        AbstractResult indexResult = new RangeResult(weightIndex, Collections.singletonList(candidateRange));


        int numSamples = 1000000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        float[] expected = {0.0F, 0.0F, 0.08033F, 0.10474F, 0.126173F, 0.145977F, 0.164427F, 0.18205F, 0.196303F, 0.0F};
        for (int i = 0; i < frequence.length; i++) {
            double empiricalProb = frequence[i] / (double) numSamples / 3.0;
            assertEquals(expected[i], empiricalProb, expected[i] * 0.1);
        }
    }

    @Test
    public void testTypeTimeFilterByWeightWithoutReplacement() {
        String sampleMeta = "weighted_sampler(by=weight, limit=4, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        WeightedSampler sampler = new WeightedSampler(sampleCondition, neighborDataset);
        List<Integer> candidateIndices = Arrays.asList(0, 2, 4, 5, 6, 9);
        AbstractResult indexResult = new CommonResult(typeIndex, candidateIndices);


        int numSamples = 1000000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        float[] expected = {0.0496775F, 0.0F, 0.1312775F, 0.0F, 0.1828825F, 0.1978675F, 0.20961F, 0.0F, 0.0F, 0.228685F};
        for (int i = 0; i < frequence.length; i++) {
            double empiricalProb = frequence[i] / (double) numSamples / 4.0;
            assertEquals(expected[i], empiricalProb, expected[i] * 0.1);
        }
    }
}
