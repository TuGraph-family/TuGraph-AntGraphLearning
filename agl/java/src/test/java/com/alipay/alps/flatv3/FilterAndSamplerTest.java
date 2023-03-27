package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.RangeIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.Test;

public class FilterAndSamplerTest {
    @Test
    public void test1() throws Exception {
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11";
        String indexMeta = "range_index:time:long";
        String sampleCond = "topk(by=time, limit=2)";

        FilterAndSampler filterAndSampler = new FilterAndSampler("", filterCond, sampleCond);

        List<String> ids = new ArrayList<>();
        List<Long> timestamp = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            timestamp.add(2L * i);
        }
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", timestamp);
        RangeIndex weightIndex = new RangeIndex(indexMeta, neighborDataset);
        List<BaseIndex> indexList = new ArrayList<>();
        indexList.add(weightIndex);

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }
        List<List<Integer>> result = filterAndSampler.process(seedIds, seedAttrs, indexList, null, null);
        List<List<Integer>> expected = new ArrayList<>(seedIds.size());
        expected.add(Arrays.asList(1, 2));
        expected.add(Arrays.asList(2, 3));
        expected.add(Arrays.asList(2, 3));
        expected.add(Arrays.asList(3, 4));
        expected.add(Arrays.asList(3, 4));
        assertEquals(result, expected);
    }

    @Test
    public void test2() throws Exception {
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11 OR index.type in ('item', 'user')";
        String sampleCond = "topk(by=time, limit=3)";
        FilterAndSampler filterAndSampler = new FilterAndSampler("", filterCond, sampleCond);

        List<String> ids = new ArrayList<>();
        List<Long> timestamp = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            timestamp.add(2L * i);
            indices.add(i);
        }
        List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        neighborDataset.addAttributes("time", timestamp);
        neighborDataset.addAttributes("type", types);

        BaseIndex timeIndex = new RangeIndex("range_index:time:long", neighborDataset);
        BaseIndex typeIndex = new HashIndex("hash_index:type:string", neighborDataset);

        List<BaseIndex> indexList = new ArrayList<>();
        indexList.add(timeIndex);
        indexList.add(typeIndex);

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }
        List<List<Integer>> result = filterAndSampler.process(seedIds, seedAttrs, indexList, null, null);

        Collections.shuffle(indices);
        for (int i = 0; i < 10; i++) {
            while (indices.get(i) != i) {
                int newIdx = indices.get(i);
                Collections.swap(ids, i, newIdx);
                Collections.swap(timestamp, i, newIdx);
                Collections.swap(types, i, newIdx);
                Collections.swap(indices, i, newIdx);
            }
        }
        NeighborDataset<String> newNeighborDataset = new NeighborDataset<>(ids, null);
        newNeighborDataset.addAttributes("time", timestamp);
        newNeighborDataset.addAttributes("type", types);
        BaseIndex newTimeIndex = new RangeIndex("range_index:time:long", newNeighborDataset);
        BaseIndex newTypeIndex = new HashIndex("hash_index:type:string", newNeighborDataset);
        List<BaseIndex> newIndexList = new ArrayList<>();
        newIndexList.add(newTimeIndex);
        newIndexList.add(newTypeIndex);
        List<List<Integer>> newResult = filterAndSampler.process(seedIds, seedAttrs, indexList, null, null);
        assertEquals(result, newResult);
    }
}