package com.alipay.alps.flatv3.lib.spark;

import com.alipay.alps.flatv3.lib.SubGraphElement;
import com.alipay.alps.flatv3.lib.SubGraphElementIterator;
import org.apache.spark.sql.Row;

import java.util.Iterator;

public class SparkIterator implements SubGraphElementIterator {
    private Iterator<Row> values = null;
    private RowSubGraphElement currentVal = null;

    public SparkIterator(Iterator<Row> values) {
        this.values = values;
        currentVal = new RowSubGraphElement();
    }

    private void lazyLoad() {
    }

    @Override
    public boolean hasNext() {
        return values.hasNext();
    }

    @Override
    public SubGraphElement getNext() {
        currentVal.setCurrentInput(values.next());
        return currentVal;
    }

    @Override
    public void reset() {

    }
}
