package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.NeighborDataset;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;

public class CommonIndexResultTest {
    private BaseIndex index;
    private CommonIndexResult result;

    @Before
    public void setup() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
        }
        NeighborDataset<String> neighborDataset = new NeighborDataset<>(ids, null);
        index = new BaseIndex("", neighborDataset);
        result = new CommonIndexResult(index, Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void testJoin() {
        CommonIndexResult otherResult = new CommonIndexResult(index, Arrays.asList(3, 4, 5, 6, 7));
        IndexResult joinedResult = result.join(otherResult);
        assertArrayEquals(Arrays.asList(3, 4, 5).toArray(), joinedResult.getIndices().toArray());
    }

    @Test
    public void testUnion() {
        CommonIndexResult otherResult = new CommonIndexResult(index, Arrays.asList(3, 4, 5, 6, 7));
        IndexResult unionResult = result.union(otherResult);

        assertArrayEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7).toArray(), unionResult.getIndices().toArray());
    }

    @Test
    public void testGetSize() {
        assertEquals(5, result.getSize());
    }

    @Test
    public void testGetIndices() {
        assertArrayEquals(Arrays.asList(1, 2, 3, 4, 5).toArray(), result.getIndices().toArray());
    }
}
