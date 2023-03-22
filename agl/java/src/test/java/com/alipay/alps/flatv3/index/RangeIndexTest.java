package com.alipay.alps.flatv3.index;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import static org.junit.Assert.*;

public class RangeIndexTest {

    @Test
    public void testBuildIndex() {
        // create a RangeIndex object with some test data
        RangeIndex rangeIndex = new RangeIndex("range_index:col1:double");
        rangeIndex.setNode2IDs(Arrays.asList(new String[]{"A", "B", "C"}));
        rangeIndex.setEdgeIDs(Arrays.asList(new String[]{"e1", "e2", "e3"}));
        rangeIndex.addAttributes("col1", Arrays.asList(new Float[]{3.0F, 2.0F, 1.0F}));

        // build the index
        rangeIndex.buildIndex();

        // check if the index is built correctly
        Integer[] expectedOriginIndex = new Integer[]{2, 1, 0};
        Integer[] actualOriginIndex = rangeIndex.getOriginIndex();
        assertArrayEquals(expectedOriginIndex, actualOriginIndex);

        HashMap<String, List<Double>> expectedIndexedData = new HashMap<>();
        expectedIndexedData.put("col1", Arrays.asList(new Double[]{1.0, 2.0, 3.0}));
//        HashMap<String, List<Double>> actualIndexedData = rangeIndex.getFloatAttribute();
//        assertEquals(expectedIndexedData, actualIndexedData);
    }

    @Test
    public void testBinarySearchInequation() {
        // create a RangeIndex object with some test data
        RangeIndex rangeIndex = new RangeIndex("range_index:col1:double");
        rangeIndex.setNode2IDs(Arrays.asList(new String[]{"A", "B", "C"}));
        rangeIndex.setEdgeIDs(Arrays.asList(new String[]{"e1", "e2", "e3"}));
        rangeIndex.addAttributes("col1", Arrays.asList(new Float[]{1.0F, 2.0F, 3.0F}));

        // test binarySearchInequation method
//        int expectedIndex = 1;
//        int actualIndex = rangeIndex.binarySearchInequation(Arrays.asList(new Double[]{1.0, 2.0, 3.0}), ">=", 2.0);
//        assertEquals(expectedIndex, actualIndex);
    }

    @Test
    public void testSearch() throws Exception {
        // create a RangeIndex object with some test data
        RangeIndex rangeIndex = new RangeIndex("range_index:col1:double");
        rangeIndex.setNode2IDs(Arrays.asList(new String[]{"A", "B", "C"}));
        rangeIndex.setEdgeIDs(Arrays.asList(new String[]{"e1", "e2", "e3"}));
        rangeIndex.addAttributes("col1", Arrays.asList(new Float[]{1.0F, 2.0F, 3.0F}));

        // build the index
        rangeIndex.buildIndex();

        // test search method
//        FilterConditionSingleton filterConditionSingleton = new FilterConditionSingleton();
//        filterConditionSingleton.addFilterCondition("col1", ">=", 2.0);
//        IndexResult expectedIndexResult = new IndexResult();
//        expectedIndexResult.addEdge("e2", "B");
//        expectedIndexResult.addEdge("e3", "C");
//        IndexResult actualIndexResult = rangeIndex.search(filterConditionSingleton, 0);
//        assertEquals(expectedIndexResult, actualIndexResult);
    }
}
