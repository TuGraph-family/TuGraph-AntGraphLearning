package com.alipay.alps.flatv3.filter_exp;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.List;

import com.antfin.agl.proto.sampler.ArithmeticOp;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompareExpUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CompareExpUtil.class);
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
        if (cmpExp.getLhsRPNCount() == 1 && cmpExp.getLhsRPN(0).getSymbolCase() == Element.SymbolCase.VAR) {
            variableSource = cmpExp.getLhsRPN(0).getVar();
            categoryElements = cmpExp.getRhsRPNList();
        } else if (cmpExp.getRhsRPNCount() == 1 && cmpExp.getRhsRPN(0).getSymbolCase() == Element.SymbolCase.VAR) {
            variableSource = cmpExp.getRhsRPN(0).getVar();
            categoryElements = cmpExp.getLhsRPNList();
        } else {
            throw new Exception("category expression should accept only one variable, category expression:{}" + cmpExp);
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

            if (cmpOp == CmpOp.LE || cmpOp == CmpOp.GE || cmpOp == CmpOp.EQ) {
                if (Math.abs(d1 - d2) < 0.001) {
                    return true;
                }
            } else if (cmpOp == CmpOp.NE) {
                return Math.abs(d1 - d2) >= 0.001;
            }
        }

        int compareResult = left.compareTo(right);
        if (cmpOp == CmpOp.LE) {
            return compareResult <= 0;
        } else if (cmpOp == CmpOp.LT) {
            return compareResult < 0;
        } else if (cmpOp == CmpOp.GE) {
            return compareResult >= 0;
        } else if (cmpOp == CmpOp.GT) {
            return compareResult > 0;
        } else if (cmpOp == CmpOp.EQ) {
            return compareResult == 0;
        } else if (cmpOp == CmpOp.NE) {
            return compareResult != 0;
        }
        return false;
    }

    public static boolean evalStringCompareExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        String left = getStringValue(cmpExp.getLhsRPN(0), inputVariables);
        String right = getStringValue(cmpExp.getRhsRPN(0), inputVariables);
        return compare(left, right, cmpExp.getOp());
    }

    public static boolean evalDoubleCompareExp(CmpExp cmpExp, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        double leftSum = calculateArithmetic(cmpExp.getLhsRPNList(), inputVariables);
        double rightSum = calculateArithmetic(cmpExp.getRhsRPNList(), inputVariables);
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
            VariableSource sourceType = element.getVar().getSource();
            String name = element.getVar().getName();
            return inputVariables.get(sourceType).get(name).getS();
        }
    }

    public static CmpOp parseCmpOp(String cmpOp) {
        if (cmpOp.compareToIgnoreCase("<=") == 0) {
            return CmpOp.LE;
        } else if (cmpOp.compareToIgnoreCase("<") == 0) {
            return CmpOp.LT;
        } else if (cmpOp.compareToIgnoreCase(">=") == 0) {
            return CmpOp.GE;
        } else if (cmpOp.compareToIgnoreCase(">") == 0) {
            return CmpOp.GT;
        } else if (cmpOp.compareToIgnoreCase("=") == 0 || cmpOp.compareToIgnoreCase("==") == 0) {
            return CmpOp.EQ;
        } else if (cmpOp.compareToIgnoreCase("!=") == 0) {
            return CmpOp.NE;
        } else if (cmpOp.compareToIgnoreCase("IN") == 0) {
            return CmpOp.IN;
        } else if (cmpOp.compareToIgnoreCase("NOT IN") == 0) {
            return CmpOp.NOT_IN;
        }
        LOG.error("not supported comparison op:{}", cmpOp);
        return CmpOp.CMP_UNKNOWN;
    }

    public static double calculateArithmetic(List<Element> elements, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        Stack<Double> vars = new Stack<>();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.getSymbolCase() == Element.SymbolCase.NUM) {
                vars.push(getDoubleValue(element.getNum()));
            } else if (element.hasVar()) {
                VariableSource sourceType = element.getVar().getSource();
                String name = element.getVar().getName();
                vars.push(getDoubleValue(inputVariables.get(sourceType).get(name)));
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
            }
        }
        return vars.peek();
    }
}
