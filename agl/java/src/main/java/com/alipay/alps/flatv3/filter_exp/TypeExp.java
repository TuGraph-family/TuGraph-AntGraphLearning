package com.alipay.alps.flatv3.antlr2;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class TypeExp extends Expression<String> {
    public int indexedVariableIdx = -1;
    public int seedVariableIdx = -1;
    public ArrayList<String> variables = new ArrayList<>();
    public String cmpOp;

    public TypeExp(String leftExpression, String cmpOp, String rightExpression) {
        this.cmpOp = cmpOp;
        parseExp(leftExpression, 1);
        parseExp(rightExpression, -1);
    }

    private void parseExp(String expression, int sign) {
        String delimitor = "(),";
        StringTokenizer x = new StringTokenizer(expression, delimitor, true);
        int op = 1;
        while (x.hasMoreTokens()) {
            String p = x.nextToken();
            if (delimitor.contains(p)) {
                op = p.charAt(0) == '-' ? -1 : 1;
            } else {
                if (p.startsWith("INDEX")) {
                    indexedVariableIdx = variables.size();
                } else if (p.startsWith("SEED")) {
                    seedVariableIdx = variables.size();
                }
                variables.add(p);
            }
        }
    }

    @Override
    public boolean eval(String indexVaue, String seedValue) {
        boolean equal = false;
        if (cmpOp.compareTo("IN") == 0 || cmpOp.compareTo("=") == 0 || cmpOp.compareTo("==") == 0) {
            equal = true;
        }
        if (indexedVariableIdx >= 0 && seedVariableIdx >= 0) {
            return equal ? indexVaue.compareToIgnoreCase(seedValue) == 0 :  indexVaue.compareToIgnoreCase(seedValue) != 0;
        }

        for (int i = 0; i < variables.size(); i++) {
            if (i != indexedVariableIdx && i != seedVariableIdx) {
                if ((indexVaue != null && indexVaue.compareToIgnoreCase(variables.get(i)) == 0) || (seedValue != null && seedValue.compareToIgnoreCase(variables.get(i)) == 0)) {
                    return equal;
                }
            }
        }

        return !equal;
    }
}
