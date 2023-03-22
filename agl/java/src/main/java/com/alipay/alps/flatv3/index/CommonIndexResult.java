package com.alipay.alps.flatv3.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommonIndexResult extends IndexResult {
    public List<Integer> sortedIndices = null;
    public CommonIndexResult(BaseIndex index, List<Integer> sortedIndices) {
        super(index);
        this.sortedIndices = sortedIndices;
    }
    public CommonIndexResult(Map<String, BaseIndex> indexes, List<Integer> sortedIndices) {
        super(indexes);
        this.sortedIndices = sortedIndices;
    }
    public static List<Integer> joinList(List<Integer> sortedList1, List<Integer> sortedList2) {
        List<Integer> joinedList = new ArrayList<>();
        int i = 0, j = 0;
        while (i < sortedList1.size() && j < sortedList2.size()) {
            if (sortedList1.get(i) < sortedList2.get(i)) {
                i++;
            } else if (sortedList1.get(i) > sortedList2.get(j)) {
                j++;
            } else {
                joinedList.add(sortedList1.get(i++));
                j++;
            }
        }
        return joinedList;
    }

    public static List<Integer> unionList(List<Integer> sortedList1, List<Integer> sortedList2) {
        List<Integer> unionedList = new ArrayList<>();
        int i = 0, j = 0;
        while (i < sortedList1.size() && j < sortedList2.size()) {
            if (sortedList1.get(i) < sortedList2.get(j)) {
                unionedList.add(sortedList1.get(i++));
            } else if (sortedList1.get(i) > sortedList2.get(j)) {
                unionedList.add(sortedList2.get(j++));
            } else {
                unionedList.add(sortedList1.get(i++));
                j++;
            }
        }
        while (i < sortedList1.size()) {
            unionedList.add(sortedList1.get(i++));
        }

        while (j < sortedList2.size()) {
            unionedList.add(sortedList2.get(j++));
        }
        return unionedList;
    }

    @Override
    public IndexResult intersection(IndexResult right) {
        List<Integer> joinedList = joinList(sortedIndices, right.getIndices());
        return new CommonIndexResult(indexes, joinedList);
    }

    @Override
    public IndexResult union(IndexResult right) {
        List<Integer> unionedList = unionList(sortedIndices, right.getIndices());
        return new CommonIndexResult(indexes, unionedList);
    }

    @Override
    public List<Integer> getIndices() {
        return sortedIndices;
    }

    @Override
    public int getSize() {
        return sortedIndices.size();
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
//        RangeIndexResult leftRange = new RangeIndexResult(null, range1);
//        RangeIndexResult rightRange = new RangeIndexResult(null, range2);
//        System.out.println("----unionRange:" + Arrays.toString(((RangeIndexResult) leftRange.union(rightRange)).sortedIntervals.toArray()));
//        System.out.println("----joinRange:" + Arrays.toString(((RangeIndexResult) leftRange.intersection(rightRange)).sortedIntervals.toArray()));
    }
}
