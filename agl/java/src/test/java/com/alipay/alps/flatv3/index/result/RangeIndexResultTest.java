package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.RangeIndex;
import com.alipay.alps.flatv3.index.BaseIndex;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class RangeIndexResultTest {
    @Test
    public void testJoin() {
        List<Integer> ids = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(i);
            weights.add(1.0F * i);
        }
        NeighborDataset neighborDataset = new NeighborDataset(ids.size());
        neighborDataset.addAttributeList("weight", weights);
        BaseIndex index = IndexFactory.createIndex("range_index:weight:float", neighborDataset);

        List<Range> ranges1 = new ArrayList<>();
        Range range1 = new Range(1, 3);
        Range range2 = new Range(2, 4);
        Range range3 = new Range(3, 5);
        ranges1.add(range1);
        ranges1.add(range2);
        ranges1.add(range3);
        RangeIndexResult result1 = new RangeIndexResult(index, ranges1);

        List<Range> ranges2 = new ArrayList<>();
        Range range4 = new Range(2, 5);
        Range range5 = new Range(4, 8);
        ranges2.add(range4);
        ranges2.add(range5);
        RangeIndexResult result2 = new RangeIndexResult(index, ranges2);

        AbstractIndexResult joinedResult = result1.join(result2);

        List<Range> expectedRanges = new ArrayList<>();
        expectedRanges.add(new Range(2, 3));
        expectedRanges.add(new Range(4, 4));

        RangeIndexResult expectedResult = new RangeIndexResult(index, expectedRanges);
        assertEquals(expectedResult.getIndex().getIndexColumn(), joinedResult.getIndex().getIndexColumn());
        assertEquals(expectedResult.getIndices(), joinedResult.getIndices());
        List<Range> expectedRangeList = expectedResult.getRangeList();
        List<Range> resultRangeList = ((RangeIndexResult)joinedResult).getRangeList();
        for (int i = 0; i < expectedRangeList.size(); i++) {
            assertEquals(expectedRangeList.get(i).getLow(), resultRangeList.get(i).getLow());
            assertEquals(expectedRangeList.get(i).getHigh(), resultRangeList.get(i).getHigh());
        }
    }
}
