package com.alipay.alps.flatv3.filter.result;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.IndexFactory;
import com.alipay.alps.flatv3.index.NeighborDataset;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RangeUnitIndexResultTest {
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

        List<RangeUnit> ranges1 = new ArrayList<>();
        RangeUnit range1 = new RangeUnit(1, 3);
        RangeUnit range2 = new RangeUnit(2, 4);
        RangeUnit range3 = new RangeUnit(3, 5);
        ranges1.add(range1);
        ranges1.add(range2);
        ranges1.add(range3);
        RangeResult result1 = new RangeResult(index, ranges1);

        List<RangeUnit> ranges2 = new ArrayList<>();
        RangeUnit range4 = new RangeUnit(2, 5);
        RangeUnit range5 = new RangeUnit(4, 8);
        ranges2.add(range4);
        ranges2.add(range5);
        RangeResult result2 = new RangeResult(index, ranges2);

        AbstractResult joinedResult = result1.join(result2);

        List<RangeUnit> expectedRanges = new ArrayList<>();
        expectedRanges.add(new RangeUnit(2, 3));
        expectedRanges.add(new RangeUnit(4, 4));

        RangeResult expectedResult = new RangeResult(index, expectedRanges);
        assertEquals(expectedResult.getIndex().getIndexColumn(), joinedResult.getIndex().getIndexColumn());
        assertEquals(expectedResult.getIndices(), joinedResult.getIndices());
        List<RangeUnit> expectedRangeList = expectedResult.getRangeList();
        List<RangeUnit> resultRangeList = ((RangeResult) joinedResult).getRangeList();
        for (int i = 0; i < expectedRangeList.size(); i++) {
            assertEquals(expectedRangeList.get(i).getLow(), resultRangeList.get(i).getLow());
            assertEquals(expectedRangeList.get(i).getHigh(), resultRangeList.get(i).getHigh());
        }
    }
}
