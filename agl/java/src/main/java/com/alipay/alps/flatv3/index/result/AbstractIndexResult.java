package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;
import java.util.List;

public abstract class AbstractIndexResult {
    private final BaseIndex index;
    public AbstractIndexResult(BaseIndex index) {
        this.index = index;
    }

    public BaseIndex getIndex() {
        return index;
    }

    public abstract AbstractIndexResult join(AbstractIndexResult right);
    public abstract AbstractIndexResult union(AbstractIndexResult right);
    public abstract int getSize();
    public abstract List<Integer> getIndices();

    protected BaseIndex updateIndex(AbstractIndexResult right) {
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

    public Integer[] getOriginIndice() {
        return index.originIndices;
    }
}
