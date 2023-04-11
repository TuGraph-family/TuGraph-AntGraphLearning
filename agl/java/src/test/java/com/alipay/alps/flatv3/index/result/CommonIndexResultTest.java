package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.index.NeighborDataset;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CommonIndexResultTest {
    private BaseIndex index;
    private CommonIndexResult result;

    @Before
    public void setup() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(i);
        }
        NeighborDataset neighborDataset = new NeighborDataset(ids.size());
        index = IndexFactory.createIndex("", neighborDataset);
        result = new CommonIndexResult(index, Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void testJoin() {
        CommonIndexResult otherResult = new CommonIndexResult(index, Arrays.asList(3, 4, 5, 6, 7));
        AbstractIndexResult joinedResult = result.join(otherResult);
        assertArrayEquals(Arrays.asList(3, 4, 5).toArray(), joinedResult.getIndices().toArray());
    }

    @Test
    public void testUnion() {
        CommonIndexResult otherResult = new CommonIndexResult(index, Arrays.asList(3, 4, 5, 6, 7));
        AbstractIndexResult unionResult = result.union(otherResult);

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
