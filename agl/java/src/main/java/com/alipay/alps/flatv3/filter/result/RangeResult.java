package com.alipay.alps.flatv3.filter.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.ArrayList;
import java.util.List;

public class RangeResult extends AbstractResult {
    private List<RangeUnit> sortedIntervals = null;
    private List<Integer> prefixCounts = null;

    public RangeResult(BaseIndex index, List<RangeUnit> sortedIntervals) {
        super(index);
        this.sortedIntervals = sortedIntervals;
    }

    private static List<RangeUnit> joinRanges(List<RangeUnit> range1, List<RangeUnit> range2) {
        List<RangeUnit> mergedRange = new ArrayList<>();
        int i = 0, j = 0;
        while (i < range1.size() && j < range2.size()) {
            RangeUnit pair1 = range1.get(i);
            RangeUnit pair2 = range2.get(j);
            if (pair1.getHigh() < pair2.getLow()) {
                i++;
            } else if (pair2.getHigh() < pair1.getLow()) {
                j++;
            } else {
                mergedRange.add(new RangeUnit(Math.max(pair1.getLow(), pair2.getLow()), Math.min(pair1.getHigh(), pair2.getHigh())));
                i++;
                j++;
            }
        }
        return mergedRange;
    }

    private static List<RangeUnit> unionRanges(List<RangeUnit> range1, List<RangeUnit> range2) {
        List<RangeUnit> mergedRange = new ArrayList<>();
        int i = 0, j = 0;
        while (i < range1.size() && j < range2.size()) {
            RangeUnit pair1 = new RangeUnit(range1.get(i));
            RangeUnit pair2 = new RangeUnit(range2.get(j));
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
    public AbstractResult join(AbstractResult right) {
        if (getIndex() == right.getIndex() && right instanceof RangeResult) {
            List<RangeUnit> joinedIntervals = joinRanges(sortedIntervals, ((RangeResult) right).sortedIntervals);
            return new RangeResult(getIndex(), joinedIntervals);
        } else {
            List<Integer> joinedList = CommonResult.joinList(getIndices(), right.getIndices());
            return new CommonResult(updateIndex(right), joinedList);
        }
    }

    @Override
    public AbstractResult union(AbstractResult right) {
        if (getIndex() == right.getIndex() && right instanceof RangeResult) {
            List<RangeUnit> unionedIntervals = unionRanges(sortedIntervals, ((RangeResult) right).sortedIntervals);
            return new RangeResult(getIndex(), unionedIntervals);
        } else {
            List<Integer> unionedList = CommonResult.unionList(getIndices(), right.getIndices());
            return new CommonResult(updateIndex(right), unionedList);
        }
    }

    @Override
    public List<Integer> getIndices() {
        List<Integer> ans = new ArrayList<>();
        int[] originIndex = getIndex() == null ? null : getIndex().getOriginIndices();
        for (RangeUnit p : sortedIntervals) {
            for (int i = p.getLow(); i <= p.getHigh(); i++) {
                ans.add(originIndex == null ? i : originIndex[i]);
            }
        }
        return ans;
    }

    @Override
    public int getOriginIndex(int i) {
        if (getIndex() == null) {
            return i;
        }
        int[] originIndex = getIndex().getOriginIndices();
        return originIndex[i];
    }

    public int getRangeIndex(int rank) {
        if (prefixCounts == null) {
            prefixCounts = new ArrayList<>(sortedIntervals.size());
            int currentCount = 0;
            for (RangeUnit range : sortedIntervals) {
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

    public List<RangeUnit> getRangeList() {
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
