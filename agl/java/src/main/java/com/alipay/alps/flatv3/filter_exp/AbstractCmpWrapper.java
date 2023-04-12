package com.alipay.alps.flatv3.filter_exp;

import com.antfin.agl.proto.sampler.ArithmeticOp;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.VariableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class AbstractCmpWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractCmpWrapper.class);
    protected CmpExp cmpExp;
    protected String indexColumn;

    public static CmpOp parseCmpOp(String cmpOp) {
        switch (cmpOp.toLowerCase()) {
            case "<=":
                return CmpOp.LE;
            case "<":
                return CmpOp.LT;
            case ">=":
                return CmpOp.GE;
            case ">":
                return CmpOp.GT;
            case "=":
            case "==":
                return CmpOp.EQ;
            case "!=":
                return CmpOp.NE;
            case "in":
                return CmpOp.IN;
            case "not in":
                return CmpOp.NOT_IN;
            default:
                LOG.error("not supported comparison op:{}", cmpOp);
                return CmpOp.CMP_UNKNOWN;
        }
    }

    public String getIndexColumn() {
        return indexColumn;
    }

    public AbstractCmpWrapper(CmpExp cmpExp) {
        this.cmpExp = cmpExp;
        parseIndexName(this.cmpExp.getLhsRPNList());
        parseIndexName(this.cmpExp.getRhsRPNList());
    }

    public CmpExp getCmpExp() {
        return cmpExp;
    }

    private void parseIndexName(List<Element> rpnList) {
        boolean foundDIVorMODOperator = false;
        boolean foundIndex = false;
        for (Element element : rpnList) {
            switch (element.getSymbolCase()) {
                case VAR:
                    if (element.getVar().getSource() == VariableSource.INDEX) {
                        if (indexColumn != null) {
                            LOG.error("Only support at most one index column in each compare expression, but the exp breaks this rule. {}", cmpExp);
                        }
                        indexColumn = element.getVar().getName();
                        foundIndex = true;
                    }
                    break;
                case OP:
                    if (element.getOp() == ArithmeticOp.DIV || element.getOp() == ArithmeticOp.MOD) {
                        foundDIVorMODOperator = true;
                    }
                    break;
                case NUM:
                    // do nothing
                    break;
                default:
                    LOG.error("Unsupported element type in RPN list. {}", element);
            }
        }
        if (foundIndex && foundDIVorMODOperator) {
            LOG.error("DIV and MOD operators are not supported for formulas containing index. {}", cmpExp);
        }
    }

    protected <T extends Comparable<T>> boolean compare(T left, T right, CmpOp cmpOp) {
        if (left instanceof Double && right instanceof Double) {
            Double diff = (Double) left - (Double) right;
            if (Math.abs(diff) < 0.001) {
                return compareDiff((int) (diff * 100000), cmpOp);
            }
        }

        return compareDiff(left.compareTo(right), cmpOp);
    }

    private boolean compareDiff(int diff, CmpOp cmpOp) {
        switch (cmpOp) {
            case LE:
                return diff <= 0;
            case LT:
                return diff < 0;
            case GE:
                return diff >= 0;
            case GT:
                return diff > 0;
            case EQ:
                return diff == 0;
            case NE:
                return diff != 0;
            default:
                LOG.error("not supported comparison op:{}", cmpOp);
                return false;
        }
    }

    public abstract boolean eval(Map<VariableSource, Map<String, Element.Number>> inputVariables);
}
