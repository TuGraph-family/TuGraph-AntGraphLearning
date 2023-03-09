package com.alipay.alps.flatv3.filter_exp;

import java.util.ArrayList;

public class FilterVisitor extends TestJavaBaseVisitor<FilterConditionSingleton> {

    @Override public FilterConditionSingleton visitAndExp(TestJavaParser.AndExpContext ctx) {
        System.out.println("----AndExp:" + ctx.getText() + " child:" + ctx.getChildCount() + " c1:" + ctx.getChild(1).getText());
        FilterConditionSingleton filterCondition = FilterConditionSingleton.getInstance();
        ArrayList<ArrayList<Expression>> unionFilters =  filterCondition.getUnionJoinFilters();
        if (!(ctx.parent instanceof TestJavaParser.AndExpContext)) {
            ArrayList<Expression> filter = new ArrayList<>();
            unionFilters.add(filter);
        }
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public FilterConditionSingleton visitOrExp(TestJavaParser.OrExpContext ctx) {
        System.out.println("----ORExp:" + ctx.getText() + " child:" + ctx.getChildCount() + " c1:" + ctx.getChild(1).getText());
        return visitChildren(ctx);
    }


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public FilterConditionSingleton visitLtExp(TestJavaParser.LtExpContext ctx) {
        FilterConditionSingleton filterCondition = FilterConditionSingleton.getInstance();
        System.out.println("----LtExp:" + ctx.getText() + " OR|AND:" + (ctx.parent instanceof TestJavaParser.OrExpContext));
        String leftExp = ctx.getChild(0).getText();
        String op = ctx.getChild(1).getText();
        String rightExp = ctx.getChild(2).getText();
        AlgebraicExp algebraicExp = new AlgebraicExp(leftExp, op, rightExp);
        ArrayList<ArrayList<Expression>> unionFilters =  filterCondition.getUnionJoinFilters();
        if (ctx.parent instanceof TestJavaParser.OrExpContext) {
            ArrayList<Expression> filter = new ArrayList<>();
            filter.add(algebraicExp);
            unionFilters.add(filter);
        } else {
            if (!(ctx.parent instanceof TestJavaParser.AndExpContext)) {
                unionFilters.add(new ArrayList<>());
            }
            unionFilters.get(unionFilters.size() - 1).add(algebraicExp);
        }
        return filterCondition;
    }


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public FilterConditionSingleton visitEqExp(TestJavaParser.EqExpContext ctx) {
        System.out.println("----EqExp:" + ctx.getText() + " child:" + ctx.getChildCount() + " left:" + ctx.getChild(0).getText() + " right:" + ctx.getChild(2).getText());
        FilterConditionSingleton filterCondition = FilterConditionSingleton.getInstance();
        String leftExp = ctx.getChild(0).getText();
        String op = ctx.getChild(1).getText();
        String rightExp = ctx.getChild(2).getText();
        TypeExp typeExp = new TypeExp(leftExp, op, rightExp);
        ArrayList<ArrayList<Expression>> unionFilters =  filterCondition.getUnionJoinFilters();
        if (ctx.parent instanceof TestJavaParser.OrExpContext) {
            ArrayList<Expression> filter = new ArrayList<>();
            filter.add(typeExp);
            unionFilters.add(filter);
        } else {
            if (!(ctx.parent instanceof TestJavaParser.AndExpContext)) {
                unionFilters.add(new ArrayList<>());
            }
            unionFilters.get(unionFilters.size() - 1).add(typeExp);
        }
        return filterCondition;
    }
}
