package com.alipay.alps.flatv3.index.result;

import com.alipay.alps.flatv3.index.BaseIndex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class IndexResult {
    private Map<String, BaseIndex> indexes = null;
    public IndexResult(BaseIndex index) {
        if (this.indexes == null) {
            this.indexes = new HashMap<>();
        }
        this.indexes.put(index.getIndexColumn(), index);
    }
    public IndexResult(Map<String, BaseIndex> indexes) {
        if (this.indexes == null) {
            this.indexes = new HashMap<>();
        }
        this.indexes.putAll(indexes);
    }

    public Map<String, BaseIndex> getIndexes() {
        return indexes;
    }

    public abstract IndexResult join(IndexResult right);
    public abstract IndexResult union(IndexResult right);
    public abstract int getSize();
    public abstract List<Integer> getIndices();

    public boolean hasFilterCondition() {
        if (indexes == null) {
            return false;
        }
        for (String indexName : indexes.keySet()) {
            if (indexes.get(indexName).getIndexColumn() != null) {
                return true;
            }
        }
        return false;
    }

    public void copyNumberAttributes(String column, List<Float> destination) {
        for (String indexName : indexes.keySet()) {
            List<Float> floatValues = indexes.get(indexName).getNeighborDataset().getFloatAttributes(column);
            if (floatValues != null) {
                destination.addAll(floatValues);
                break;
            }
            List<Long> longValues = indexes.get(indexName).getNeighborDataset().getLongAttributes(column);
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
