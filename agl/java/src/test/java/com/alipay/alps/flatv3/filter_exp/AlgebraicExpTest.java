package com.alipay.alps.flatv3.filter_exp;

import org.junit.Assert;
import org.junit.Test;

public class AlgebraicExpTest {
    @Test
    public void testEval() {
        AlgebraicExp algebraicExp = new AlgebraicExp("SEED.1 + 10", "<", "INDEX.time + 5");
        boolean result = algebraicExp.eval(2.0, 5.0);
        Assert.assertFalse(result);
    }

    @Test
    public void testEval2() {
        AlgebraicExp algebraicExp = new AlgebraicExp("INDEX.time + 10", ">=", "seed.1 + 5");
        boolean result = algebraicExp.eval(1.0, 5.0);
        Assert.assertTrue(result);
    }

    @Test
    public void testEval3() {
        AlgebraicExp algebraicExp = new AlgebraicExp("SEED.1 - INDEX.time", ">=", "3");
        boolean result = algebraicExp.eval(1.0, 5.0);
        Assert.assertTrue(result);
    }

    @Test
    public void testEval4() {
        AlgebraicExp algebraicExp = new AlgebraicExp("INDEX.time - seed.1", ">=", "5");
        boolean result = algebraicExp.eval(1.0, 5.0);
        Assert.assertFalse(result);
    }
}

