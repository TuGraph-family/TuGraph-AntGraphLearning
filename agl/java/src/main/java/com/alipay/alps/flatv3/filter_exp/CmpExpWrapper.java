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

public abstract class CmpExpWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(CmpExpWrapper.class);
    protected CmpExp cmpExp;
    protected String indexColumn;
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

    public String getIndexColumn() {
        return indexColumn;
    }

    public CmpExpWrapper(CmpExp cmpExp) {
        this.cmpExp = cmpExp;
        getIndexName(this.cmpExp.getLhsRPNList(), true);
        getIndexName(this.cmpExp.getRhsRPNList(), false);
    }

    public CmpExp getCmpExp() {
        return cmpExp;
    }

    private void getIndexName(List<Element> rpnList, boolean isLeft) {
        boolean foundDIVorMODOperator = false;
        boolean foundIndex = false;
        for (Element element : rpnList) {
            if (element.getSymbolCase() == Element.SymbolCase.VAR && element.getVar().getSource() == VariableSource.INDEX) {
                if (indexColumn != null) {
                    LOG.error("Only support at most one index column in each compare expression, but the exp breaks this rule. {}", cmpExp);
                }
                indexColumn = element.getVar().getName();
                foundIndex = true;
            } else if (element.getSymbolCase() == Element.SymbolCase.OP) {
                if (element.getOp() == ArithmeticOp.DIV || element.getOp() == ArithmeticOp.MOD) {
                    foundDIVorMODOperator = true;
                }
            }
        }
        if (foundIndex && foundDIVorMODOperator) {
            LOG.error("DIV and MOD operators are not supported for formulas containing index. {}", cmpExp);
        }
    }

    protected <T extends Comparable<T>> boolean compare(T left, T right, CmpOp cmpOp) {
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

    public abstract boolean eval(Map<VariableSource, Map<String, Element.Number>> inputVariables);
}
