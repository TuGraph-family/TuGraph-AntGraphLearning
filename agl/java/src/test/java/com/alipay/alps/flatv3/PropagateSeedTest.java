package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.RangeIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class PropagateSeedTest {
    private NeighborDataset neighborDataset;
    List<Integer> indices = new ArrayList<>();
    List<String> ids = new ArrayList<>();
    List<Long> times = new ArrayList<>();
    List<String> types = Arrays.asList("item", "shop", "user", "item", "user", "item", "shop", "user", "item", "user");

    @Before
    public void setUp() {
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            times.add(2L * i);
            indices.add(i);
        }
        neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("time", times);
        neighborDataset.addAttributeList("type", types);
    }

    @Test
    public void testNoFilterTopkSampler() throws Exception {
        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }

        String filterCond = "";
        String sampleCond = "topk(by=time, limit=2)";
        PropagateSeed propagateSeed = new PropagateSeed("", null, neighborDataset, filterCond, sampleCond);
        List<List<Integer>> result = propagateSeed.process(seedIds, seedAttrs, null, null);
        List<List<Integer>> expected = new ArrayList<>(seedIds.size());
        expected.add(Arrays.asList(0, 1));
        expected.add(Arrays.asList(0, 1));
        expected.add(Arrays.asList(0, 1));
        expected.add(Arrays.asList(0, 1));
        expected.add(Arrays.asList(0, 1));
        assertEquals(expected, result);
    }

    @Test
    public void testRangeFilterTopkSampler() throws Exception {
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("range_index:time:long");

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }

        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11";
        String sampleCond = "topk(by=time, limit=2)";
        PropagateSeed propagateSeed = new PropagateSeed("", indexMetas, neighborDataset, filterCond, sampleCond);
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
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("range_index:time:long");
        indexMetas.add("hash_index:type:string");

        List<List<Object>> seedAttrs = new ArrayList<>();
        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }

        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11 OR index.type in ('item', 'user')";
        String sampleCond = "topk(by=time, limit=3)";
        PropagateSeed propagateSeed = new PropagateSeed("", indexMetas, neighborDataset, filterCond, sampleCond);
        List<List<Integer>> result = propagateSeed.process(seedIds, seedAttrs, null, null);
        List<List<String>> chosenNeighbors = new ArrayList<>();
        for (List<Integer> neighborsOfSeed : result) {
            List<String> neighborIds = new ArrayList<>();
            for (Integer neighborIdx : neighborsOfSeed) {
                neighborIds.add(ids.get(neighborIdx));
            }
            chosenNeighbors.add(neighborIds);
        }
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
        NeighborDataset newNeighborDataset = new NeighborDataset(ids.size());
        newNeighborDataset.addAttributeList("new_time", times);
        newNeighborDataset.addAttributeList("new_type", types);

        String newFilterCond = "index.new_time - seed.1 >= 0.5 AND index.new_time <= seed.1 + 11 OR index.new_type in ('item', 'user')";
        String newSampleCond = "topk(by=new_time, limit=3)";
        List<String> newIndexMetas = new ArrayList<>();
        newIndexMetas.add("range_index:new_time:long");
        newIndexMetas.add("hash_index:new_type:string");
        IndexFactory.clear();
        PropagateSeed newPropagateSeed = new PropagateSeed("", newIndexMetas, newNeighborDataset, newFilterCond, newSampleCond);
        List<List<Integer>> newResult = newPropagateSeed.process(seedIds, seedAttrs, null, null);
        List<List<String>> newChosenNeighbors = new ArrayList<>();
        for (List<Integer> neighborsOfSeed : newResult) {
            List<String> neighborIds = new ArrayList<>();
            for (Integer neighborIdx : neighborsOfSeed) {
                neighborIds.add(ids.get(neighborIdx));
            }
            newChosenNeighbors.add(neighborIds);
        }
        assertEquals(chosenNeighbors, newChosenNeighbors);
    }
}