package com.alipay.alps.flatv3.lib;

public interface SubGraphElementIterator {
    boolean hasNext();

    SubGraphElement getNext();

    void reset();
}
