package com.alipay.alps.flatv3.lib.max_compute;

import com.alipay.alps.flatv3.lib.SubGraphElement;

public class ArraySubGraphElement implements SubGraphElement {
    private Object[] currentData = null;

    public ArraySubGraphElement(Object[] data) {
        this.currentData = data;
    }
    public void setCurrentInput(Object[] data) {
        this.currentData = data;
    }
    @Override
    public String getString(String column) {
        return null;
    }

    @Override
    public int getInt(String column) {
        return 0;
    }

    @Override
    public float getFloat(String column) {
        return 0;
    }
}
