package com.alipay.alps.flatv3.filter.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.List;

public abstract class AbstractResult {
    private final BaseIndex index;

    public AbstractResult(BaseIndex index) {
        this.index = index;
    }

    public BaseIndex getIndex() {
        return index;
    }

    public abstract AbstractResult join(AbstractResult right);

    public abstract AbstractResult union(AbstractResult right);

    public abstract int getSize();

    public abstract List<Integer> getIndices();

    public abstract int getOriginIndex(int i);

    protected BaseIndex updateIndex(AbstractResult right) {
        BaseIndex finalIndex;
        BaseIndex leftIndex = getIndex();
        if (leftIndex != null && leftIndex.getIndexColumn() != null) {
            finalIndex = leftIndex;
        } else {
            finalIndex = right.getIndex();
        }
        return finalIndex;
    }

    public boolean hasFilterCondition() {
        return index != null && index.getIndexColumn() != null;
    }

}
