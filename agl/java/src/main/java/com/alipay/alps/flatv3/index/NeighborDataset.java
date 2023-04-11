package com.alipay.alps.flatv3.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class NeighborDataset implements Serializable {
    private int neighborCount;
    private HashMap<String, List<String>> stringAttributes = null;
    private HashMap<String, List<Float>> floatAttributes = null;
    private HashMap<String, List<Long>> longAttributes = null;

    public NeighborDataset(int neigiborCount) {
        this.neighborCount = neigiborCount;
    }

    public int getNeighborCount() {
        return this.neighborCount;
    }

    public <T extends Comparable<T>> List<T> deepCopyAndReIndex(int[] originIndex, String key) {
        int neighborCount = getNeighborCount();
        int shuffleIndex[] = new int[neighborCount];
        for (int i = 0; i < neighborCount; i++) {
            shuffleIndex[originIndex[i]] = i;
        }
        List<T> ret = new ArrayList<>(getAttributeList(key));
        for (int i = 0; i < neighborCount; i++) {
            while (shuffleIndex[i] != i) {
                int newIndex = shuffleIndex[i];
                Collections.swap(ret, i, newIndex);
                int tmp = shuffleIndex[i];
                shuffleIndex[i] = shuffleIndex[newIndex];
                shuffleIndex[newIndex] = tmp;
            }
        }
        return ret;
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

    public List<Float> getFloatAttributeList(String attrName) {
        return floatAttributes != null ? floatAttributes.get(attrName) : null;
    }

    public List<Long> getLongAttributeList(String attrName) {
        return longAttributes != null ? longAttributes.get(attrName) : null;
    }

    public List<String> getStringAttributeList(String attrName) {
        return stringAttributes != null ? stringAttributes.get(attrName) : null;
    }

    public <T extends Comparable<T>> List<T> deepCopyAttributeList(String key) {
        if (floatAttributes != null && floatAttributes.containsKey(key)) {
            return (List<T>) new ArrayList<>(floatAttributes.get(key));
        } else if (longAttributes != null && longAttributes.containsKey(key)) {
            return (List<T>) new ArrayList<>(longAttributes.get(key));
        } else if (stringAttributes != null && stringAttributes.containsKey(key)) {
            return (List<T>) new ArrayList<>(stringAttributes.get(key));
        }
        return null;
    }

    public <T extends Comparable<T>> List<T> getAttributeList(String key) {
        if (floatAttributes != null && floatAttributes.containsKey(key)) {
            return (List<T>) floatAttributes.get(key);
        } else if (longAttributes != null && longAttributes.containsKey(key)) {
            return (List<T>) longAttributes.get(key);
        } else if (stringAttributes != null && stringAttributes.containsKey(key)) {
            return (List<T>) stringAttributes.get(key);
        }
        return null;
    }

    public List<Float> getNumberAttributeList(String column) {
        String dtype = getDtype(column);
        if (dtype.compareToIgnoreCase("float") == 0) {
            return getFloatAttributeList(column);
        } else if (dtype.compareToIgnoreCase("long") == 0) {
            List<Long> longWeights = getLongAttributeList(column);
            return longWeights.stream().map(Long::floatValue).collect(Collectors.toList());
        }
        throw new RuntimeException("column " + column + " is not a number column");
    }

    public <T> void addAttributeList(String key, List<T> values) {
        if (values.get(0) instanceof Float) {
            addFloatAttributeList(key, (List<Float>) values);
        } else if (values.get(0) instanceof Long) {
            addLongAttributeList(key, (List<Long>) values);
        } else if (values.get(0) instanceof String) {
            addStringAttributeList(key, (List<String>) values);
        }
    }

    private void addFloatAttributeList(String key, List<Float> values) {
        if (floatAttributes == null) {
            floatAttributes = new HashMap<>();
        }
        floatAttributes.put(key, values);
    }

    private void addLongAttributeList(String key, List<Long> values) {
        if (longAttributes == null) {
            longAttributes = new HashMap<>();
        }
        longAttributes.put(key, values);
    }

    private void addStringAttributeList(String key, List<String> values) {
        if (stringAttributes == null) {
            stringAttributes = new HashMap<>();
        }
        stringAttributes.put(key, values);
    }
}
