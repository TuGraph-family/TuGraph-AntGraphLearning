package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.index.NeighborDataset;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
        // Create the list of seed IDs as strings.
        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");

        // Create the list of seed attributes as lists of objects.
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i + 1) * 1L));
        }

        // Create the filter condition.
        String filterCond = "";

        // Create the sample condition.
        String sampleCond = "topk(by=time, limit=2)";

        // Create the PropagateSeed object.
        PropagateSeed propagateSeed = new PropagateSeed("", null, neighborDataset, filterCond, sampleCond);

        // Run the algorithm.
        List<List<Integer>> result = propagateSeed.process(seedIds, seedAttrs, null, null);

        // Create the list of expected results.
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
        // add 1 range index meta
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("range_index:time:long");

        // create seed ids
        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");

        // create seed attrs
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i + 1) * 1L));
        }

        // create filter condition
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11";

        // create sample condition
        String sampleCond = "topk(by=time, limit=2)";

        // create propagate seed
        PropagateSeed propagateSeed = new PropagateSeed("", indexMetas, neighborDataset, filterCond, sampleCond);

        // process propagate seed
        List<List<Integer>> result = propagateSeed.process(seedIds, seedAttrs, null, null);

        // create expected result
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
        // add a range index meta and a hash index meta
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("range_index:time:long");
        indexMetas.add("hash_index:type:string");

        // the attributes of the seeds
        List<List<Object>> seedAttrs = new ArrayList<>();
        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i + 1) * 1L));
        }

        // the filter condition for the propagation
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11 OR index.type in ('item', 'user')";

        // the sample condition for the propagation
        String sampleCond = "topk(by=time, limit=3)";

        PropagateSeed propagateSeed = new PropagateSeed("", indexMetas, neighborDataset, filterCond, sampleCond);

        // the result of the propagation
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