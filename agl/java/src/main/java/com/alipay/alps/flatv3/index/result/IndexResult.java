package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class IndexResult {
    public Map<String, BaseIndex> indexes = null;
    public IndexResult(BaseIndex index) {
        if (indexes == null) {
            indexes = new HashMap<>();
        }
        this.indexes.put(index.getIndexColumn(), index);
    }
    public IndexResult(Map<String, BaseIndex> indexes) {
        if (this.indexes == null) {
            this.indexes = new HashMap<>();
        }
        this.indexes.putAll(indexes);
    }
    public abstract IndexResult intersection(IndexResult right);
    public abstract IndexResult union(IndexResult right);
    public abstract int getSize();
    public abstract List<Integer> getIndices();

    public boolean hasFilterCondition() {
        if (indexes == null) {
            return true;
        }
        for (String indexName : indexes.keySet()) {
            if (indexes.get(indexName).getIndexColumn() != null) {
                return false;
            }
        }
        return true;
    }

    public void copyNumberAttributes(String column, List<Float> destination) {
        for (String indexName : indexes.keySet()) {
            List<Float> floatValues = indexes.get(indexName).getFloatAttributes(column);
            if (floatValues != null) {
                destination.addAll(floatValues);
                break;
            }
            List<Long> longValues = indexes.get(indexName).getLongAttributes(column);
            if (longValues != null) {
                for (long l : longValues) {
                    destination.add((float) l);
                }
                break;
            }
        }
    }

    public Integer[] getOriginIndex() {
        for (String indexName : indexes.keySet()) {
            return indexes.get(indexName).originIndex;
        }
        return null;
    }
}
