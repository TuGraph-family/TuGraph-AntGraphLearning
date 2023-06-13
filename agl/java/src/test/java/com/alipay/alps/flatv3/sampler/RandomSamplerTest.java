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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RandomSamplerTest {
    private HeteroDataset neighborDataset;
    private BaseIndex baseIndex;
    private BaseIndex weightIndex;
    private BaseIndex typeIndex;

    @Before
    public void setUp() {
        List<String> ids = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(1.0F - 0.1F * i);
        }
        neighborDataset = new HeteroDataset(ids.size());
        neighborDataset.addAttributeList("weight", weight);
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");
        neighborDataset.addAttributeList("node_type", typeList);
        baseIndex = new IndexFactory().createIndex("", neighborDataset);
        weightIndex = new IndexFactory().createIndex("range_index:weight:float", neighborDataset);
        typeIndex = new IndexFactory().createIndex("hash_index:node_type:string", neighborDataset);
    }

    @Test
    public void testNoFilterWithReplacement() {
        AbstractResult indexResult = new RangeResult(baseIndex, Collections.singletonList(new RangeUnit(0, 9)));
        String sampleMeta = "random_sampler(limit=6, replacement=true)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 100000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (float) numSamples;
            assertEquals(6.0F * 1 / 10, empiricalProb, 0.01);
        }
    }

    // test sampling without replacement based on range filter results,
    // but the sample size is less than the number of filtered results * sampleCountToCandidateCountRatio
    @Test
    public void testTypeFilterWithoutReplacementSmallSampleCount() {
        AbstractResult indexResult = new RangeResult(typeIndex, Arrays.asList(new RangeUnit(0, 3), new RangeUnit(6, 9)));
        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=2, replacement=False)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 100000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (float) numSamples;
            if (i != 1 && i != 6) {
                assertEquals(2.0F * 1 / 8, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }

    // test sampling with replacement based on no filter results
    @Test
    public void testSmallCandidateSize() {
        AbstractResult indexResult = new RangeResult(typeIndex, Arrays.asList(new RangeUnit(0, 3), new RangeUnit(6, 9)));
        String sampleMeta = "random_sampler(limit=9, replacement=False)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 100000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (float) numSamples;
            if (i != 1 && i != 6) {
                assertEquals(1.0F, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }

    // test sampling without replacement based on type filter results,
    // but the sample size is larger than the number of filtered results
    @Test
    public void testWeightFilterWithoutReplacement() throws Exception {
        AbstractResult indexResult = new RangeResult(weightIndex, Arrays.asList(new RangeUnit(0, 3), new RangeUnit(5, 8)));
        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=3, replacement=False)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 100000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (float) numSamples;
            if (i != 0 && i != 5) {
                assertEquals(3.0F / 8, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }

    // test sampling without replacement based on type filter results,
    // but the sample size is larger than the number of filtered results * sampleCountToCandidateCountRatio
    @Test
    public void testTypeFilterWithReplacement() throws Exception {
        AbstractResult indexResult = new RangeResult(weightIndex, Arrays.asList(new RangeUnit(0, 3), new RangeUnit(5, 8)));
        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=3, replacement=True)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 100000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (float) numSamples;
            if (i != 0 && i != 5) {
                assertEquals(3.0F / 8, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }

    // test sampling with replacement based on type filter and range filter results
    @Test
    public void testWeightTypeFilterWithoutReplacementSmallSampleCount() throws Exception {
        AbstractResult indexResult = new CommonResult(weightIndex, Arrays.asList(1, 2, 4, 6, 7, 9));

        // test sampling with replacement
        String sampleMeta = "random_sampler(limit=2, replacement=True)";
        AbstractSampler sampler = SamplerFactory.createSampler(new SampleCondition(sampleMeta), neighborDataset);

        int numSamples = 100000;
        int[] frequence = new int[10];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                frequence[index]++;
            }
        }
        for (int i = 0; i < 10; i++) {
            double empiricalProb = frequence[i] / (float) numSamples;
            if (i == 1 || i == 2 || i == 4 || i == 6 || i == 7 || i == 9) {
                assertEquals(2.0F * 1 / 6, empiricalProb, 0.01);
            } else {
                assertEquals(0, empiricalProb, 0.01);
            }
        }
    }
}
