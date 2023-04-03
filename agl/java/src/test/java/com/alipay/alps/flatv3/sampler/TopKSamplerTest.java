package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
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

public class TopKSamplerTest {
    List<String> ids = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private List<Float> weights = Arrays.asList(1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F);
    private List<Long> times = Arrays.asList(100L, 99L, 98L, 97L, 96L, 95L, 94L, 93L, 92L, 91L);
    private List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");

    @Test
    public void testSmallCandidateSize() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("weight", weights);
        neighborDataset.addAttributes("type", types);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        BaseIndex typeIndex = new HashIndex("hash_index:type:string", neighborDataset);
        indexMap.put(typeIndex.getIndexType(), typeIndex);

        String sampleMeta = "topk(by=weight, limit=5, reverse=false, replacement=False)";
        TopKSampler sampler = new TopKSampler(new SampleCondition(sampleMeta), indexMap);

        IndexResult indexResult = new RangeIndexResult(typeIndex, Collections.singletonList(new Range(0, 2)));
        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(0, 1, 2);
        assertEquals(expected, actual);
    }

    @Test
    public void testNoFilterSampleByTimestampReverse() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", times);
        neighborDataset.addAttributes("weight", weights);
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);

        String sampleMeta = "topk(by=weight, limit=5, reverse=true, replacement=false)";
        TopKSampler sampler = new TopKSampler(new SampleCondition(sampleMeta), baseIndex);
        IndexResult indexResult = new RangeIndexResult(baseIndex, Collections.singletonList(new Range(0, 9)));

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);
        assertEquals(expected, actual);
    }

    @Test
    public void testWeightFilterSampleByTimestamp() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", times);
        neighborDataset.addAttributes("weight", weights);
        BaseIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);

        String sampleMeta = "topk(by=time, limit=5, reverse=false, replacement=false)";
        TopKSampler sampler = new TopKSampler(new SampleCondition(sampleMeta), rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 8)));

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);

        assertEquals(expected, actual);
    }

    @Test
    public void testTimeFilterSampleByTimestamp() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", times);
        neighborDataset.addAttributes("weight", weights);
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);

        String sampleMeta = "topk(by=time, limit=5, reverse=false, replacement=false)";
        TopKSampler sampler = new TopKSampler(new SampleCondition(sampleMeta), rangeIndex);
        IndexResult indexResult = new RangeIndexResult(rangeIndex, Collections.singletonList(new Range(1, 8)));

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6, 5, 4);

        assertEquals(expected, actual);
    }

    @Test
    public void testTypeTimeFilterSampleByWeight() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("type", types);
        neighborDataset.addAttributes("time", times);
        neighborDataset.addAttributes("weight", weights);
        BaseIndex typeIndex = new HashIndex("range_index:type:string", neighborDataset);
        BaseIndex rangeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(typeIndex.getIndexType(), typeIndex);
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);

        String sampleMeta = "topk(by=weight, limit=3, reverse=true, replacement=false)";
        TopKSampler sampler = new TopKSampler(new SampleCondition(sampleMeta), rangeIndex);
        IndexResult indexResult = new CommonIndexResult(indexMap, Arrays.asList(0, 2, 4, 5, 6, 9));

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6);
        assertEquals(expected, actual);
    }

    @Test
    public void testTypeWeightFilterSampleByWeight() {
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("type", types);
        neighborDataset.addAttributes("time", times);
        neighborDataset.addAttributes("weight", weights);
        BaseIndex typeIndex = new HashIndex("range_index:type:string", neighborDataset);
        BaseIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(typeIndex.getIndexType(), typeIndex);
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);

        String sampleMeta = "topk(by=weight, limit=3, reverse=false, replacement=false)";
        TopKSampler sampler = new TopKSampler(new SampleCondition(sampleMeta), rangeIndex);
        IndexResult indexResult = new CommonIndexResult(indexMap, Arrays.asList(0, 2, 4, 5, 6, 9));

        List<Integer> actual = sampler.sample(indexResult);
        List<Integer> expected = Arrays.asList(8, 7, 6);
        assertEquals(expected, actual);
    }
}
