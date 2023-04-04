package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommonIndexResult extends AbstractIndexResult {
    public List<Integer> indices = null;
    public CommonIndexResult(BaseIndex index, List<Integer> sortedIndices) {
        super(index);
        this.indices = sortedIndices;
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
    public AbstractIndexResult join(AbstractIndexResult right) {
        List<Integer> joinedList = joinList(indices, right.getIndices());
        return new CommonIndexResult(updateIndex(right), joinedList);
    }

    @Override
    public AbstractIndexResult union(AbstractIndexResult right) {
        List<Integer> unionedList = unionList(indices, right.getIndices());
        return new CommonIndexResult(updateIndex(right), unionedList);
    }

    @Override
    public List<Integer> getIndices() {
        return indices;
    }

    @Override
    public int getSize() {
        return indices.size();
    }
}
