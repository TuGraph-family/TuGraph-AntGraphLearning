package com.alipay.alps.flatv3.filter.parser;

import com.alipay.alps.flatv3.antlr4.FilterLexer;
import com.alipay.alps.flatv3.antlr4.FilterParser;
import com.antfin.agl.proto.sampler.LogicExps;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Arrays;

public class FilterConditionParser {
    public static LogicExps parseFilterCondition(String filterCond) {
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
            throw new RuntimeException("filter syntax error. filter input:" + filterCond + "\n error:" + Arrays.toString(e.getStackTrace()) + "\n err:" + e.getMessage());
        }
        return logicExps;
    }
}