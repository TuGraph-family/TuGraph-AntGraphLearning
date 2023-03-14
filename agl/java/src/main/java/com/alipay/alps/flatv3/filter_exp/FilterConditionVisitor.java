package com.alipay.alps.flatv3.filter_exp;

import com.alipay.alps.flatv3.antlr4.FilterBaseVisitor;
import com.alipay.alps.flatv3.antlr4.FilterParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.antfin.ai.alps.graph.aglstore.loader.CmpExp;
import com.antfin.ai.alps.graph.aglstore.loader.Element;
import com.antfin.ai.alps.graph.aglstore.loader.LogicExps;
import com.antfin.ai.alps.graph.aglstore.loader.LogicOp;
import com.antfin.ai.alps.graph.aglstore.loader.VariableSource;

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
        logicExpsBuilder.addExps(LogicExps.ExpOp.newBuilder().setOp(LogicOp.valueOf(op.toUpperCase())));
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
        logicExpsBuilder.addExps(LogicExps.ExpOp.newBuilder().setOp(LogicOp.valueOf(op.toUpperCase())));
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
            Element.Var.Builder elementVarBuilder = Element.newBuilder().getVBuilder().setSource(VariableSource.valueOf(arrs[0].toUpperCase()));
            elementVarBuilder.setName(arrs.length <= 1 ? "" : arrs[1]);
            arithmeticValOps.add(Element.newBuilder().setV(elementVarBuilder).build());
        } else {
            Element.Num.Builder elementNumBuilder = Element.newBuilder().getNBuilder().setS(exp);
            arithmeticValOps.add(Element.newBuilder().setN(elementNumBuilder).build());
        }
        if (ctx.parent instanceof FilterParser.CompareExpContext || ctx.parent instanceof FilterParser.CategoryExpContext) {
            arithmeticValOps = arithmeticValOpsRight;
        }
        return logicExpsBuilder;
    }

    @Override public LogicExps.Builder visitLiteralExp(FilterParser.LiteralExpContext ctx) {
        String exp = ctx.getChild(0).getText();
        visitChildren(ctx);

        Element.Num.Builder elementNumBuilder = Element.newBuilder().getNBuilder();
        if (exp.matches("\\d+(\\.\\d+)?")) {
            elementNumBuilder.setF(Float.parseFloat(exp));
        } else {
            elementNumBuilder.setF(Integer.parseInt(exp)); // setInt?
        }
        arithmeticValOps.add(Element.newBuilder().setN(elementNumBuilder).build());
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
        cmpExpBuilder.addAllLeft(arithmeticValOpsLeft);
        cmpExpBuilder.addAllRight(arithmeticValOpsRight);
        try {
            cmpExpBuilder.setOp(CompareExpUtil.parseCmpOp(op));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // reverse Polish representation
        logicExpsBuilder.addExps(LogicExps.ExpOp.newBuilder().setExp(cmpExpBuilder));
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
        cmpExpBuilder.addAllLeft(arithmeticValOpsLeft);
        cmpExpBuilder.addAllRight(arithmeticValOpsRight);
        try {
            cmpExpBuilder.setOp(CompareExpUtil.parseCmpOp(op));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // reverse Polish representation
        logicExpsBuilder.addExps(LogicExps.ExpOp.newBuilder().setExp(cmpExpBuilder));
        return logicExpsBuilder;
    }
}
