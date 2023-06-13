package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter.parser.AbstractCmpWrapper;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.RangeUnit;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseIndex implements Serializable {
//    protected HeteroDataset neighborDataset = null;
    protected int[] originIndices = null;

    /*
     * indexType: can be any of [hash_index, range_index]
     * indexDtype: can be any of [long, float, string]
     * indexColumn: the column name of the index
     */
    private String indexType;
    private String indexDtype;
    private String indexColumn;

    public BaseIndex() {
    }

    public BaseIndex(String indexType, String indexColumn, String indexDtype) {
        this.indexType = indexType;
        this.indexColumn = indexColumn;
        this.indexDtype = indexDtype;
//        this.neighborDataset = neighborDataset;
//        originIndices = buildIndex();
    }

    public int[] getOriginIndices() {
        return originIndices;
    }
    public void setOriginIndices(int[] originIndices) {
        this.originIndices = originIndices;
    }

    public String getIndexType() {
        return indexType;
    }
    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getIndexColumn() {
        return indexColumn;
    }
    public void setIndexColumn(String indexColumn) {
        this.indexColumn = indexColumn;
    }

    public String getIndexDtype() {
        return indexDtype;
    }
    public void setIndexDtype(String indexDtype) {
        this.indexDtype = indexDtype;
    }

//    public HeteroDataset getNeighborDataset() {
//        return neighborDataset;
//    }
//    public void setNeighborDataset(HeteroDataset neighborDataset) {
//        this.neighborDataset = neighborDataset;
//    }
//
//    public String getDType(String column) {
//        return neighborDataset.getDtype(column);
//    }

    public int[] buildIndex(HeteroDataset neighborDataset) {
        originIndices = new int[neighborDataset.getArraySize()];
        for (int i = 0; i < neighborDataset.getArraySize(); i++) {
            originIndices[i] = i;
        }
        return originIndices;
    }

    public AbstractResult search(AbstractCmpWrapper cmpExpWrapper, Map<VariableSource, Map<String, Element.Number>> inputVariables, HeteroDataset neighborDataset) throws Exception {
        List<RangeUnit> ranges = new ArrayList<>();
        ranges.add(new RangeUnit(0, originIndices.length - 1));
        RangeResult rangeIndexResult = new RangeResult(this, ranges);
        return rangeIndexResult;
    }
}
