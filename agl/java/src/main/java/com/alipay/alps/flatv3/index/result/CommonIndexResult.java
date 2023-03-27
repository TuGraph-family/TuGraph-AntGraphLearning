package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Set<Integer> joinedList = new HashSet<>(sortedList1);
        joinedList.retainAll(new HashSet<>(sortedList2));
        return new ArrayList<>(joinedList);
    }

    public static List<Integer> unionList(List<Integer> sortedList1, List<Integer> sortedList2) {
        Set<Integer> unionList = new HashSet<>(sortedList1);
        unionList.addAll(sortedList2);
        return new ArrayList<>(unionList);
    }

    @Override
    public IndexResult join(IndexResult right) {
        List<Integer> joinedList = joinList(sortedIndices, right.getIndices());
        return new CommonIndexResult(getIndexes(), joinedList);
    }

    @Override
    public IndexResult union(IndexResult right) {
        List<Integer> unionedList = unionList(sortedIndices, right.getIndices());
        return new CommonIndexResult(getIndexes(), unionedList);
    }

    @Override
    public List<Integer> getIndices() {
        return sortedIndices;
    }

    @Override
    public int getSize() {
        return sortedIndices.size();
    }
}
