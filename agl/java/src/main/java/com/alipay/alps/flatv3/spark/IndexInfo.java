package com.alipay.alps.flatv3.spark;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.RangeIndex;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexInfo implements Serializable {
    private String node1_id;
    private List<String> node2_id;
    private List<String> edge_id;
    private List<Long> time;
    private Map<String, BaseIndex> baseIndexes;
    private Map<String, RangeIndex> rangeIndexes;
    private Map<String, HashIndex> hashIndexes;

    public IndexInfo() {
    }

    public String getNode1_id() {
        return node1_id;
    }

    public void setNode1_id(String node1_id) {
        this.node1_id = node1_id;
    }

    public List<String> getNode2_id() {
        return node2_id;
    }

    public void setNode2_id(List<String> node2_id) {
        this.node2_id = node2_id;
    }

    public List<String> getEdge_id() {
        return edge_id;
    }

    public void setEdge_id(List<String> edge_id) {
        this.edge_id = edge_id;
    }

    public List<Long> getTime() {
        return time;
    }

    public void setTime(List<Long> time) {
        this.time = time;
    }

    public Map<String, BaseIndex> getBaseIndexes() {
        return baseIndexes;
    }

    public void setBaseIndexes(Map<String, BaseIndex> indexes) {
        this.baseIndexes = indexes;
    }

    public Map<String, RangeIndex> getRangeIndexes() {
        return rangeIndexes;
    }

    public void setRangeIndexes(Map<String, RangeIndex> indexes) {
        this.rangeIndexes = indexes;
    }

    public Map<String, HashIndex> getHashIndexes() {
        return hashIndexes;
    }

    public void setHashIndexes(Map<String, HashIndex> indexes) {
        this.hashIndexes = indexes;
    }

    public static void setAllIndexes(Map<String, BaseIndex> allIndexes, IndexInfo indexInfo) {
        Map<String, BaseIndex> baseIndexes = new HashMap<String, BaseIndex>();
        Map<String, RangeIndex> rangeIndexes = new HashMap<String, RangeIndex>();
        Map<String, HashIndex> hashIndexes = new HashMap<String, HashIndex>();
        for (Map.Entry<String, BaseIndex> entry : allIndexes.entrySet()) {
            BaseIndex index = entry.getValue();
            if (index instanceof RangeIndex) {
                rangeIndexes.put(entry.getKey(), (RangeIndex) index);
            } else if (index instanceof HashIndex) {
                hashIndexes.put(entry.getKey(), (HashIndex) index);
            } else {
                baseIndexes.put(entry.getKey(), index);
            }
        }
        if (rangeIndexes.size() > 0) {
            indexInfo.setRangeIndexes(rangeIndexes);
        }
        if (hashIndexes.size() > 0) {
            indexInfo.setHashIndexes(hashIndexes);
        }
        if (baseIndexes.size() > 0) {
            indexInfo.setBaseIndexes(baseIndexes);
        }
    }

    public static Map<String, BaseIndex> getAllIndexes(IndexInfo indexInfo) {
        Map<String, BaseIndex> allIndexes = new HashMap<String, BaseIndex>();
        if (indexInfo.getBaseIndexes() != null) {
            allIndexes.putAll(indexInfo.getBaseIndexes());
        }
        if (indexInfo.getRangeIndexes() != null) {
            allIndexes.putAll(indexInfo.getRangeIndexes());
        }
        if (indexInfo.getHashIndexes() != null) {
            allIndexes.putAll(indexInfo.getHashIndexes());
        }
        return allIndexes;
    }
}