package com.alipay.alps.flatv3.index;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

public class BaseIndexTest {

    private NeighborDataset<String> neighborDataset;
    private Integer[] originIndex;

    @Before
    public void setUp() {
        neighborDataset = new NeighborDataset<>(Arrays.asList("a", "b", "c", "d"), Arrays.asList("ab", "ac", "cd"));
        neighborDataset.addAttributes("attr1", Arrays.asList("val1", "val2", "val3", "val4"));
        neighborDataset.addAttributes("attr2", Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f));
        neighborDataset.addAttributes("attr3", Arrays.asList(1L, 2L, 3L, 4L));
        originIndex = new Integer[]{0, 1, 2, 3};
    }

    @Test
    public void testBuildIndex() {
        HashIndex baseIndex = new HashIndex("hash_index:attr1:string", neighborDataset);
        assertEquals("hash_index", baseIndex.getIndexType());
        assertEquals("attr1", baseIndex.getIndexColumn());
        assertEquals("string", baseIndex.getIndexDtype());
    }

    @Test
    public void testGetters() {
        RangeIndex baseIndex = new RangeIndex("range_index:attr2:float", neighborDataset);
        assertEquals("range_index", baseIndex.getIndexType());
        assertEquals("attr2", baseIndex.getIndexColumn());
        assertEquals("float", baseIndex.getIndexDtype());
    }

    @Test
    public void testGetDtype() {
        BaseIndex baseIndex = new RangeIndex("range_index:attr2:float", neighborDataset);
        assertEquals("float", baseIndex.getDtype("attr2"));
        assertEquals("long", baseIndex.getDtype("attr3"));
        assertNull(baseIndex.getDtype("attr1"));
    }
}

