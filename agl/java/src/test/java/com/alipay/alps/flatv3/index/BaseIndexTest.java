package com.alipay.alps.flatv3.index;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

public class BaseIndexTest {

    BaseIndex baseIndex;

    @Before
    public void setUp() {
        String indexMeta = "range_index:column_name:double,random:column_name:double";
        baseIndex = new RangeIndex(indexMeta);
    }

    @Test
    public void testSetNode2IDs() {
        List<String> node2IDs = Arrays.asList("1", "2", "3");
        baseIndex.setNode2IDs(node2IDs);
        assertEquals(node2IDs, baseIndex.node2IDs);
    }

    @Test
    public void testSetEdgeIDs() {
        List<String> edgeIDs = Arrays.asList("edge1", "edge2", "edge3");
        baseIndex.setEdgeIDs(edgeIDs);
        assertEquals(edgeIDs, baseIndex.edgeIDs);
    }

    @Test
    public void testIndexMeta() {
        String expectedIndexMeta = "range_index:column_name:double,random:column_name:double";
        assertEquals(expectedIndexMeta, baseIndex.indexMeta);
    }

    @Test
    public void testIndexType() {
        String expectedIndexType = "range_index";
        assertEquals(expectedIndexType, baseIndex.getIndexType());
    }

    @Test
    public void testIndexColumn() {
        String expectedIndexColumn = "column_name";
        assertEquals(expectedIndexColumn, baseIndex.getIndexColumn());
    }

    @Test
    public void testIndexValueType() {
        String expectedIndexValueType = "double";
        assertEquals(expectedIndexValueType, baseIndex.getIndexDtype());
    }

    @Test
    public void testSampleMethod() {
        String expectedSampleMethod = "random";
        assertEquals(expectedSampleMethod, baseIndex.getSamplingMethod());
    }

    @Test
    public void testSampleColumn() {
        String expectedSampleColumn = "column_name";
        assertEquals(expectedSampleColumn, baseIndex.getSamplingColumn());
    }

    @Test
    public void testSampleValueType() {
        String expectedSampleValueType = "double";
        assertEquals(expectedSampleValueType, baseIndex.getSamplingDtype());
    }

    @Test
    public void testGetNode2ID() {
        baseIndex.node2IDs = Arrays.asList("1", "2", "3");
        baseIndex.originIndex = new Integer[]{1, 2, 0};
        assertEquals("2", baseIndex.getNode2ID(1));
    }

    @Test
    public void testGetEdgeID() {
        baseIndex.edgeIDs = Arrays.asList("edge1", "edge2", "edge3");
        baseIndex.originIndex = new Integer[]{1, 2, 0};
        assertEquals("edge2", baseIndex.getEdgeID(1));
    }
}

