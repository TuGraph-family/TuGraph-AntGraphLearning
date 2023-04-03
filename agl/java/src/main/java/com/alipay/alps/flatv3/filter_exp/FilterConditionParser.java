package com.alipay.alps.flatv3.filter_exp;

import com.alipay.alps.flatv3.antlr4.FilterLexer;
import com.alipay.alps.flatv3.antlr4.FilterParser;
import com.antfin.agl.proto.sampler.LogicExps;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class FilterConditionParser {
    private static final Logger LOG = LoggerFactory.getLogger(FilterConditionParser.class);
    public LogicExps parseFilterCondition(String filterCond) {
        LogicExps logicExps = null;
        try {
            // if filterCond is empty string, return an empty LogicExps
            if (filterCond == null || filterCond.isEmpty()) {
                return LogicExps.newBuilder().build();
            }
            ANTLRInputStream input = new ANTLRInputStream(filterCond);
            FilterLexer lexer = new FilterLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            FilterParser parser = new FilterParser(tokens);
            FilterConditionVisitor visitor = new FilterConditionVisitor();
            logicExps = visitor.visit(parser.start()).build();
        } catch (Throwable e) {
            String errorMsg = "filter syntax error. filter input:"+ filterCond +"\n error:" + Arrays.toString(e.getStackTrace());
            LOG.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        return logicExps;
    }
}
