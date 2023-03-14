package com.alipay.alps.flatv3.filter_exp;

import com.alipay.alps.flatv3.antlr4.FilterBaseVisitor;
import com.alipay.alps.flatv3.antlr4.FilterParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.antfin.ai.alps.graph.flat.sample.CmpExp;
import com.antfin.ai.alps.graph.flat.sample.Element;
import com.antfin.ai.alps.graph.flat.sample.LogicExps;
import com.antfin.ai.alps.graph.flat.sample.LogicOp;
import com.antfin.ai.alps.graph.flat.sample.VariableSource;

public class FilterConditionVisitor extends FilterBaseVisitor<LogicExps.Builder> {
    private LogicExps.Builder logicExpsBuilder = LogicExps.newBuilder();
    private List<Element> arithmeticValOps = null;
    private List<Element> arithmeticValOpsLeft = new ArrayList<>();
    private List<Element> arithmeticValOpsRight = new ArrayList<>();
    private HashMap<String, Integer> arithmeticOpMap = new HashMap<>();
    private HashSet<String> sourceTypeSet = new HashSet<>();

    public FilterConditionVisitor() {
        arithmeticOpMap.put("-", 0);
        arithmeticOpMap.put("+", 1);
        arithmeticOpMap.put("*", 2);
        arithmeticOpMap.put("/", 3);
        arithmeticOpMap.put("%", 4);
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

    @Override public LogicExps.Builder visitStarDivExp(FilterParser.StarDivExpContext ctx) {
        String op = ctx.getChild(1).getText();
        visitChildren(ctx);
        arithmeticValOps.add(Element.newBuilder().setOpValue(arithmeticOpMap.get(op)).build());
        if (ctx.parent instanceof FilterParser.CompareExpContext || ctx.parent instanceof FilterParser.CategoryExpContext) {
            arithmeticValOps = arithmeticValOpsRight;
        }
        return logicExpsBuilder;
    }

    @Override public LogicExps.Builder visitPlusMinusExp(FilterParser.PlusMinusExpContext ctx) {
        String op = ctx.getChild(1).getText();
        visitChildren(ctx);
        arithmeticValOps.add(Element.newBuilder().setOpValue(arithmeticOpMap.get(op)).build());
        if (ctx.parent instanceof FilterParser.CompareExpContext || ctx.parent instanceof FilterParser.CategoryExpContext) {
            arithmeticValOps = arithmeticValOpsRight;
        }
        return logicExpsBuilder;
    }

    @Override public LogicExps.Builder visitColumnExp(FilterParser.ColumnExpContext ctx) {
        String exp = ctx.getChild(0).getText();
        visitChildren(ctx);
        String arrs[] = exp.split("\\.");
        if (sourceTypeSet.contains(arrs[0].toUpperCase())) {
            Element.Variable.Builder elementVarBuilder = Element.newBuilder().getVariableBuilder().setSource(VariableSource.valueOf(arrs[0].toUpperCase()));
            elementVarBuilder.setName(arrs.length <= 1 ? "" : arrs[1]);
            arithmeticValOps.add(Element.newBuilder().setVariable(elementVarBuilder).build());
        } else {
            Element.Number.Builder elementNumBuilder = Element.newBuilder().getNumBuilder().setS(exp);
            arithmeticValOps.add(Element.newBuilder().setNum(elementNumBuilder).build());
        }
        if (ctx.parent instanceof FilterParser.CompareExpContext || ctx.parent instanceof FilterParser.CategoryExpContext) {
            arithmeticValOps = arithmeticValOpsRight;
        }
        return logicExpsBuilder;
    }

    @Override public LogicExps.Builder visitLiteralExp(FilterParser.LiteralExpContext ctx) {
        String exp = ctx.getChild(0).getText();
        visitChildren(ctx);

        Element.Number.Builder elementNumBuilder = Element.newBuilder().getNumBuilder();
        if (exp.matches("\\d+(\\.\\d+)?")) {
            elementNumBuilder.setF(Float.parseFloat(exp));
        } else {
            elementNumBuilder.setF(Integer.parseInt(exp)); // setInt?
        }
        arithmeticValOps.add(Element.newBuilder().setNum(elementNumBuilder).build());
        if (ctx.parent instanceof FilterParser.CompareExpContext || ctx.parent instanceof FilterParser.CategoryExpContext) {
            arithmeticValOps = arithmeticValOpsRight;
        }
        return logicExpsBuilder;
    }
    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public LogicExps.Builder visitCompareExp(FilterParser.CompareExpContext ctx) {
        String op = ctx.getChild(1).getText();
        arithmeticValOpsLeft.clear();
        arithmeticValOpsRight.clear();
        arithmeticValOps = arithmeticValOpsLeft;

        visitChildren(ctx);

        // generate compare expression from arithmetic values and arithmetic ops
        CmpExp.Builder cmpExpBuilder = CmpExp.newBuilder();
        cmpExpBuilder.addAllLeftFormulaRPN(arithmeticValOpsLeft);
        cmpExpBuilder.addAllRightFormulaRPN(arithmeticValOpsRight);
        try {
            cmpExpBuilder.setOp(CompareExpUtil.parseCmpOp(op));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // reverse Polish representation
        logicExpsBuilder.addExpRPN(LogicExps.ExpOrOp.newBuilder().setExp(cmpExpBuilder));
        return logicExpsBuilder;
    }


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public LogicExps.Builder visitCategoryExp(FilterParser.CategoryExpContext ctx) {
        String op = ctx.getChild(1).getText();
        if (ctx.getChildCount() >= 4) {
            op += " " + ctx.getChild(2).getText();
        }
        arithmeticValOpsLeft.clear();
        arithmeticValOpsRight.clear();
        arithmeticValOps = arithmeticValOpsLeft;

        visitChildren(ctx);

        // generate category expression from arithmetic values and arithmetic ops
        CmpExp.Builder cmpExpBuilder = CmpExp.newBuilder();
        cmpExpBuilder.addAllLeftFormulaRPN(arithmeticValOpsLeft);
        cmpExpBuilder.addAllRightFormulaRPN(arithmeticValOpsRight);
        try {
            cmpExpBuilder.setOp(CompareExpUtil.parseCmpOp(op));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // reverse Polish representation
        logicExpsBuilder.addExpRPN(LogicExps.ExpOrOp.newBuilder().setExp(cmpExpBuilder));
        return logicExpsBuilder;
    }
}
