package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.AbstractCmpWrapper;
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
    protected NeighborDataset neighborDataset = null;
    protected int[] originIndices = null;

    /*
     * indexType: can be any of [hash_index, range_index]
     * indexDtype: can be any of [long, float, string]
     * indexColumn: the column name of the index
     */
    private String indexType;
    private String indexDtype;
    private String indexColumn;
    
    public BaseIndex(String indexType, String indexColumn, String indexDtype, NeighborDataset neighborDataset) {
        this.indexType = indexType;
        this.indexColumn = indexColumn;
        this.indexDtype = indexDtype;
        this.neighborDataset = neighborDataset;
        originIndices = buildIndex();
    }

    public int[] getOriginIndices() {
        return originIndices;
    }

    public String getIndexType() {
        return indexType;
    }

    public String getIndexColumn() {
        return indexColumn;
    }

    public String getIndexDtype() {
        return indexDtype;
    }

    public NeighborDataset getNeighborDataset() {
        return neighborDataset;
    }

    public String getDtype(String column) {
        return neighborDataset.getDtype(column);
    }

    protected int[] buildIndex() {
        originIndices = new int[neighborDataset.getNeighborCount()];
        for (int i = 0; i < neighborDataset.getNeighborCount(); i++) {
            originIndices[i] = i;
        }
        return originIndices;
    }

    public AbstractIndexResult search(AbstractCmpWrapper cmpExpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        List<Range> ranges = new ArrayList<>();
        ranges.add(new Range(0, neighborDataset.getNeighborCount() - 1));
        RangeIndexResult rangeIndexResult = new RangeIndexResult(this, ranges);
        return rangeIndexResult;
    }
}
