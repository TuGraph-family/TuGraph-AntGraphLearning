package com.alipay.alps.flatv3.filter_exp;

import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CategoryCmpWrapper extends AbstractCmpWrapper {
    public CategoryCmpWrapper(CmpExp cmpExp) {
        super(cmpExp);
    }

    public boolean eval(Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        String left = getStringValue(cmpExp.getLhsRPN(0), inputVariables);
        if (cmpExp.getOp() == CmpOp.IN || cmpExp.getOp() == CmpOp.NOT_IN) {
            Set<String> rightTypes = new HashSet<>();
            for (int i = 0; i < cmpExp.getRhsRPNCount(); i++) {
                rightTypes.add(getStringValue(cmpExp.getRhsRPN(i), inputVariables));
            }
            return cmpExp.getOp() == CmpOp.IN ? rightTypes.contains(left) : !rightTypes.contains(left);
        } else {
            String right = getStringValue(cmpExp.getRhsRPN(0), inputVariables);
            return compare(left, right, cmpExp.getOp());
        }
    }

    // TODO: support more types, like long, etc.
    public String getStringValue(Element element, Map<VariableSource, Map<String, Element.Number>> inputVariables) {
        if (element.getSymbolCase() == Element.SymbolCase.NUM) {
            return element.getNum().getS();
        } else {
            VariableSource sourceType = element.getVar().getSource();
            String name = element.getVar().getName();
            return inputVariables.get(sourceType).get(name).getS();
        }
    }
}
