package com.alipay.alps.flatv3.sampler;

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
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TopKSamplerTest {
    private TopKSampler sampler;
    private RangeIndex rangeIndex = null;

    @Before
    public void setUp() {
        List<String> ids = new ArrayList<>();
        List<Float> weight = new ArrayList<>();
        List<Long> time = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            weight.add(1.0F * i);
            time.add(100L - i);
        }
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", time);
        neighborDataset.addAttributes("weight", weight);
        rangeIndex = new RangeIndex("range_index:weight:double", neighborDataset);
    }

    @Test
    public void testSample_withSmallSampleSize() {
        String sampleMeta = "topk(by=weight, limit=5, reverse=true, replacement=False)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        sampler = new TopKSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(0, 2)));
        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

    @Test
    public void testSample_withLargeSampleSize() {
        String sampleMeta = "topk(by=weight, limit=5, reverse=false, replacement=False)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        sampler = new TopKSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 8)));

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);

        assertEquals(expected, actual);
    }

    @Test
    public void testSample_withOtherColumn() {
        String sampleMeta = "topk(by=time, limit=5, reverse=true, replacement=False)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        sampler = new TopKSampler(sampleCondition, rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 8)));

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5);

        assertEquals(expected, actual);
    }
}
