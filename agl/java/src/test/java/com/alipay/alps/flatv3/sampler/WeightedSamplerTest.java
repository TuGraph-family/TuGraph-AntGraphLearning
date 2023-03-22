package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WeightedSamplerTest {
    private WeightedSampler sampler;
    private RangeIndex<String> rangeIndex = null;
    List<String> ids = new ArrayList<>();
    List<Float> weight = new ArrayList<>();
    List<Float> time = new ArrayList<>();

    @Before
    public void setUp() {
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(1.0F * i / 45);
            time.add(100 - 1.0F * i);
        }
    }

    @Test
    public void testSample_withSmallSampleSize_withoutReplacement() {
        rangeIndex = new RangeIndex<>("range_index:TIME:double,");
        rangeIndex.setNode2IDs(ids);
        rangeIndex.addAttributes("WEIGHT", weight);
        rangeIndex.addAttributes("TIME", time);
        rangeIndex.buildIndex();
        String sampleMeta = "weighted_sampler(by=index.WEIGHT, limit=5, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(0, 2)));
        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

    @Test
    public void testSample_AliasMethod() {
        rangeIndex = new RangeIndex<>("");
        rangeIndex.setNode2IDs(ids);
        rangeIndex.addAttributes("WEIGHT", weight);
        rangeIndex.addAttributes("TIME", time);
        rangeIndex.buildIndex();
        String sampleMeta = "weighted_sampler(by=index.WEIGHT, limit=2, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 9)));

        int numSamples = 1000000;
        int [] freqWithReplacement = new int[rangeIndex.originIndex.length];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = rangeIndex.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < freqWithReplacement.length; i++) {
            double empiricalProb = freqWithReplacement[i] / (double) numSamples;
            assertEquals(1.0 * i / 45, empiricalProb, 0.01);
        }

        sampleMeta = "weighted_sampler(by=index.WEIGHT, limit=2, replacement=false)";
        sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 9)));
        int [] freqWithoutReplacement = new int[rangeIndex.originIndex.length];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = rangeIndex.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0 * i / 45, empiricalProb, 0.01);
        }
    }

    @Test
    public void testSample_PrefixSum() {
        rangeIndex = new RangeIndex<>("range_index:TIME:double,");
        rangeIndex.setNode2IDs(ids);
        rangeIndex.addAttributes("WEIGHT", weight);
        rangeIndex.addAttributes("TIME", time);
        rangeIndex.buildIndex();
        String sampleMeta = "weighted_sampler(by=index.WEIGHT, limit=2, replacement=true)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 9)));

        int numSamples = 1000000;
        int [] freqWithReplacement = new int[rangeIndex.originIndex.length];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = rangeIndex.getNode2ID(index);
                freqWithReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < freqWithReplacement.length; i++) {
            double empiricalProb = freqWithReplacement[i] / (double) numSamples;
            assertEquals(1.0 * i / 45, empiricalProb, 0.01);
        }

        sampleMeta = "weighted_sampler(by=index.WEIGHT, limit=2, replacement=false)";
        sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 9)));
        int [] freqWithoutReplacement = new int[rangeIndex.originIndex.length];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = rangeIndex.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0 * i / 45, empiricalProb, 0.01);
        }
    }

    @Test
    public void testSample_OrderStatisticTree() {
        rangeIndex = new RangeIndex<>("range_index:TIME:double,");
        rangeIndex.setNode2IDs(ids);
        rangeIndex.addAttributes("WEIGHT", weight);
        rangeIndex.addAttributes("TIME", time);
        rangeIndex.buildIndex();
        String sampleMeta = "weighted_sampler(by=index.WEIGHT, limit=5, replacement=false)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 9)));

        sampleMeta = "weighted_sampler(by=index.WEIGHT, limit=2, replacement=false)";
        sampleCondition = new SampleCondition(sampleMeta);
        sampler = new WeightedSampler(sampleCondition, rangeIndex);
        indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 9)));

        int numSamples = 1000000;
        int [] freqWithoutReplacement = new int[rangeIndex.originIndex.length];
        for (int i = 0; i < numSamples; i++) {
            List<Integer> neighborIndex = sampler.sample(indexResult);
            for (int index : neighborIndex) {
                String id = rangeIndex.getNode2ID(index);
                freqWithoutReplacement[Integer.valueOf(id)]++;
            }
        }
        for (int i = 0; i < freqWithoutReplacement.length; i++) {
            double empiricalProb = freqWithoutReplacement[i] / (double) numSamples;
            assertEquals(1.0 * i / 45, empiricalProb, 0.01);
        }
    }
}
