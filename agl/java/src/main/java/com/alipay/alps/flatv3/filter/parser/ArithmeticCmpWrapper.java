package com.alipay.alps.flatv3.filter.parser;

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

    public double getNumericValue(Element.Number num) {
        switch (num.getDataCase()) {
            case S:
                throw new RuntimeException("string is not supported");
            case F:
                return num.getF();
            case I:
                return (double) num.getI();
            default:
                throw new RuntimeException("invalid number");
        }
    }

    public double calculateArithmetic(List<Element> elements, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        Stack<Double> vars = new Stack<>();
        for (int i = 0; i < elements.size(); i++) {
            switch (elements.get(i).getSymbolCase()) {
                case NUM:
                    vars.push(getNumericValue(elements.get(i).getNum()));
                    break;
                case VAR:
                    VariableSource sourceType = elements.get(i).getVar().getSource();
                    String name = elements.get(i).getVar().getName();
                    vars.push(getNumericValue(inputVariables.get(sourceType).get(name)));
                    break;
                case OP:
                    double varRight = vars.pop();
                    double varLeft = vars.pop();
                    switch (elements.get(i).getOp()) {
                        case DIV:
                            vars.push(varLeft / varRight);
                            break;
                        case STAR:
                            vars.push(varLeft * varRight);
                            break;
                        case MOD:
                            vars.push(varLeft % varRight);
                            break;
                        case PLUS:
                            vars.push(varLeft + varRight);
                            break;
                        case MINUS:
                            vars.push(varLeft - varRight);
                            break;
                        default:
                            throw new RuntimeException("invalid op");
                    }
                    break;
                default:
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
