package com.alipay.alps.flatv3.filter_exp;

import com.alipay.alps.flatv3.antlr4.FilterLexer;
import com.alipay.alps.flatv3.antlr4.FilterParser;
import com.antfin.ai.alps.graph.flat.sample.LogicExps;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterConditionParser {
    private static final Logger LOG = LoggerFactory.getLogger(FilterConditionParser.class);
    public LogicExps parseFilterCondition(String filterCond) {
        ANTLRInputStream input = new ANTLRInputStream(filterCond);
        FilterLexer lexer = new FilterLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FilterParser parser = new FilterParser(tokens);
        FilterConditionVisitor visitor = new FilterConditionVisitor();
        LogicExps logicExps = visitor.visit(parser.start()).build();
        LOG.info("filterCond: {} logicExps: {}", filterCond, logicExps);
        return logicExps;
    }
}
