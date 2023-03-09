package com.alipay.alps.flatv3.filter_exp;

import java.util.Arrays;
import java.util.StringTokenizer;


/**
 CategoryExp evaluates a comparison between two string expressions
 It allows for variables to be designated as the "index" or "seed" variables
 Examples of CategoryExp could be:
    SEED.1 IN (item, user)
    INDEX.type NOT IN (item, user)
    SEED.1 = item
    SEED.1 == item
    SEED.1 != item
 */
public class CategoryExp extends Expression<String> {

    /**
     Constructs a new CategoryExp object with the given left and right string expressions
     and comparison operator.
     @param leftExpression the left-hand side expression
     @param cmpOp the comparison operator
     @param rightExpression the right-hand side expression
     */
    public CategoryExp(String leftExpression, String cmpOp, String rightExpression) {
        this.cmpOp = cmpOp;
        parseExp(leftExpression);
        parseExp(rightExpression);
    }

    /**
     Parses a given expression string into its component variables and adds them to the list of variables for this expression.
     If the variable starts with "INDEX." or "index.", it designates that variable as the index variable.
     If the variable starts with "SEED." or "seed.", it designates that variable as the seed variable.
     @param expression the expression to parse
     */
    private void parseExp(String expression) {
        String delimitor = "(),";
        StringTokenizer x = new StringTokenizer(expression.replaceAll("\\s+",""), delimitor, true);
        while (x.hasMoreTokens()) {
            String p = x.nextToken();
            if (!delimitor.contains(p)) {
                if (p.startsWith("INDEX.") || p.startsWith("index.")) {
                    indexedVariableIdx = variables.size();
                } else if (p.startsWith("SEED.") || p.startsWith("seed.")) {
                    seedVariableIdx = variables.size();
                }
                variables.add(p);
            }
        }
    }

    /**
     Evaluates the CategoryExp object using the given index value and seed value.
     If the comparison operator is "IN", "=", or "==", returns true if the values are equal and false otherwise.
     If both an index variable and seed variable have been designated, compares the index value to the seed value
     using the comparison operator.
     Otherwise, compares each variable in the list to the index value and/or seed value, and returns true if any match
     using the comparison operator.
     @param indexValue the value to use for the index variable
     @param seedValue the value to use for the seed variable
     @return true if the expression is true based on the given index and seed values, false otherwise
     */
    @Override
    public boolean eval(String indexValue, String seedValue) {
        boolean equal = false;
        if (cmpOp.compareTo("IN") == 0 || cmpOp.compareTo("=") == 0 || cmpOp.compareTo("==") == 0) {
            equal = true;
        }
        if (indexedVariableIdx >= 0 && seedVariableIdx >= 0) {
            return equal ? indexValue.compareToIgnoreCase(seedValue) == 0 :  indexValue.compareToIgnoreCase(seedValue) != 0;
        }

        for (int i = 0; i < variables.size(); i++) {
            if (i != indexedVariableIdx && i != seedVariableIdx) {
                if ((indexValue != null && indexedVariableIdx != -1 && indexValue.compareToIgnoreCase(variables.get(i)) == 0) ||
                    (seedValue != null && seedVariableIdx != -1 && seedValue.compareToIgnoreCase(variables.get(i)) == 0)) {
                    return equal;
                }
            }
        }

        return !equal;
    }


    @Override
    public String toString() {
        return "TypeExp{" +
                "indexedVariableIdx=" + indexedVariableIdx +
                ", seedVariableIdx=" + seedVariableIdx +
                ", variables=" + Arrays.toString(variables.toArray()) +
                ", cmpOp='" + cmpOp + '\'' +
                '}';
    }
}
