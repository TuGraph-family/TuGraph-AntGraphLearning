package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class TopKSamplerTest {
    private NeighborDataset neighborDataset = null;
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
        AbstractIndexResult indexResult = new RangeIndexResult(typeIndex, Collections.singletonList(new Range(0, 2)));

        String sampleMeta = "topk(by=weight, limit=5, reverse=false, replacement=False)";
        TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

   @Test
   public void testNoFilterSampleByTimestampReverse() {
       AbstractIndexResult indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(new Range(0, 9)));

       String sampleMeta = "topk(by=weight, limit=5, reverse=true, replacement=false)";
       TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

       List<Integer> actual = sampler.sample(indexResult);
       List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);
       assertEquals(expected, actual);
   }

   @Test
   public void testWeightFilterSampleByTimestamp() {
       AbstractIndexResult indexResult = new RangeIndexResult(weightIndex, Collections.singletonList(new Range(1, 8)));

       String sampleMeta = "topk(by=time, limit=5, reverse=false, replacement=false)";
       TopKSampler<Long> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

       List<Integer> actual = sampler.sample(indexResult);
       List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);

       assertEquals(expected, actual);
   }

   @Test
   public void testTimeFilterSampleByTimestamp() {
       AbstractIndexResult indexResult = new RangeIndexResult(timeIndex, Collections.singletonList(new Range(1, 8)));

       String sampleMeta = "topk(by=time, limit=5, reverse=false, replacement=false)";
       TopKSampler<Long>  sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

       List<Integer> actual = sampler.sample(indexResult);
       List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);

       assertEquals(expected, actual);
   }

   @Test
   public void testTypeTimeFilterSampleByWeight() {
       AbstractIndexResult indexResult = new CommonIndexResult(typeIndex, Arrays.asList(0, 2, 4, 5, 6, 9));

       String sampleMeta = "topk(by=weight, limit=3, reverse=true, replacement=false)";
       TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

       List<Integer> actual = sampler.sample(indexResult);
       List<Integer> expected = Arrays.asList(8, 7, 6);
       assertEquals(expected, actual);
   }

   @Test
   public void testTypeWeightFilterSampleByWeight() {
       AbstractIndexResult indexResult = new CommonIndexResult(typeIndex, Arrays.asList(0, 2, 4, 5, 6, 9));

       String sampleMeta = "topk(by=weight, limit=3, reverse=false, replacement=false)";
       TopKSampler<Float> sampler = new TopKSampler<>(new SampleCondition(sampleMeta), neighborDataset);

       List<Integer> actual = sampler.sample(indexResult);
       List<Integer> expected = Arrays.asList(8, 7, 6);
       assertEquals(expected, actual);
   }
}
