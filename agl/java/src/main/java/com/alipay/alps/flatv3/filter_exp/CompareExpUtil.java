package com.alipay.alps.flatv3.filter_exp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.List;

import com.antfin.ai.alps.graph.aglstore.loader.ArithmeticOp;
import com.antfin.ai.alps.graph.aglstore.loader.CmpExp;
import com.antfin.ai.alps.graph.aglstore.loader.CmpOp;
import com.antfin.ai.alps.graph.aglstore.loader.Element;
import com.antfin.ai.alps.graph.aglstore.loader.VariableSource;

public class CompareExpUtil {
    public static <T> boolean checkCategory(T left, List<T> categories, CmpOp cmpOp) {
        if (cmpOp == CmpOp.IN) {
            return categories.contains(left);
        } else if (cmpOp == CmpOp.NOT_IN) {
            return !categories.contains(left);
        }
        return false;
    }
    public static boolean evalCategoryExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Num>> inputVariables) throws Exception {
        Element.Var variableSource = null;
        List<Element> categoryElements = null;
        if (cmpExp.getLeftCount() == 1 && cmpExp.getLeft(0).getVarCase() == Element.VarCase.V) {
            variableSource = cmpExp.getLeft(0).getV();
            categoryElements = cmpExp.getRightList();
        } else if (cmpExp.getRightCount() == 1 && cmpExp.getRight(0).getVarCase() == Element.VarCase.V) {
            variableSource = cmpExp.getRight(0).getV();
            categoryElements = cmpExp.getLeftList();
        } else {
            throw new Exception("category expression should accept one variable");
        }
        Element.Num variable = inputVariables.get(variableSource.getSource()).get(variableSource.getName());
        if (variable.getDataCase() == Element.Num.DataCase.S) {
            List<String> categories = new ArrayList<>();
            for (int i = 0; i < categoryElements.size(); i++) {
                categories.add(categoryElements.get(i).getN().getS());
            }
            return checkCategory(variable.getS(), categories, cmpExp.getOp());
        } else if (variable.getDataCase() == Element.Num.DataCase.F) {
            List<Float> categories = new ArrayList<>();
            for (int i = 0; i < categoryElements.size(); i++) {
                categories.add(categoryElements.get(i).getN().getF());
            }
            return checkCategory(variable.getF(), categories, cmpExp.getOp());
        } else {
            List<Long> categories = new ArrayList<>();
            for (int i = 0; i < categoryElements.size(); i++) {
                categories.add(categoryElements.get(i).getN().getI());
            }
            return checkCategory(variable.getI(), categories, cmpExp.getOp());
        }
    }

    public static <T extends Comparable<T>> boolean compare(T left, T right, CmpOp cmpOp) {
        if (left instanceof Double && right instanceof Double) {
            Double d1 = (Double) left;
            Double d2 = (Double) right;

            if (cmpOp == CmpOp.LESS_EQ || cmpOp == CmpOp.GREATER_EQ || cmpOp == CmpOp.EQ) {
                if (Math.abs(d1 - d2) < 0.001) return true;
            } else if (cmpOp == CmpOp.NOT_EQ) {
                return Math.abs(d1 - d2) >= 0.001;
            }
        }

        int compareResult = left.compareTo(right);
        if (cmpOp == CmpOp.LESS_EQ) {
            return compareResult <= 0;
        } else if (cmpOp == CmpOp.LESS) {
            return compareResult < 0;
        } else if (cmpOp == CmpOp.GREATER_EQ) {
            return compareResult >= 0;
        } else if (cmpOp == CmpOp.GREATER) {
            return compareResult > 0;
        } else if (cmpOp == CmpOp.EQ) {
            return compareResult == 0;
        } else if (cmpOp == CmpOp.NOT_EQ) {
            return compareResult != 0;
        }
        return false;
    }

    public static boolean evalStringCompareExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Num>> inputVariables) {
        String left = getStringValue(cmpExp.getLeft(0), inputVariables);
        String right = getStringValue(cmpExp.getRight(0), inputVariables);
        return compare(left, right, cmpExp.getOp());
    }

    public static boolean evalDoubleCompareExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Num>> inputVariables) {
        double leftSum = calculateArithmetic(cmpExp.getLeftList(), inputVariables);
        double rightSum = calculateArithmetic(cmpExp.getRightList(), inputVariables);
        return compare(leftSum, rightSum, cmpExp.getOp());
    }

    public static double getDoubleValue(Element.Num num) {
        if (num.getDataCase() == Element.Num.DataCase.F) {
            return (double) num.getF();
        } else if (num.getDataCase() == Element.Num.DataCase.I) {
            return (double) num.getI();
        }
        return -1;
    }

    public static String getStringValue(Element element, Map<VariableSource, Map<String, Element.Num>> inputVariables) {
        if (element.getVarCase() == Element.VarCase.N) {
            return element.getN().getS();
        } else {
            VariableSource sourceType = element.getV().getSource();
            String name = element.getV().getName();
            return inputVariables.get(sourceType).get(name).getS();
        }
    }

    public static CmpOp parseCmpOp(String cmpOp) throws Exception {
        if (cmpOp.compareToIgnoreCase("<=") == 0) {
            return CmpOp.LESS_EQ;
        } else if (cmpOp.compareToIgnoreCase("<") == 0) {
            return CmpOp.LESS;
        } else if (cmpOp.compareToIgnoreCase(">=") == 0) {
            return CmpOp.GREATER_EQ;
        } else if (cmpOp.compareToIgnoreCase(">") == 0) {
            return CmpOp.GREATER;
        } else if (cmpOp.compareToIgnoreCase("=") == 0 || cmpOp.compareToIgnoreCase("==") == 0) {
            return CmpOp.EQ;
        } else if (cmpOp.compareToIgnoreCase("!=") == 0) {
            return CmpOp.NOT_EQ;
        } else if (cmpOp.compareToIgnoreCase("IN") == 0) {
            return CmpOp.IN;
        } else if (cmpOp.compareToIgnoreCase("NOT IN") == 0) {
            return CmpOp.NOT_IN;
        }
        throw new Exception("not supported comparison op:" + cmpOp);
    }

    public static double calculateArithmetic(List<Element> elements, Map<VariableSource, Map<String, Element.Num>> inputVariables) {
        Stack<Double> vars = new Stack<>();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.getVarCase() == Element.VarCase.N) {
                vars.push(getDoubleValue(element.getN()));
                System.out.println("---calculate element:" + element + " push Num " + vars.peek());
            } else if (element.hasV()) {
                VariableSource sourceType = element.getV().getSource();
                String name = element.getV().getName();
                vars.push(getDoubleValue(inputVariables.get(sourceType).get(name)));
                System.out.println("---calculate element:" + element + " push Var " + vars.peek());
            } else {
                double varRight = vars.pop();
                double varLeft = vars.pop();
                if (element.getOp() == ArithmeticOp.DIV) {
                    vars.push(varLeft / varRight);
                } else if (element.getOp() == ArithmeticOp.STAR) {
                    vars.push(varLeft * varRight);
                } else if (element.getOp() == ArithmeticOp.MOD) {
                    vars.push(varLeft % varRight);
                } else if (element.getOp() == ArithmeticOp.PLUS) {
                    vars.push(varLeft + varRight);
                } else if (element.getOp() == ArithmeticOp.MINUS) {
                    vars.push(varLeft - varRight);
                }
                System.out.println("---calculate op:" + element.getOp() + " get var " + vars.peek());
            }
        }
        System.out.println("---final result:" + Arrays.toString(vars.toArray()));
        return vars.peek();
    }
}
