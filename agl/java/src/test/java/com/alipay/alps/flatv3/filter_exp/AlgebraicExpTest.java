package com.alipay.alps.flatv3.filter_exp;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AlgebraicExpTest {
    @Test
    public void testEval() {
        AlgebraicExp algebraicExp = new AlgebraicExp("INDEX + 5", ">", "SEED + 10");
        boolean result = algebraicExp.eval(2.0, 5.0);
        Assert.assertTrue(result);
    }

    @Test
    public void testEval2() {
        AlgebraicExp algebraicExp = new AlgebraicExp("INDEX + 10", ">=", "SEED + 5");
        boolean result = algebraicExp.eval(1.0, 5.0);
        Assert.assertTrue(result);
    }
}

