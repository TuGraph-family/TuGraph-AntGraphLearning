package com.alipay.alps.flatv3.index;


import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterTest {
    @Test
    public void testNoFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributeList("node_type", typeList);

        List<Object> seedType = Arrays.asList("item");

        // Create a hash index
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put("", baseIndex);
        Filter filter = new Filter(indexMap, "");
        AbstractIndexResult indexResult = filter.filter(seedType);
        assertArrayEquals(Arrays.asList(0, 1, 2, 3, 4).toArray(), indexResult.getIndices().toArray());
    }

    // test type filter
    @Test
    public void testTypeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributeList("node_type", typeList);

        List<Object> seedType = Arrays.asList("item");

        // Create a hash index
        Map<String, BaseIndex> indexMap = new HashMap<>();
        BaseIndex typeIndex = new HashIndex("hash_index:node_type:string", neighborDataset);
        indexMap.put(typeIndex.getIndexColumn(), typeIndex);
        Filter filter = new Filter(indexMap, "index.node_type in (user, shop)");
        AbstractIndexResult indexResult = filter.filter(seedType);
        assertArrayEquals(Arrays.asList(1, 2, 4).toArray(), indexResult.getIndices().toArray());
    }

    // test range filter
    @Test
    public void testRangeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<Float> scoreList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("score", scoreList);

        List<Object> seedScore = Arrays.asList(0.1F);

        // Create a hash index
        BaseIndex baseIndex = new RangeIndex("range_index:score:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(baseIndex.getIndexColumn(), baseIndex);
        Filter filter = new Filter(indexMap, "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1");
        AbstractIndexResult indexResult = filter.filter(seedScore);
        assertArrayEquals(Arrays.asList(2, 3).toArray(), indexResult.getIndices().toArray());
    }

    // test range filter and type filter
    @Test
    public void testRangeAndTypeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<Float> scoreList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("score", scoreList);

        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributeList("node_type", typeList);

        List<Object> seedScore = Arrays.asList(0.1F);

        // Create a hash index
        BaseIndex rangeIndex = new RangeIndex("range_index:score:float", neighborDataset);
        BaseIndex typeIndex = new HashIndex("hash_index:node_type:string", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexColumn(), rangeIndex);
        indexMap.put(typeIndex.getIndexColumn(), typeIndex);
        Filter filter = new Filter(indexMap, "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1 and index.node_type in (user, shop)");
        AbstractIndexResult indexResult = filter.filter(seedScore);
        assertArrayEquals(Arrays.asList(2).toArray(), indexResult.getIndices().toArray());
    }

    // test two range filters
    @Test
    public void testTwoRangeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<Float> scoreList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("score", scoreList);

        List<Float> priceList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("price", priceList);

        List<Object> seedData = Arrays.asList(0.1F, 0.2F);

        // Create a filter
        BaseIndex rangeIndex = new RangeIndex("range_index:score:float", neighborDataset);
        BaseIndex rangeIndex2 = new RangeIndex("range_index:price:float", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexColumn(), rangeIndex);
        indexMap.put(rangeIndex2.getIndexColumn(), rangeIndex2);
        Filter filter = new Filter(indexMap, "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1 and index.price - seed.2 >= 0.2 and index.price < 0.4  + seed.2");
        AbstractIndexResult indexResult = filter.filter(seedData);
        assertArrayEquals(Arrays.asList(3).toArray(), indexResult.getIndices().toArray());
    }
}
