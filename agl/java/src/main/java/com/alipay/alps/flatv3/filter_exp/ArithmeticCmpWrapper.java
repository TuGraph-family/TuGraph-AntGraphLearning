package com.alipay.alps.flatv3.filter_exp;

import com.antfin.agl.proto.sampler.ArithmeticOp;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ArithmeticCmpWrapper extends AbstractCmpWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(ArithmeticCmpWrapper.class);
    private Boolean hasLowerBound = null;

    public ArithmeticCmpWrapper(CmpExp cmpExp) {
        super(cmpExp);
    }

    public boolean eval(Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        double leftSum = calculateArithmetic(cmpExp.getLhsRPNList(), inputVariables);
        double rightSum = calculateArithmetic(cmpExp.getRhsRPNList(), inputVariables);
        return compare(leftSum, rightSum, cmpExp.getOp());
    }

    public double getDoubleValue(Element.Number num) {
        if (num.getDataCase() == Element.Number.DataCase.F) {
            return (double) num.getF();
        } else if (num.getDataCase() == Element.Number.DataCase.I) {
            return (double) num.getI();
        }
        throw new RuntimeException("invalid number");
    }

    public double calculateArithmetic(List<Element> elements, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        Stack<Double> vars = new Stack<>();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.getSymbolCase() == Element.SymbolCase.NUM) {
                vars.push(getDoubleValue(element.getNum()));
            } else if (element.getSymbolCase() == Element.SymbolCase.VAR) {
                VariableSource sourceType = element.getVar().getSource();
                String name = element.getVar().getName();
                vars.push(getDoubleValue(inputVariables.get(sourceType).get(name)));
            } else if (element.getSymbolCase() == Element.SymbolCase.OP) {
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
                } else {
                    throw new RuntimeException("invalid op");
                }
            } else {
                throw new RuntimeException("invalid element");
            }
        }
        return vars.peek();
    }

    // check if the expression has lower bound, if the expression has lower bound, 
    // then we can use binary search to find the first matched index.
    // if the expression has no lower bound, then it will has an upper bound.
    // we choose two values to evaluate the expression, one is Float.MAX_VALUE/-2.0F, the other is Float.MAX_VALUE/2.0F
    // if resultWithMinIndexVal is true, and resultWithMaxIndexVal is false, then the expression has lower bound,
    // otherwise the expression has upper bound
    public boolean hasLowerBound() {
        if (hasLowerBound != null) {
            return hasLowerBound;
        }
        Map<VariableSource, Map<String, Element.Number>> fakeVariablesWithMinIndexVal = new HashMap<>();
        fakeVariables(this.cmpExp.getLhsRPNList(), Float.MAX_VALUE / -2.0F, fakeVariablesWithMinIndexVal);
        fakeVariables(this.cmpExp.getRhsRPNList(), Float.MAX_VALUE / -2.0F, fakeVariablesWithMinIndexVal);
        boolean resultWithMinIndexVal = eval(fakeVariablesWithMinIndexVal);
        Map<VariableSource, Map<String, Element.Number>> fakeVariablesWithMaxIndexVal = new HashMap<>();
        fakeVariables(this.cmpExp.getLhsRPNList(), Float.MAX_VALUE / 2.0F, fakeVariablesWithMaxIndexVal);
        fakeVariables(this.cmpExp.getRhsRPNList(), Float.MAX_VALUE / 2.0F, fakeVariablesWithMaxIndexVal);
        boolean resultWithMaxIndexVal = eval(fakeVariablesWithMaxIndexVal);
        if (resultWithMinIndexVal && resultWithMaxIndexVal || (!resultWithMinIndexVal && !resultWithMaxIndexVal)) {
            LOG.error("Absolutely impossible, both values cannot be {}, exp:{}", resultWithMinIndexVal, cmpExp);
        }
        hasLowerBound = !resultWithMinIndexVal && resultWithMaxIndexVal;
        return hasLowerBound;
    }

    // set index value to indexVal, set other variables in elements to 0
    private void fakeVariables(List<Element> elements, float indexVal, Map<VariableSource, Map<String, Element.Number>> fakeVariables) {
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.getSymbolCase() == Element.SymbolCase.VAR) {
                VariableSource sourceType = element.getVar().getSource();
                if (!fakeVariables.containsKey(sourceType)) {
                    fakeVariables.put(sourceType, new HashMap<>());
                }
                String name = element.getVar().getName();
                float val = 0;
                if (sourceType == VariableSource.INDEX) {
                    val = indexVal;
                }
                fakeVariables.get(sourceType).put(name, Element.Number.newBuilder().setF(val).build());
            }
        }
    }
}
