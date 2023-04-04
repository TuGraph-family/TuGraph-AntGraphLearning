package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.AbstactCmpWrapper;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseIndex implements Serializable {
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
    protected NeighborDataset neighborDataset = null;
    public Integer[] originIndices = null;

    public BaseIndex(String indexAndSamplingMeta, NeighborDataset neighborDataset) {
        this.neighborDataset = neighborDataset;
        this.indexAndSamplingMeta = indexAndSamplingMeta.replaceAll("\\s", "");
        String arrs[] = this.indexAndSamplingMeta.split(",");
        for (int i = 0; i < arrs.length; i++) {
            String t[] = arrs[i].split(":");
            if (t[0].compareToIgnoreCase("hash_index") == 0 || t[0].compareToIgnoreCase("range_index") == 0 || t[0].compareToIgnoreCase("hash_range_index") == 0) {
                indexMeta = new Meta(t);
            }
        }
        buildIndex();
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

    public NeighborDataset getNeighborDataset() {
        return neighborDataset;
    }

    public String getDtype(String column) {
        return neighborDataset.getDtype(column);
    }

    protected void buildIndex() {
    }

    public AbstractIndexResult search(AbstactCmpWrapper cmpExpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        List<Range> ranges = new ArrayList<>();
        ranges.add(new Range(0, neighborDataset.getNeighborCount() - 1));
        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, ranges);
        return rangeIndexResult;
    }
}
