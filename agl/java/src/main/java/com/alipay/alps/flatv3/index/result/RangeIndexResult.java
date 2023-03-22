package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.ArrayList;
import java.util.Map;

public class RangeIndexResult extends IndexResult {
    public List<Range> sortedIntervals = null;

    public RangeIndexResult(BaseIndex index, List<Range> sortedIntervals) {
        super(index);
        this.sortedIntervals = sortedIntervals;
    }
    public RangeIndexResult(Map<String, BaseIndex> indexes, List<Range> sortedIntervals) {
        super(indexes);
        this.sortedIntervals = sortedIntervals;
    }

    private static List<Range> joinRanges(List<Range> range1, List<Range> range2) {
        List<Range> mergedRange = new ArrayList<>();
        int i = 0, j = 0;
        while (i < range1.size() && j < range2.size()) {
            Range pair1 = range1.get(i);
            Range pair2 = range2.get(j);
            if (pair1.getHigh() < pair2.getLow()) {
                i++;
            } else if (pair2.getHigh() < pair1.getLow()) {
                j++;
            } else {
                mergedRange.add(new Range(Math.max(pair1.getLow(), pair2.getLow()), Math.min(pair1.getHigh(), pair2.getHigh())));
                i++;
                j++;
            }
        }
        return mergedRange;
    }

    private static List<Range> unionRanges(List<Range> range1, List<Range> range2) {
        List<Range> mergedRange = new ArrayList<>();
        int i = 0, j = 0;
        while (i < range1.size() && j < range2.size()) {
            Range pair1 = new Range(range1.get(i));
            Range pair2 = new Range(range2.get(j));
            if (pair1.getHigh() < pair2.getLow()) {
                mergedRange.add(pair1);
                i++;
            } else if (pair2.getHigh() < pair1.getLow()) {
                mergedRange.add(pair2);
                j++;
            } else {
                int start = Math.min(pair1.getLow(), pair2.getLow());
                if (pair1.getHigh() < pair2.getHigh()) {
                    i++;
                    pair2.setLow(start);
                } else {
                    j++;
                    pair1.setLow(start);
                }
            }
        }
        while (i < range1.size()) {
            mergedRange.add(range1.get(i));
            i++;
        }
        while (j < range2.size()) {
            mergedRange.add(range2.get(j));
            j++;
        }
        return mergedRange;
    }

    @Override
    public IndexResult intersection(IndexResult right) {
        if (right instanceof RangeIndexResult && right.indexes.keySet().containsAll(indexes.keySet())) {
            List<Range> joinedIntervals = joinRanges(sortedIntervals, ((RangeIndexResult) right).sortedIntervals);
            return new RangeIndexResult(indexes, joinedIntervals);
        } else {
            List<Integer> joinedList = CommonIndexResult.joinList(getIndices(), right.getIndices());
            HashMap<String, BaseIndex> newIndexes = new HashMap<>();
            newIndexes.putAll(this.indexes);
            newIndexes.putAll(right.indexes);
            return new CommonIndexResult(newIndexes, joinedList);
        }
    }

    @Override
    public IndexResult union(IndexResult right) {
        if (right instanceof RangeIndexResult && right.indexes.keySet().containsAll(indexes.keySet())) {
            List<Range> unionedIntervals = unionRanges(sortedIntervals, ((RangeIndexResult) right).sortedIntervals);
            return new RangeIndexResult(indexes, unionedIntervals);
        } else {
            List<Integer> unionedList = CommonIndexResult.unionList(getIndices(), right.getIndices());
            HashMap<String, BaseIndex> newIndexes = new HashMap<>();
            newIndexes.putAll(this.indexes);
            newIndexes.putAll(right.indexes);
            return new CommonIndexResult(newIndexes, unionedList);
        }
    }


    public List<Integer> getIndices() {
        Integer[] originIndex = getOriginIndex();
        List<Integer> ans = new ArrayList<>();
        for (Range p : sortedIntervals) {
            for (int i = p.getLow(); i <= p.getHigh(); i++) {
                ans.add(originIndex[i]);
            }
        }
        return ans;
    }

    @Override
    public int getSize() {
        int size = 0;
        for (int i = 0; i < sortedIntervals.size(); i++) {
            size += sortedIntervals.get(i).getSize();
        }
        return size;
    }

    @Override
    public String toString() {
        return "RangeIndexResult{" +
                "sortedIntervals=" + sortedIntervals +
                '}';
    }

    public static void main(String[] args) throws Exception {
        List<Range> range1 = new ArrayList<>();
        range1.add(new Range(1, 5));
        range1.add(new Range(10, 15));
        range1.add(new Range(30, 35));

        List<Range> range2 = new ArrayList<>();
        range2.add(new Range(4, 11));
        range2.add(new Range(20, 25));
        range2.add(new Range(31, 55));
        RangeIndexResult leftRange = new RangeIndexResult((BaseIndex) null, range1);
        RangeIndexResult rightRange = new RangeIndexResult((BaseIndex) null, range2);
        System.out.println("----unionRange:" + Arrays.toString(((RangeIndexResult) leftRange.union(rightRange)).sortedIntervals.toArray()));
        System.out.println("----joinRange:" + Arrays.toString(((RangeIndexResult) leftRange.intersection(rightRange)).sortedIntervals.toArray()));
    }
}
