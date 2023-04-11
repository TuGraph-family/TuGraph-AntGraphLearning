package com.alipay.alps.flatv3.index;


import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class FilterTest {
    private NeighborDataset neighborDataset;

    @Before
    public void setUp() {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        neighborDataset = new NeighborDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<Float> scoreList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("score", scoreList);
        List<Float> priceList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("price", priceList);
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributeList("node_type", typeList);
    }

    @Test
    public void testNoFilter() throws Exception {
        List<Object> seedType = Arrays.asList("item");
        // Create a hash index
        Filter filter = new Filter(null, "", neighborDataset);
        AbstractIndexResult indexResult = filter.filter(seedType);
        assertArrayEquals(Arrays.asList(0, 1, 2, 3, 4).toArray(), indexResult.getIndices().toArray());
    }

    // test type filter
    @Test
    public void testTypeFilter() throws Exception {
        List<Object> seedType = Arrays.asList("item");
        // add a hash index
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("hash_index:node_type:string");
        Filter filter = new Filter(indexMetas, "index.node_type in (user, shop)", neighborDataset);
        AbstractIndexResult indexResult = filter.filter(seedType);
        assertArrayEquals(Arrays.asList(1, 2, 4).toArray(), indexResult.getIndices().toArray());
    }

    // test range filter
    @Test
    public void testRangeFilter() throws Exception {
        List<Object> seedScore = Arrays.asList(0.1F);
        // add a range index
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("range_index:score:float");
        Filter filter = new Filter(indexMetas, "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1", neighborDataset);
        AbstractIndexResult indexResult = filter.filter(seedScore);
        assertArrayEquals(Arrays.asList(2, 3).toArray(), indexResult.getIndices().toArray());
    }

    // test range filter and type filter
    @Test
    public void testRangeAndTypeFilter() throws Exception {
        List<Object> seedScore = Arrays.asList(0.1F);
        // add a range index and a hash index
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("range_index:score:float");
        indexMetas.add("hash_index:node_type:string");
        Filter filter = new Filter(indexMetas,
                "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1 and index.node_type in (user, shop)",
                neighborDataset);
        AbstractIndexResult indexResult = filter.filter(seedScore);
        assertArrayEquals(Arrays.asList(2).toArray(), indexResult.getIndices().toArray());
    }

    // test two range filters
    @Test
    public void testTwoRangeFilter() throws Exception {
        List<Object> seedData = Arrays.asList(0.1F, 0.2F);
        // add a score range index and a price range index
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("range_index:score:float");
        indexMetas.add("range_index:price:float");
        Filter filter = new Filter(indexMetas,
                "index.score - seed.1 >= 0.2 and index.score < 0.4 + seed.1 and index.price - seed.2 >= 0.2 and index.price < 0.4  + seed.2",
                neighborDataset);
        AbstractIndexResult indexResult = filter.filter(seedData);
        assertArrayEquals(Arrays.asList(3).toArray(), indexResult.getIndices().toArray());
    }
}
