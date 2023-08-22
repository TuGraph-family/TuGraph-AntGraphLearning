package com.alipay.alps.flatv3.lib.spark;

import com.alipay.alps.flatv3.lib.SubGraphElement;
import com.alipay.alps.flatv3.spark.utils.Constants;
import org.apache.spark.sql.Row;

public class RowSubGraphElement implements SubGraphElement {
    private Row currentInputRow = null;

    public RowSubGraphElement() {
    }

    public RowSubGraphElement(Row row) {
        this.currentInputRow = row;
    }

    public void setCurrentInput(Row row) {
        this.currentInputRow = row;
    }

    @Override
    public String getString(String column) {
        return currentInputRow.getString(Constants.ELEMENT_FIELD_INDEX.get(column));
    }

    @Override
    public int getInt(String column) {
        int i = Constants.ELEMENT_FIELD_INDEX.get(column);
        return currentInputRow.getInt(i);
    }

    @Override
    public float getFloat(String column) {
        return currentInputRow.getFloat(Constants.ELEMENT_FIELD_INDEX.get(column));
    }
}
