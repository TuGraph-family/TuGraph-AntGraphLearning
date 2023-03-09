package com.alipay.alps.flatv3.filter_exp;

import java.util.ArrayList;

/**
 Expression that can be evaluated.
 It allows for variables to be designated as the "index" or "seed" variables
 */
public abstract class Expression<T> {
    // index of the indexed variable in the variables list, -1 if not present
    protected int indexedVariableIdx = -1;
    // index of the seed variable in the variables list, -1 if not present
    protected int seedVariableIdx = -1;
    // list of variables in the expression
    protected ArrayList<String> variables = new ArrayList<>();
    // comparison operator used in the expression
    protected String cmpOp;

    /**
     * Gets the name of the indexed variable in the expression, if present.
     * @return the name of the indexed variable, or null if not present
     */
    public String getIndexName() {
        if (indexedVariableIdx >= 0) {
            return variables.get(indexedVariableIdx).substring("INDEX_".length());
        }
        return null;
    }

    /**
     * Gets the name of the seed variable in the expression, if present.
     * @return the name of the seed variable, or null if not present
     */
    public String getSeedValName() {
        if (seedVariableIdx >= 0) {
            return variables.get(seedVariableIdx);
        }
        return null;
    }

    /**
     * Evaluates the expression with the given index and seed values.
     * @param indexValue the index value to evaluate the expression on
     * @param seedValue the seed value to evaluate the expression on
     * @return true if the expression evaluates to true, false otherwise
     */
    public abstract boolean eval(T indexValue, T seedValue);
}
