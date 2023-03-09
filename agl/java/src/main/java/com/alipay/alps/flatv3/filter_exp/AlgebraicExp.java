package com.alipay.alps.flatv3.antlr2;

import com.antfin.alps.graph.common.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;


public class AlgebraicExp extends Expression<Double> {
    public ArrayList<Integer> signs = new ArrayList<>();

    /**
     Constructor for the AlgebraicExp class.
     @param leftExpression The left part of the expression.
     @param cmpOp The comparison operator.
     @param rightExpression The right part of the expression.
     */
    public AlgebraicExp(String leftExpression, String cmpOp, String rightExpression) {
        this.cmpOp = cmpOp;
        parseExp(leftExpression, 1);
        parseExp(rightExpression, -1);
    }

    /**
     Private helper method to parse the expression.
     @param expression The expression to be parsed.
     @param sign The sign of the expression (1 or -1).
     */
    private void parseExp(String expression, int sign) {
        String delimitor = "+-";
        StringTokenizer x = new StringTokenizer(expression, delimitor, true);
        int op = 1;
        while (x.hasMoreTokens()) {
            String p = x.nextToken();
            if (delimitor.contains(p)) {
                op = p.charAt(0) == '-' ? -1 : 1;
            } else {
                if (p.startsWith("INDEX_")) {
                    indexedVariableIdx = variables.size();
                } else if (p.startsWith("SEED_")) {
                    seedVariableIdx = variables.size();
                }
                variables.add(p);
                signs.add(sign * op);
            }
        }
    }

    /**
     Evaluates the expression with the given index and seed values.
     @param indexVaue The value of the index.
     @param seedValue The value of the seed.
     @return The result of the evaluation.
     */
    @Override
    public boolean eval(Double indexVaue, Double seedValue) {
        double sum = 0;
        for (int i = 0; i < variables.size(); i++) {
            if (i == indexedVariableIdx) {
                sum += indexVaue * signs.get(i);
            } else if (i == seedVariableIdx) {
                sum += seedValue * signs.get(i);
            } else {
                sum += Double.parseDouble(variables.get(i)) *  signs.get(i);
            }
        }
        if (cmpOp.compareTo("<=") == 0) {
            return sum <= 0;
        } else if (cmpOp.compareTo("<") == 0) {
            return sum < 0;
        } else if (cmpOp.compareTo(">=") == 0) {
            return sum >= 0;
        } else if (cmpOp.compareTo(">") == 0) {
            return sum > 0;
        } else if (cmpOp.compareTo("=") == 0 || cmpOp.compareTo("==") == 0) {
            return sum == 0;
        }
        return sum != 0;
    }

    @Override
    public String toString() {
        return "AlgebraicExp{" +
                "indexedVariableIdx=" + indexedVariableIdx +
                ", seedVariableIdx=" + seedVariableIdx +
                ", variables=" + Arrays.toString(variables.toArray()) +
                ", cmpOp='" + cmpOp + '\'' +
                '}';
    }
}