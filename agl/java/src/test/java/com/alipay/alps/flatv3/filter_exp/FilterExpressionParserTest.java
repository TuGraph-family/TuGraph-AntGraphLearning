package com.alipay.alps.flatv3.filter_exp;

import com.antfin.agl.proto.sampler.CmpOp;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.LogicOp;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class FilterExpressionParserTest {
    @Test
    public void testParseFilterCond() {
        String filterCond = "INDEX.TIME - SEED.1 >= 0.5 AND INDEX.TIME <= SEED.1 + 11";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
        Assert.assertEquals(expOps.size(), 3);
        Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
        Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.LE);
        Assert.assertEquals(expOps.get(2), LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("AND")).build());
    }

    @Test
    public void testParseFilterCond2() {
        String filterCond = "index.time - seed.1 >= 0.5 or index.type not in (node, item)";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
        Assert.assertEquals(expOps.size(), 3);
        Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
        Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.NOT_IN);
        Assert.assertEquals(expOps.get(2), LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("OR")).build());
    }

    @Test
    public void testParseFilterCond3() {
        String filterCond = "index.time - seed.1 >= 0.5 or INDEX.TIME <= SEED.1 + 11 and index.type not in (node, item)";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
        Assert.assertEquals(expOps.size(), 5);
        Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
        Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.LE);
        Assert.assertEquals(expOps.get(2).getExp().getOp(), CmpOp.NOT_IN);
        Assert.assertEquals(expOps.get(3), LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("AND")).build());
        Assert.assertEquals(expOps.get(4), LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("OR")).build());
    }

    @Test
    public void testParseFilterCond4() {
        String filterCond = "index.time - (seed.1 - seed.2 * 4.3) / index.type >= 0.5 and INDEX.1 - 10 < SEED + 100 or SEED in (item, user)";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        List<LogicExps.ExpOrOp> expOps = logicExps.getExpRPNList();
        Assert.assertEquals(expOps.size(), 5);
        Assert.assertEquals(expOps.get(0).getExp().getOp(), CmpOp.GE);
        Assert.assertEquals(expOps.get(1).getExp().getOp(), CmpOp.LT);
        Assert.assertEquals(expOps.get(2), LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("AND")).build());
        Assert.assertEquals(expOps.get(3).getExp().getOp(), CmpOp.IN);
        Assert.assertEquals(expOps.get(4), LogicExps.ExpOrOp.newBuilder().setOp(LogicOp.valueOf("OR")).build());
    }
}