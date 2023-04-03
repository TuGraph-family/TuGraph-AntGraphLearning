package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.RangeIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.junit.Test;

public class PropagateSeedTest {
    @Test
    public void testNoFilterTopkSampler() throws Exception {
        String filterCond = "";
        String sampleCond = "topk(by=time, limit=2)";

        List<String> ids = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            times.add(2L * i);
        }
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", times);
        RangeIndex weightIndex = new RangeIndex("", neighborDataset);

        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(weightIndex.getIndexColumn(), weightIndex);

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }


        PropagateSeed propagateSeed = new PropagateSeed("", indexes, filterCond, sampleCond);
        List<List<Integer>> result = propagateSeed.process(seedIds, seedAttrs, null, null);
        List<List<Integer>> expected = new ArrayList<>(seedIds.size());
        expected.add(Arrays.asList(1, 2));
        expected.add(Arrays.asList(2, 3));
        expected.add(Arrays.asList(2, 3));
        expected.add(Arrays.asList(3, 4));
        expected.add(Arrays.asList(3, 4));
        assertEquals(result, expected);
    }

    @Test
    public void testRangeFilterTopkSampler() throws Exception {
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11";
        String sampleCond = "topk(by=time, limit=2)";

        List<String> ids = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            times.add(2L * i);
        }
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", times);
        RangeIndex weightIndex = new RangeIndex("range_index:time:long", neighborDataset);

        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(weightIndex.getIndexColumn(), weightIndex);

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }


        PropagateSeed propagateSeed = new PropagateSeed("", indexes, filterCond, sampleCond);
        List<List<Integer>> result = propagateSeed.process(seedIds, seedAttrs, null, null);
        List<List<Integer>> expected = new ArrayList<>(seedIds.size());
        expected.add(Arrays.asList(1, 2));
        expected.add(Arrays.asList(2, 3));
        expected.add(Arrays.asList(2, 3));
        expected.add(Arrays.asList(3, 4));
        expected.add(Arrays.asList(3, 4));
        assertEquals(result, expected);
    }

    @Test
    public void testRangeTypeFilterTopkSampler() throws Exception {
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11 OR index.type in ('item', 'user')";
        String sampleCond = "topk(by=time, limit=3)";

        List<String> ids = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            times.add(2L * i);
            indices.add(i);
        }
        List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", times);
        neighborDataset.addAttributes("type", types);

        BaseIndex timeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        BaseIndex typeIndex = new HashIndex("hash_index:type:string", neighborDataset);

        Map<String, BaseIndex> indexes = new HashMap<>();
        indexes.put(timeIndex.getIndexColumn(), timeIndex);
        indexes.put(typeIndex.getIndexColumn(), typeIndex);

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }
        PropagateSeed propagateSeed = new PropagateSeed("", indexes, filterCond, sampleCond);
        List<List<Integer>> result = propagateSeed.process(seedIds, seedAttrs, null, null);

        Collections.shuffle(indices);
        for (int i = 0; i < 10; i++) {
            while (indices.get(i) != i) {
                int newIdx = indices.get(i);
                Collections.swap(ids, i, newIdx);
                Collections.swap(times, i, newIdx);
                Collections.swap(types, i, newIdx);
                Collections.swap(indices, i, newIdx);
            }
        }
        NeighborDataset<String> newNeighborDataset = new NeighborDataset<>(ids, null);
        newNeighborDataset.addAttributes("time", times);
        newNeighborDataset.addAttributes("type", types);
        BaseIndex newTimeIndex = new RangeIndex("range_index:time:long", newNeighborDataset);
        BaseIndex newTypeIndex = new HashIndex("hash_index:type:string", newNeighborDataset);
        Map<String, BaseIndex> newIndexes = new HashMap<>();
        newIndexes.put(newTimeIndex.getIndexColumn(), newTimeIndex);
        newIndexes.put(newTypeIndex.getIndexColumn(), newTypeIndex);
        PropagateSeed newPropagateSeed = new PropagateSeed("", newIndexes, filterCond, sampleCond);
        List<List<Integer>> newResult = newPropagateSeed.process(seedIds, seedAttrs, null, null);
        assertEquals(result, newResult);
    }
}