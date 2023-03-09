package com.alipay.alps.flatv3.filter_exp;

import java.util.ArrayList;

public abstract class Expression<T> {
    public int indexedVariableIdx = -1;
    public int seedVariableIdx = -1;
    public ArrayList<String> variables = new ArrayList<>();
    public String cmpOp;

    public String getIndexName() {
        if (indexedVariableIdx >= 0) {
            return variables.get(indexedVariableIdx).substring("INDEX_".length());
        }
        return null;
    }

    public String getSeedValName() {
        if (seedVariableIdx >= 0) {
            return variables.get(seedVariableIdx);
        }
        return null;
    }
    /**
     Evaluates the expression with the given index and seed values.
     @param indexVaue The value of the index.
     @param seedValue The value of the seed.
     @return The result of the evaluation.
     */
    public abstract boolean eval(T indexVaue, T seedValue);
}
