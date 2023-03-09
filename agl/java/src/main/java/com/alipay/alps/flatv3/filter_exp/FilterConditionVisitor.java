package com.alipay.alps.flatv3.filter_exp;

import com.alipay.alps.flatv3.antlr4.FilterBaseVisitor;
import com.alipay.alps.flatv3.antlr4.FilterParser;

import java.util.ArrayList;

public class FilterConditionVisitor extends FilterBaseVisitor<FilterConditionSingleton> {

    @Override public FilterConditionSingleton visitAndExp(FilterParser.AndExpContext ctx) {
        FilterConditionSingleton filterCondition = FilterConditionSingleton.getInstance();
        ArrayList<ArrayList<Expression>> unionFilters =  filterCondition.getUnionJoinFilters();
        if (!(ctx.parent instanceof FilterParser.AndExpContext)) {
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
    @Override public FilterConditionSingleton visitOrExp(FilterParser.OrExpContext ctx) {
        return visitChildren(ctx);
    }


    /**
     * {@inheritDoc}
     *
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override public FilterConditionSingleton visitCompareExp(FilterParser.CompareExpContext ctx) {
        FilterConditionSingleton filterCondition = FilterConditionSingleton.getInstance();
        String leftExp = ctx.getChild(0).getText();
        String op = ctx.getChild(1).getText();
        String rightExp = ctx.getChild(2).getText();
        AlgebraicExp algebraicExp = new AlgebraicExp(leftExp, op, rightExp);
        ArrayList<ArrayList<Expression>> unionFilters =  filterCondition.getUnionJoinFilters();
        if (ctx.parent instanceof FilterParser.OrExpContext) {
            ArrayList<Expression> filter = new ArrayList<>();
            filter.add(algebraicExp);
            unionFilters.add(filter);
        } else {
            if (!(ctx.parent instanceof FilterParser.AndExpContext)) {
                unionFilters.add(new ArrayList<Expression>());
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
    @Override public FilterConditionSingleton visitCategoryExp(FilterParser.CategoryExpContext ctx) {
        FilterConditionSingleton filterCondition = FilterConditionSingleton.getInstance();
        String leftExp = ctx.getChild(0).getText();
        String op = ctx.getChild(1).getText();
        String rightExp = ctx.getChild(2).getText();
        CategoryExp typeExp = new CategoryExp(leftExp, op, rightExp);
        ArrayList<ArrayList<Expression>> unionFilters =  filterCondition.getUnionJoinFilters();
        if (ctx.parent instanceof FilterParser.OrExpContext) {
            ArrayList<Expression> filter = new ArrayList<>();
            filter.add(typeExp);
            unionFilters.add(filter);
        } else {
            if (!(ctx.parent instanceof FilterParser.AndExpContext)) {
                unionFilters.add(new ArrayList<Expression>());
            }
            unionFilters.get(unionFilters.size() - 1).add(typeExp);
        }
        return filterCondition;
    }
}
