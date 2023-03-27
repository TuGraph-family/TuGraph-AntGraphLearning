package com.alipay.alps.flatv3.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class NeighborDataset<ID> implements Serializable {
    public List<ID> node2IDs = null;
    public List<ID> edgeIDs = null;
    public HashMap<String, List<String>> stringAttributes = null;
    public HashMap<String, List<Float>> floatAttributes = null;
    public HashMap<String, List<Long>> longAttributes = null;

    public NeighborDataset(List<ID> node2IDs, List<ID> edgeIDs) {
        this.node2IDs = node2IDs;
        this.edgeIDs = edgeIDs;
    }

    public int getNeighborCount() {
        return this.node2IDs.size();
    }
    public void shuffle(Integer[] originIndex) {
        int neighborCount = getNeighborCount();
        int shuffleIndex[] = new int[neighborCount];
        for (int i = 0; i < neighborCount; i++) {
            shuffleIndex[originIndex[i]] = i;
        }
        for (int i = 0; i < neighborCount; i++) {
            while (shuffleIndex[i] != i) {
                int newIndex = shuffleIndex[i];
                if (floatAttributes != null) {
                    for (String key : this.floatAttributes.keySet()) {
                        Collections.swap(this.floatAttributes.get(key), i, newIndex);
                    }
                }
                if (longAttributes != null) {
                    for (String key : this.longAttributes.keySet()) {
                        Collections.swap(this.longAttributes.get(key), i, newIndex);
                    }
                }
                int tmp = shuffleIndex[i];
                shuffleIndex[i] = shuffleIndex[newIndex];
                shuffleIndex[newIndex] = tmp;
            }
        }
    }

    public String getDtype(String key) {
        if (floatAttributes != null && floatAttributes.containsKey(key)) {
            return "float";
        } else if (longAttributes != null && longAttributes.containsKey(key)) {
            return "long";
        } else if (stringAttributes != null && stringAttributes.containsKey(key)) {
            return "string";
        }
        return null;
    }
    public List<Float> getFloatAttributes(String attrName) {
        return floatAttributes != null ? floatAttributes.get(attrName) : null;
    }
    public List<Long> getLongAttributes(String attrName) {
        return longAttributes != null ? longAttributes.get(attrName) : null;
    }
    public List<String> getStringAttributes(String attrName) {
        return stringAttributes != null ? stringAttributes.get(attrName) : null;
    }
    public <T> List<T> getAttributeList(String key) {
        if (floatAttributes != null && floatAttributes.containsKey(key)) {
            return (List<T>) floatAttributes.get(key);
        } else if (longAttributes != null && longAttributes.containsKey(key)) {
            return (List<T>) longAttributes.get(key);
        } else if (stringAttributes != null && stringAttributes.containsKey(key)) {
            return (List<T>) stringAttributes.get(key);
        }
        return null;
    }

    public <T> void addAttributes(String key, List<T> values) {
        if (values.get(0) instanceof Float) {
            addFloatAttributes(key, (List<Float>) values);
        } else if (values.get(0) instanceof Long) {
            addLongAttributes(key, (List<Long>) values);
        } else if (values.get(0) instanceof String) {
            addStringAttributes(key, (List<String>) values);
        }
    }
    private void addFloatAttributes(String key, List<Float> values) {
        if (floatAttributes == null) {
            floatAttributes = new HashMap<>();
        }
        floatAttributes.put(key, values);
    }
    private void addLongAttributes(String key, List<Long> values) {
        if (longAttributes == null) {
            longAttributes = new HashMap<>();
        }
        longAttributes.put(key, values);
    }
    private void addStringAttributes(String key, List<String> values) {
        if (stringAttributes == null) {
            stringAttributes = new HashMap<>();
        }
        stringAttributes.put(key, values);
    }

    public ID getNode2ID(int neigborIdx) {
        return node2IDs.get(neigborIdx);
    }

    public ID getEdgeID(int neigborIdx) {
        return edgeIDs.get(neigborIdx);
    }
}
