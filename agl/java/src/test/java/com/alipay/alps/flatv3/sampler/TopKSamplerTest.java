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

public class TopKSamplerTest {
    private HeteroDataset neighborDataset = null;
    private BaseIndex baseIndex = null;
    private BaseIndex typeIndex = null;
    private BaseIndex timeIndex = null;
    private BaseIndex weightIndex = null;

    @Before
    public void setUp() {
        List<String> ids = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        List<Float> weights = Arrays.asList(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F);
        List<Long> times = Arrays.asList(100L, 99L, 98L, 97L, 96L, 95L, 94L, 93L, 92L, 91L);
        List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");
        neighborDataset = new HeteroDataset(ids.size());
        neighborDataset.addAttributeList("weight", weights);
        neighborDataset.addAttributeList("time", times);
        neighborDataset.addAttributeList("type", types);
        baseIndex = new IndexFactory().createIndex("", neighborDataset);
        typeIndex = new IndexFactory().createIndex("range_index:type:string", neighborDataset);
        timeIndex = new IndexFactory().createIndex("range_index:time:long", neighborDataset);
        weightIndex = new IndexFactory().createIndex("range_index:weight:float", neighborDataset);
    }

    @Test
    public void testSmallCandidateSize() {
        AbstractResult indexResult = new RangeResult(typeIndex, Collections.singletonList(new RangeUnit(0, 3)));

        String sampleMeta = "topk(by=weight, limit=5, reverse=false, replacement=False)";
        TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 3, 5, 8);
        assertEquals(expected, actual);
    }

    @Test
    public void testNoFilterSampleByTimestampReverse() {
        AbstractResult indexResult = new RangeResult(baseIndex, Collections.singletonList(new RangeUnit(0, 9)));

        String sampleMeta = "topk(by=weight, limit=5, reverse=true, replacement=false)";
        TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(9, 8, 7, 6, 5);
        assertEquals(expected, actual);
    }

    @Test
    public void testWeightFilterSampleByTimestamp() {
        AbstractResult indexResult = new RangeResult(weightIndex, Collections.singletonList(new RangeUnit(1, 8)));

        String sampleMeta = "topk(by=time, limit=5, reverse=false, replacement=false)";
        TopKSampler<Long> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);

        assertEquals(expected, actual);
    }

    @Test
    public void testTimeFilterSampleByTimestamp() {
        AbstractResult indexResult = new RangeResult(timeIndex, Collections.singletonList(new RangeUnit(1, 7)));

        String sampleMeta = "topk(by=time, limit=5, reverse=false, replacement=false)";
        TopKSampler<Long> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);

        assertEquals(expected, actual);
    }


    @Test
    public void testTypeTimeFilterSampleByWeight() {
        AbstractResult indexResult = new CommonResult(typeIndex, Arrays.asList(0, 2, 4, 5, 6, 9));

        String sampleMeta = "topk(by=weight, limit=3, reverse=true, replacement=false)";
        TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(9, 6, 5);
        assertEquals(expected, actual);
    }

    @Test
    public void testTypeWeightFilterSampleByWeight() {
        AbstractResult indexResult = new CommonResult(typeIndex, Arrays.asList(0, 2, 4, 5, 6, 9));

        String sampleMeta = "topk(by=weight, limit=3, reverse=false, replacement=false)";
        TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 2, 4);
        assertEquals(expected, actual);
    }
}
