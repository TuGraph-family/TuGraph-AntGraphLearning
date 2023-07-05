package com.alipay.alps.flatv3.filter;

import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HeteroDataset;
import com.alipay.alps.flatv3.index.IndexFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;

public class FilterTest {
    private HeteroDataset neighborDataset;

    @Before
    public void setUp() {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        neighborDataset = new HeteroDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<Float> scoreList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("score", scoreList);
        List<Float> priceList = Arrays.asList(0.1F, 0.2F, 0.3F, 0.4F, 0.5F);
        neighborDataset.addAttributeList("price", priceList);
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributeList("node_type", typeList);
    }
//
//    @Test
//    public void testNoFilter() throws Exception {
//        List<String> seedIds = Arrays.asList("1");
//        HeteroDataset seedAttrs = new HeteroDataset(seedIds.size());
//        // Create a hash index
//        Filter filter = new Filter("");
//        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(null, neighborDataset);
//        AbstractResult indexResult = filter.filter(0, seedAttrs, neighborDataset, indexMap);
//        assertArrayEquals(Arrays.asList(0, 1, 2, 3, 4).toArray(), indexResult.getIndices().toArray());
//    }
//
    // test type filter
    @Test
    public void testTypeFilter() throws Exception {
        List<String> seedIds = Arrays.asList("1");
        HeteroDataset seedAttrs = new HeteroDataset(seedIds.size());
        // add a hash index
        List<String> indexMetas = new ArrayList<>();
        indexMetas.add("hash_index:node_type:string");
        Filter filter = new Filter("index.node_type in (user)");
        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
        AbstractResult indexResult = filter.filter(0, seedAttrs, neighborDataset, indexMap);
        assertArrayEquals(Arrays.asList(1, 2, 4).toArray(), indexResult.getIndices().toArray());
    }
//
//    // test range filter
//    @Test
//    public void testRangeFilter() throws Exception {
//        List<Object> seedScore = Arrays.asList(0.1F);
//        HeteroDataset seedAttrs = new HeteroDataset(seedScore.size());
//        seedAttrs.addAttributeList("score", seedScore);
//        // add a range index
//        List<String> indexMetas = new ArrayList<>();
//        indexMetas.add("range_index:score:float");
//        Filter filter = new Filter("index.score - seed.score >= 0.2 and index.score < 0.4  + seed.score");
//        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
//        AbstractResult indexResult = filter.filter(0, seedAttrs, neighborDataset, indexMap);
//        assertArrayEquals(Arrays.asList(2, 3).toArray(), indexResult.getIndices().toArray());
//    }
//
//    // test range filter with negative value
//    @Test
//    public void testRangeFilterNegativeVal() throws Exception {
//        List<Object> seedScore = Arrays.asList(0.3F);
//        HeteroDataset seedAttrs = new HeteroDataset(seedScore.size());
//        seedAttrs.addAttributeList("score", seedScore);
//        // add a range index
//        List<String> indexMetas = new ArrayList<>();
//        indexMetas.add("range_index:score:float");
//        Filter filter = new Filter("index.score - seed.score <= -0.1");
//        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
//        AbstractResult indexResult = filter.filter(0, seedAttrs, neighborDataset, indexMap);
//        assertArrayEquals(Arrays.asList(0, 1).toArray(), indexResult.getIndices().toArray());
//    }
//
//    // test range filter and type filter
//    @Test
//    public void testRangeAndTypeFilter() throws Exception {
//        List<Object> seedScore = Arrays.asList(0.1F);
//        HeteroDataset seedAttrs = new HeteroDataset(seedScore.size());
//        seedAttrs.addAttributeList("score", seedScore);
//        // add a range index and a hash index
//        List<String> indexMetas = new ArrayList<>();
//        indexMetas.add("range_index:score:float");
//        indexMetas.add("hash_index:node_type:string");
//        Filter filter = new Filter(
//                "index.score - seed.score >= 0.2 and index.score < 0.4  + seed.score and index.node_type in (user, shop)");
//        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
//        AbstractResult indexResult = filter.filter(0, seedAttrs, neighborDataset, indexMap);
//        assertArrayEquals(Arrays.asList(2).toArray(), indexResult.getIndices().toArray());
//    }
//
//    // test two range filters
//    @Test
//    public void testTwoRangeFilter() throws Exception {
//        List<Object> seedScore = Arrays.asList(0.1F);
//        List<Object> seedPrice = Arrays.asList(0.2F);
//        HeteroDataset seedAttrs = new HeteroDataset(seedScore.size());
//        seedAttrs.addAttributeList("score", seedScore);
//        seedAttrs.addAttributeList("price", seedPrice);
//        // add a score range index and a price range index
//        List<String> indexMetas = new ArrayList<>();
//        indexMetas.add("range_index:score:float");
//        indexMetas.add("range_index:price:float");
//        Filter filter = new Filter(
//                "index.score - seed.score >= 0.2 and index.score < 0.4 + seed.score and index.price - seed.price >= 0.2 and index.price < 0.4  + seed.price");
//        Map<String, BaseIndex> indexMap = new IndexFactory().getIndexesMap(indexMetas, neighborDataset);
//        AbstractResult indexResult = filter.filter(0, seedAttrs, neighborDataset, indexMap);
//        assertArrayEquals(Arrays.asList(3).toArray(), indexResult.getIndices().toArray());
//    }
}
