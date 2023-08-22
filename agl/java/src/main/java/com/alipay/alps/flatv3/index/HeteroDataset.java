package com.alipay.alps.flatv3.index;

import com.antfin.agl.proto.sampler.Element;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HeteroDataset implements Serializable {
    private int arraySize;
    private HashMap<String, List<String>> stringAttributes = null;
    private HashMap<String, List<Float>> floatAttributes = null;
    private HashMap<String, List<Long>> longAttributes = null;

    public HeteroDataset(int len) {
        this.arraySize = len;
    }

    public Map<String, Element.Number> fillVariables(int index) {
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        Element.Number.Builder numberBuilder = Element.Number.newBuilder();
        if (longAttributes != null) {
            for (Map.Entry<String, List<Long>> entry : longAttributes.entrySet()) {
                numberBuilder.setI(entry.getValue().get(index));
                seedVariableMap.put(entry.getKey(), numberBuilder.build());
            }
        }
        if (floatAttributes != null) {
            for (Map.Entry<String, List<Float>> entry : floatAttributes.entrySet()) {
                numberBuilder.setF(entry.getValue().get(index));
                seedVariableMap.put(entry.getKey(), numberBuilder.build());
            }
        }
        if (stringAttributes != null) {
            for (Map.Entry<String, List<String>> entry : stringAttributes.entrySet()) {
                numberBuilder.setS(entry.getValue().get(index));
                seedVariableMap.put(entry.getKey(), numberBuilder.build());
            }
        }
        return seedVariableMap;
    }

    public int getArraySize() {
        return this.arraySize;
    }

    public <T extends Comparable<T>> List<T> deepCopyAndReIndex(int[] originIndex, String key) {
        int[] shuffleIndex = new int[arraySize];
        for (int i = 0; i < arraySize; i++) {
            shuffleIndex[originIndex[i]] = i;
        }
        List<T> ret = new ArrayList<>(getAttributeList(key));
        for (int i = 0; i < arraySize; i++) {
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
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Values list cannot be empty");
        }

        Object firstValue = values.get(0);
        if (firstValue instanceof Float) {
            addFloatAttributeList(key, (List<Float>) values);
        } else if (firstValue instanceof Long) {
            addLongAttributeList(key, (List<Long>) values);
        } else if (firstValue instanceof String) {
            addStringAttributeList(key, (List<String>) values);
        } else {
            throw new IllegalArgumentException("Value type can only be Float/Long/String, but got " + firstValue);
        }
        // arraySize = values.size();
        assert arraySize == values.size();
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HeteroDataset{");
        sb.append("arraySize=").append(arraySize);
        sb.append("\n, floatAttributes=").append(floatAttributes);
        sb.append("\n, longAttributes=").append(longAttributes);
        sb.append("\n, stringAttributes=").append(stringAttributes);
        sb.append('}');
        return sb.toString();
    }


    public static void main(String[] args) throws InvalidProtocolBufferException {
        List<Double> doubles = Arrays.asList(1.2, 2.4, 3.4);
        HeteroDataset heteroDataset = new HeteroDataset(3);
        heteroDataset.addAttributeList("double", doubles);
    }
}
