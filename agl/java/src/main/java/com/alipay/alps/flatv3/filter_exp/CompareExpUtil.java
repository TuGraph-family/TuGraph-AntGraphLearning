package com.alipay.alps.flatv3.filter_exp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.List;

import com.antfin.ai.alps.graph.flat.sample.ArithmeticOp;
import com.antfin.ai.alps.graph.flat.sample.CmpExp;
import com.antfin.ai.alps.graph.flat.sample.CmpOp;
import com.antfin.ai.alps.graph.flat.sample.Element;
import com.antfin.ai.alps.graph.flat.sample.VariableSource;

public class CompareExpUtil {
    public static <T> boolean checkCategory(T left, List<T> categories, CmpOp cmpOp) {
        if (cmpOp == CmpOp.IN) {
            return categories.contains(left);
        } else if (cmpOp == CmpOp.NOT_IN) {
            return !categories.contains(left);
        }
        return false;
    }
    public static boolean evalCategoryExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Number>> inputVariables) throws Exception {
        Element.Variable variableSource = null;
        List<Element> categoryElements = null;
        if (cmpExp.getLeftFormulaRPNCount() == 1 && cmpExp.getLeftFormulaRPN(0).getSymbolCase() == Element.SymbolCase.VARIABLE) {
            variableSource = cmpExp.getLeftFormulaRPN(0).getVariable();
            categoryElements = cmpExp.getRightFormulaRPNList();
        } else if (cmpExp.getRightFormulaRPNCount() == 1 && cmpExp.getRightFormulaRPN(0).getSymbolCase() == Element.SymbolCase.VARIABLE) {
            variableSource = cmpExp.getRightFormulaRPN(0).getVariable();
            categoryElements = cmpExp.getLeftFormulaRPNList();
        } else {
            throw new Exception("category expression should accept one variable");
        }
        Element.Number variable = inputVariables.get(variableSource.getSource()).get(variableSource.getName());
        if (variable.getDataCase() == Element.Number.DataCase.S) {
            List<String> categories = new ArrayList<>();
            for (int i = 0; i < categoryElements.size(); i++) {
                categories.add(categoryElements.get(i).getNum().getS());
            }
            return checkCategory(variable.getS(), categories, cmpExp.getOp());
        } else if (variable.getDataCase() == Element.Number.DataCase.F) {
            List<Float> categories = new ArrayList<>();
            for (int i = 0; i < categoryElements.size(); i++) {
                categories.add(categoryElements.get(i).getNum().getF());
            }
            return checkCategory(variable.getF(), categories, cmpExp.getOp());
        } else {
            List<Long> categories = new ArrayList<>();
            for (int i = 0; i < categoryElements.size(); i++) {
                categories.add(categoryElements.get(i).getNum().getI());
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

    public static boolean evalStringCompareExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        String left = getStringValue(cmpExp.getLeftFormulaRPN(0), inputVariables);
        String right = getStringValue(cmpExp.getRightFormulaRPN(0), inputVariables);
        return compare(left, right, cmpExp.getOp());
    }

    public static boolean evalDoubleCompareExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        double leftSum = calculateArithmetic(cmpExp.getLeftFormulaRPNList(), inputVariables);
        double rightSum = calculateArithmetic(cmpExp.getRightFormulaRPNList(), inputVariables);
        return compare(leftSum, rightSum, cmpExp.getOp());
    }

    public static double getDoubleValue(Element.Number num) {
        if (num.getDataCase() == Element.Number.DataCase.F) {
            return (double) num.getF();
        } else if (num.getDataCase() == Element.Number.DataCase.I) {
            return (double) num.getI();
        }
        return -1;
    }

    public static String getStringValue(Element element, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        if (element.getSymbolCase() == Element.SymbolCase.NUM) {
            return element.getNum().getS();
        } else {
            VariableSource sourceType = element.getVariable().getSource();
            String name = element.getVariable().getName();
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

    public static double calculateArithmetic(List<Element> elements, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        Stack<Double> vars = new Stack<>();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.getSymbolCase() == Element.SymbolCase.NUM) {
                vars.push(getDoubleValue(element.getNum()));
                System.out.println("---calculate element:" + element + " push Num " + vars.peek());
            } else if (element.hasVariable()) {
                VariableSource sourceType = element.getVariable().getSource();
                String name = element.getVariable().getName();
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
