package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.HashMap;
import java.util.List;

import java.util.ArrayList;
import java.util.Map;

public class RangeIndexResult extends IndexResult {
    private List<Range> sortedIntervals = null;

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
    public IndexResult join(IndexResult right) {
        if (right instanceof RangeIndexResult && right.getIndexes().keySet().containsAll(getIndexes().keySet())) {
            List<Range> joinedIntervals = joinRanges(sortedIntervals, ((RangeIndexResult) right).sortedIntervals);
            return new RangeIndexResult(getIndexes(), joinedIntervals);
        } else {
            List<Integer> joinedList = CommonIndexResult.joinList(getIndices(), right.getIndices());
            HashMap<String, BaseIndex> newIndexes = new HashMap<>();
            newIndexes.putAll(this.getIndexes());
            newIndexes.putAll(right.getIndexes());
            return new CommonIndexResult(newIndexes, joinedList);
        }
    }

    @Override
    public IndexResult union(IndexResult right) {
        if (right instanceof RangeIndexResult && right.getIndexes().keySet().containsAll(getIndexes().keySet())) {
            List<Range> unionedIntervals = unionRanges(sortedIntervals, ((RangeIndexResult) right).sortedIntervals);
            return new RangeIndexResult(getIndexes(), unionedIntervals);
        } else {
            List<Integer> unionedList = CommonIndexResult.unionList(getIndices(), right.getIndices());
            HashMap<String, BaseIndex> newIndexes = new HashMap<>();
            newIndexes.putAll(this.getIndexes());
            newIndexes.putAll(right.getIndexes());
            return new CommonIndexResult(newIndexes, unionedList);
        }
    }

    @Override
    public List<Integer> getIndices() {
        List<Integer> ans = new ArrayList<>();
        Integer[] originIndex = getOriginIndex();
        if (originIndex != null) {
            for (Range p : sortedIntervals) {
                for (int i = p.getLow(); i <= p.getHigh(); i++) {
                    ans.add(originIndex[i]);
                }
            }
        } else {
            for (Range p : sortedIntervals) {
                for (int i = p.getLow(); i <= p.getHigh(); i++) {
                    ans.add(i);
                }
            }
        }
        return ans;
    }

    public List<Range> getRangeList() {
        return sortedIntervals;
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
}
