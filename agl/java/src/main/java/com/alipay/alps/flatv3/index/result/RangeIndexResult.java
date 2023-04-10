package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.List;
import java.util.ArrayList;

public class RangeIndexResult extends AbstractIndexResult {
    private List<Range> sortedIntervals = null;
    private List<Integer> prefixCounts = null;

    public RangeIndexResult(BaseIndex index, List<Range> sortedIntervals) {
        super(index);
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
    public AbstractIndexResult join(AbstractIndexResult right) {
        if (getIndex() == right.getIndex() && right instanceof RangeIndexResult) {
            List<Range> joinedIntervals = joinRanges(sortedIntervals, ((RangeIndexResult) right).sortedIntervals);
            return new RangeIndexResult(getIndex(), joinedIntervals);
        } else {
            List<Integer> joinedList = CommonIndexResult.joinList(getIndices(), right.getIndices());
            return new CommonIndexResult(updateIndex(right), joinedList);
        }
    }

    @Override
    public AbstractIndexResult union(AbstractIndexResult right) {
        if (getIndex() == right.getIndex() && right instanceof RangeIndexResult) {
            List<Range> unionedIntervals = unionRanges(sortedIntervals, ((RangeIndexResult) right).sortedIntervals);
            return new RangeIndexResult(getIndex(), unionedIntervals);
        } else {
            List<Integer> unionedList = CommonIndexResult.unionList(getIndices(), right.getIndices());
            return new CommonIndexResult(updateIndex(right), unionedList);
        }
    }

    @Override
    public List<Integer> getIndices() {
        List<Integer> ans = new ArrayList<>();
        Integer[] originIndex = getIndex().getOriginIndices();
        for (Range p : sortedIntervals) {
            for (int i = p.getLow(); i <= p.getHigh(); i++) {
                ans.add(originIndex[i]);
            }
        }
        return ans;
    }

    @Override
    public int getOriginIndex(int i) {
        Integer[] originIndex = getIndex().getOriginIndices();
        return originIndex[i];
    }

    public int getRangeIndex(int rank) {
        if (prefixCounts == null) {
            prefixCounts = new ArrayList<>(sortedIntervals.size());
            int currentCount = 0;
            for (Range range : sortedIntervals) {
                prefixCounts.add(range.getSize() + currentCount);
                currentCount += range.getSize();
            }
        }
        // binary search sortedIntervals, find the range that contains rank
        int low = 0, high = prefixCounts.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (rank < prefixCounts.get(mid)) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        int preCount = low > 0 ? prefixCounts.get(low - 1) : 0;
        return rank - preCount + sortedIntervals.get(low).getLow();
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
