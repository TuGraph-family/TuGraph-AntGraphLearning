/**
 * Copyright 2023 AntGroup CO., Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.alipay.alps.flatv3.filter.parser;

import com.alipay.alps.flatv3.antlr4.FilterLexer;
import com.alipay.alps.flatv3.antlr4.FilterParser;
import com.antfin.agl.proto.sampler.LogicExps;
import java.util.Arrays;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

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
      throw new RuntimeException(
          "filter syntax error. filter input:" + filterCond + "\n error:" + Arrays
              .toString(e.getStackTrace()) + "\n err:" + e.getMessage());
    }
    return logicExps;
  }
}
