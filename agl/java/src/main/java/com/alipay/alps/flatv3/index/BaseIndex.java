package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.CmpExpWrapper;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseIndex<ID> implements Serializable {
    public String indexAndSamplingMeta;
    public static class Meta {
        public String type;
        public String column;
        public String dtype;
        public Meta(String t[]) {
            this.type = t[0];
            this.column = t[1];
            this.dtype = t[2];
        }
    }
    public Meta indexMeta;
    public Meta samplingMeta;

    public Integer[] originIndex = null;
    public List<ID> node2IDs = null;
    public List<ID> edgeIDs = null;

    public HashMap<String, java.util.List<String>> stringAttributes = null;
    public HashMap<String, java.util.List<Float>> floatAttributes = null;
    public HashMap<String, java.util.List<Long>> longAttributes = null;

    BaseIndex(String indexAndSamplingMeta) {
        this.indexAndSamplingMeta = indexAndSamplingMeta.replaceAll("\\s", "");
        String arrs[] = this.indexAndSamplingMeta.split(",");
        for (int i = 0; i < arrs.length; i++) {
            String t[] = arrs[i].split(":");
            if (t[0].compareToIgnoreCase("hash_index") == 0 || t[0].compareToIgnoreCase("range_index") == 0 || t[0].compareToIgnoreCase("hash_range_index") == 0) {
                indexMeta = new Meta(t);
            } else if (t[0].compareToIgnoreCase("random") == 0 || t[0].compareToIgnoreCase("weighted_sample") == 0 || t[0].compareToIgnoreCase("topK") == 0) {
                samplingMeta = new Meta(t);
            }
        }
    }

    public String getIndexType() {
        return indexMeta == null ? null : indexMeta.type;
    }
    public String getIndexColumn() {
        return indexMeta == null ? null : indexMeta.column;
    }
    public String getIndexDtype() {
        return indexMeta == null ? null : indexMeta.dtype;
    }

    public String getSamplingMethod() {
        return samplingMeta == null ? null : samplingMeta.type;
    }
    public String getSamplingColumn() {
        return samplingMeta == null ? null : samplingMeta.column;
    }
    public String getSamplingDtype() {
        return samplingMeta == null ? null : samplingMeta.dtype;
    }

    public List<Float> getFloatAttributes(String attrName) {
        return floatAttributes.get(attrName);
    }
    public List<Long> getLongAttributes(String attrName) {
        return longAttributes.get(attrName);
    }
    public List<String> getStringAttributes(String attrName) {
        return stringAttributes.get(attrName);
    }
    public <T> List<T> getAttributeList(String key) {
        if (floatAttributes.containsKey(key)) {
            return (List<T>) floatAttributes.get(key);
        } else if (longAttributes.containsKey(key)) {
            return (List<T>) longAttributes.get(key);
        } else if (stringAttributes.containsKey(key)) {
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

    public String getDtype(String column) {
        if (column.compareTo(indexMeta.column) == 0) {
            return indexMeta.dtype;
        } else if (column.compareTo(samplingMeta.column) == 0) {
            return samplingMeta.dtype;
        }
        return null;
    }

    public void setNode2IDs(List<ID> node2IDs) {
        this.node2IDs = node2IDs;
    }

    public void setEdgeIDs(List<ID> edgeIDs) {
        this.edgeIDs = edgeIDs;
    }

    public ID getNode2ID(int neigborIdx) {
        return node2IDs.get(originIndex[neigborIdx]);
    }

    public ID getEdgeID(int neigborIdx) {
        return edgeIDs.get(originIndex[neigborIdx]);
    }

    public abstract void buildIndex();

    public abstract IndexResult search(CmpExpWrapper cmpExpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception;

}
