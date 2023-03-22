package com.alipay.alps.flatv3.filter_exp;

import com.alipay.alps.flatv3.antlr4.FilterBaseVisitor;
import com.alipay.alps.flatv3.antlr4.FilterParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.antfin.agl.proto.sampler.ArithmeticOp;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.LogicOp;
import com.antfin.agl.proto.sampler.VariableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterConditionVisitor extends FilterBaseVisitor<LogicExps.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(FilterConditionParser.class);

    private LogicExps.Builder logicExpsBuilder = LogicExps.newBuilder();
    private List<Element> arithmeticValOps = null;
    private List<Element> arithmeticValOpsLeft = new ArrayList<>();
    private List<Element> arithmeticValOpsRight = new ArrayList<>();
    private HashMap<String, String> arithmeticOpMap = new HashMap<>();
    private HashSet<String> sourceTypeSet = new HashSet<>();

    public FilterConditionVisitor() {
        arithmeticOpMap.put("-", "MINUS");
        arithmeticOpMap.put("+", "PLUS");
        arithmeticOpMap.put("*", "STAR");
        arithmeticOpMap.put("/", "DIV");
        arithmeticOpMap.put("%", "MOD");
        for (VariableSource var : VariableSource.values()) {
            sourceTypeSet.add(var.name());
        }
    }

    @Override public LogicExps.Builder visitAndExp(FilterParser.AndExpContext ctx) {
        String op = ctx.getChild(1).getText();
        visitChildren(ctx);
        logicExpsBuilder.addExpRPN(LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf(op.toUpperCase())));
        return logicExpsBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public LogicExps.Builder visitOrExp(FilterParser.OrExpContext ctx) {
        String op = ctx.getChild(1).getText();
        visitChildren(ctx);
        logicExpsBuilder.addExpRPN(LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf(op.toUpperCase())));
        return logicExpsBuilder;
    }

    private LogicExps.Builder visitCompareOrCategoryExp(FilterParser.ExprContext ctx) {
        arithmeticValOpsLeft.clear();
        arithmeticValOpsRight.clear();
        arithmeticValOps = arithmeticValOpsLeft;
        visit(ctx.getChild(0));
        arithmeticValOps = arithmeticValOpsRight;

        String op = ctx.getChild(1).getText();
        if (op.compareToIgnoreCase("not") == 0 && ctx.getChild(2).getText().compareToIgnoreCase("in") == 0) {
            op = "not in";
            visit(ctx.getChild(3));
        } else {
            visit(ctx.getChild(2));
        }
        CmpExp.Builder cmpExpBuilder = CmpExp.newBuilder();
        cmpExpBuilder.addAllLhsRPN(arithmeticValOpsLeft);
        cmpExpBuilder.addAllRhsRPN(arithmeticValOpsRight);
        cmpExpBuilder.setOp(CmpExpWrapper.parseCmpOp(op));
        // reverse polish representation
        logicExpsBuilder.addExpRPN(LogicExps.ExpOrOp.newBuilder().setExp(cmpExpBuilder));
        return logicExpsBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public LogicExps.Builder visitCompareExp(FilterParser.CompareExpContext ctx) {
        return visitCompareOrCategoryExp(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public LogicExps.Builder visitCategoryExp(FilterParser.CategoryExpContext ctx) {
        return visitCompareOrCategoryExp(ctx);
    }

    @Override public LogicExps.Builder visitStarDivExp(FilterParser.StarDivExpContext ctx) {
        String op = ctx.getChild(1).getText();
        visitChildren(ctx);
        arithmeticValOps.add(Element.newBuilder().setOp(ArithmeticOp.valueOf(arithmeticOpMap.get(op))).build());
        return logicExpsBuilder;
    }

    @Override public LogicExps.Builder visitPlusMinusExp(FilterParser.PlusMinusExpContext ctx) {
        String op = ctx.getChild(1).getText();
        visitChildren(ctx);
        arithmeticValOps.add(Element.newBuilder().setOp(ArithmeticOp.valueOf(arithmeticOpMap.get(op))).build());
        return logicExpsBuilder;
    }

    @Override public LogicExps.Builder visitColumnExp(FilterParser.ColumnExpContext ctx) {
        String exp = ctx.getChild(0).getText();
        Element.Builder elementBuilder = Element.newBuilder();
        String arrs[] = exp.split("\\.");
        if (sourceTypeSet.contains(arrs[0].toUpperCase())) {
            Element.Variable.Builder elementVarBuilder = Element.newBuilder().getVarBuilder().setSource(VariableSource.valueOf(arrs[0].toUpperCase()));
            elementVarBuilder.setName(arrs.length <= 1 ? "" : arrs[1]);
            elementBuilder.setVar(elementVarBuilder);
        } else {
            Element.Number.Builder elementNumBuilder = Element.newBuilder().getNumBuilder().setS(exp);
            elementBuilder.setNum(elementNumBuilder);
        }
        visitChildren(ctx);
        arithmeticValOps.add(elementBuilder.build());
        return logicExpsBuilder;
    }

    @Override public LogicExps.Builder visitLiteralExp(FilterParser.LiteralExpContext ctx) {
        String exp = ctx.getChild(0).getText();
        Element.Builder elementBuilder = Element.newBuilder();
        Element.Number.Builder elementNumBuilder = Element.newBuilder().getNumBuilder();
        if (exp.startsWith("'") && exp.endsWith("'") || exp.startsWith("\"") && exp.endsWith("\"")) {
            System.out.println("--------exp is str: " + exp);
            String val = exp.substring(1, exp.length() - 1);
            System.out.println("--------exp is val: " + val);
            elementNumBuilder.setS(val);
        } else if (exp.matches("\\d+(\\.\\d+)?")) {
            elementNumBuilder.setF(Float.parseFloat(exp));
        } else {
            elementNumBuilder.setI(Long.parseLong(exp));
        }
        elementBuilder.setNum(elementNumBuilder);
        visitChildren(ctx);
        arithmeticValOps.add(elementBuilder.build());
        return logicExpsBuilder;
    }
}
