package com.alipay.alps.flatv3.index;


import com.alipay.alps.flatv3.filter_exp.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.CmpExpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterTest {
    @Test
    public void testNoFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> edgeIDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset<Integer> neighborDataset = new NeighborDataset<>(node2IDs, edgeIDs);

        // Add some attributes to the neighbor dataset
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributes("node_type", typeList);

        List<Object> seedType = Arrays.asList("item");

        // Create a hash index
        BaseIndex baseIndex = new BaseIndex("", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put("", baseIndex);
        Filter filter = new Filter(indexMap, "");
        IndexResult indexResult = filter.filter(seedType);
        assertArrayEquals(Arrays.asList(0, 1, 2, 3, 4).toArray(), indexResult.getIndices().toArray());
    }
    
    // test type filter
    @Test
    public void testTypeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> edgeIDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset<Integer> neighborDataset = new NeighborDataset<>(node2IDs, edgeIDs);

        // Add some attributes to the neighbor dataset
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributes("node_type", typeList);

        List<Object> seedType = Arrays.asList("item");

        // Create a hash index
        Map<String, BaseIndex> indexMap = new HashMap<>();
        BaseIndex typeIndex = new HashIndex("hash_index:node_type:string", neighborDataset);
        indexMap.put(typeIndex.getIndexType(), typeIndex);
        Filter filter = new Filter(indexMap, "index.node_type in (user, shop)");
        IndexResult indexResult = filter.filter(seedType);
        assertArrayEquals(Arrays.asList(1, 2, 4).toArray(), indexResult.getIndices().toArray());
    }

    // test range filter
    @Test
    public void testRangeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> edgeIDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset<Integer> neighborDataset = new NeighborDataset<>(node2IDs, edgeIDs);

        // Add some attributes to the neighbor dataset
        List<Double> scoreList = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        neighborDataset.addAttributes("score", scoreList);

        List<Object> seedScore = Arrays.asList(0.1);

        // Create a hash index
        BaseIndex baseIndex = new RangeIndex("range_index:score:double", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put("range_index:score:double", baseIndex);
        Filter filter = new Filter(indexMap, "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1");
        IndexResult indexResult = filter.filter(seedScore);
        assertArrayEquals(Arrays.asList(2, 3).toArray(), indexResult.getIndices().toArray());
    }

    // test range filter and type filter
    @Test
    public void testRangeAndTypeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> edgeIDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset<Integer> neighborDataset = new NeighborDataset<>(node2IDs, edgeIDs);

        // Add some attributes to the neighbor dataset
        List<Double> scoreList = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        neighborDataset.addAttributes("score", scoreList);

        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributes("node_type", typeList);

        List<Object> seedScore = Arrays.asList(0.1);

        // Create a hash index
        BaseIndex rangeIndex = new RangeIndex("range_index:score:double", neighborDataset);
        BaseIndex typeIndex = new HashIndex("hash_index:node_type:string", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);
        indexMap.put(typeIndex.getIndexType(), typeIndex);
        Filter filter = new Filter(indexMap, "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1 and index.node_type in (user, shop)");
        IndexResult indexResult = filter.filter(seedScore);
        assertArrayEquals(Arrays.asList(2).toArray(), indexResult.getIndices().toArray());
    }

    // test two range filters
    @Test
    public void testTwoRangeFilter() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> edgeIDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset<Integer> neighborDataset = new NeighborDataset<>(node2IDs, edgeIDs);

        // Add some attributes to the neighbor dataset
        List<Double> scoreList = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        neighborDataset.addAttributes("score", scoreList);

        List<Double> priceList = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        neighborDataset.addAttributes("price", priceList);

        List<Object> seedData = Arrays.asList(0.1, 0.2);

        // Create a filter
        BaseIndex rangeIndex = new RangeIndex("range_index:score:double", neighborDataset);
        BaseIndex rangeIndex2 = new RangeIndex("range_index:price:double", neighborDataset);
        Map<String, BaseIndex> indexMap = new HashMap<>();
        indexMap.put(rangeIndex.getIndexType(), rangeIndex);
        indexMap.put(rangeIndex2.getIndexType(), rangeIndex2);
        Filter filter = new Filter(indexMap, "index.score - seed.1 >= 0.2 and index.score < 0.4  + seed.1 and index.price - seed.2 >= 0.2 and index.price < 0.4  + seed.2");
        IndexResult indexResult = filter.filter(seedData);
        assertArrayEquals(Arrays.asList(3).toArray(), indexResult.getIndices().toArray());
    }
}
