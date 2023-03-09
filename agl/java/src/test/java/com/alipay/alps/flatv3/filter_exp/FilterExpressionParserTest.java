package com.alipay.alps.flatv3.filter_exp;

import com.alipay.alps.flatv3.antlr4.FilterLexer;
import com.alipay.alps.flatv3.antlr4.FilterParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class FilterExpressionParserTest {
    @Test
    public void testParseFilterCond() {
        FilterConditionSingleton.reset();
        String filterCond = "INDEX.TIME - SEED.1 >= 0.5 AND INDEX.TIME <= SEED.1 + 11";
        ANTLRInputStream input = new ANTLRInputStream(filterCond);
        FilterLexer lexer = new FilterLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FilterParser parser = new FilterParser(tokens);
        FilterConditionVisitor visitor = new FilterConditionVisitor();
        visitor.visit(parser.start());
        ArrayList<ArrayList<Expression>> unionJoinFilters = FilterConditionSingleton.getInstance().getUnionJoinFilters();
        Assert.assertEquals(unionJoinFilters.size(), 1);
        Assert.assertEquals(unionJoinFilters.get(0).size(), 2);
        Assert.assertTrue(unionJoinFilters.get(0).get(0) instanceof AlgebraicExp);
        Assert.assertTrue(unionJoinFilters.get(0).get(1) instanceof AlgebraicExp);
    }

    @Test
    public void testParseFilterCond2() {
        FilterConditionSingleton.reset();
        String filterCond = "index.time - seed.1 >= 0.5 or index.type not in (node, item)";
        ANTLRInputStream input = new ANTLRInputStream(filterCond);
        FilterLexer lexer = new FilterLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FilterParser parser = new FilterParser(tokens);
        FilterConditionVisitor visitor = new FilterConditionVisitor();
        visitor.visit(parser.start());
        ArrayList<ArrayList<Expression>> unionJoinFilters = FilterConditionSingleton.getInstance().getUnionJoinFilters();
        Assert.assertEquals(unionJoinFilters.size(), 2);
        Assert.assertEquals(unionJoinFilters.get(1).size(), 1);
        Assert.assertTrue(unionJoinFilters.get(0).get(0) instanceof AlgebraicExp);
        Assert.assertTrue(unionJoinFilters.get(1).get(0) instanceof CategoryExp);
    }

    @Test
    public void testParseFilterCond3() {
        FilterConditionSingleton.reset();
        String filterCond = "index.time - seed.1 >= 0.5 or INDEX.TIME <= SEED.1 + 11 and index.type not in (node, item)";
        ANTLRInputStream input = new ANTLRInputStream(filterCond);
        FilterLexer lexer = new FilterLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FilterParser parser = new FilterParser(tokens);
        FilterConditionVisitor visitor = new FilterConditionVisitor();
        visitor.visit(parser.start());
        ArrayList<ArrayList<Expression>> unionJoinFilters = FilterConditionSingleton.getInstance().getUnionJoinFilters();
        Assert.assertEquals(unionJoinFilters.size(), 2);
        Assert.assertEquals(unionJoinFilters.get(1).size(), 2);
        Assert.assertTrue(unionJoinFilters.get(0).get(0) instanceof AlgebraicExp);
        Assert.assertTrue(unionJoinFilters.get(1).get(0) instanceof AlgebraicExp);
        Assert.assertTrue(unionJoinFilters.get(1).get(1) instanceof CategoryExp);
    }
}
