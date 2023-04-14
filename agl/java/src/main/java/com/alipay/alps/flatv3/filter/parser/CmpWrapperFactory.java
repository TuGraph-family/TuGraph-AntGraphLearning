package com.alipay.alps.flatv3.filter.parser;

import com.antfin.agl.proto.sampler.CmpExp;

public class CmpWrapperFactory {
    public static AbstractCmpWrapper createCmpWrapper(CmpExp cmpExp) {
        switch (cmpExp.getOp()) {
            case EQ:
            case NE:
            case GT:
            case GE:
            case LT:
            case LE:
                return new ArithmeticCmpWrapper(cmpExp);
            case IN:
            case NOT_IN:
                return new CategoryCmpWrapper(cmpExp);
            default:
                throw new RuntimeException("Unsupported cmp op: " + cmpExp.getOp());
        }
    }
}
