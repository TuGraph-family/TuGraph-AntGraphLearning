package com.alipay.alps.flatv3.lib.max_compute;

import com.alipay.alps.flatv3.lib.SubGraphElement;
import com.alipay.alps.flatv3.lib.SubGraphElementIterator;

import java.util.List;

public class MaxComputeIterator implements SubGraphElementIterator {
    private int currentPosition = 0;
    private List<Object[]> values = null;
    private ArraySubGraphElement currentVal = null;

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public SubGraphElement getNext() {
        currentVal.setCurrentInput(values.get(currentPosition++));
        return currentVal;
    }

    @Override
    public void reset() {

    }
}
