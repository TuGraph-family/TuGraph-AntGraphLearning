package com.alipay.alps.flatv3.filter_exp;

import org.junit.Assert;
import org.junit.Test;

public class CategoryExpTest {
    @Test
    public void testEval() {
        CategoryExp typeExp = new CategoryExp("SEED.1", "IN", "(item, user)");
        boolean result = typeExp.eval(null, "item");
        Assert.assertTrue(result);
    }

    @Test
    public void testEval2() {
        CategoryExp typeExp = new CategoryExp("INDEX.type", "NOT IN", "(item, user)");
        boolean result = typeExp.eval("user", "item");
        Assert.assertFalse(result);
    }

    @Test
    public void testEval3() {
        CategoryExp typeExp = new CategoryExp("SEED.1", "=", "item");
        boolean result = typeExp.eval(null, "item");
        Assert.assertTrue(result);
    }

    @Test
    public void testEval4() {
        CategoryExp typeExp = new CategoryExp("SEED.1", "==", "item");
        boolean result = typeExp.eval(null, "user");
        Assert.assertFalse(result);
    }
    @Test
    public void testEval5() {
        CategoryExp typeExp = new CategoryExp("SEED.1", "!=", "item");
        boolean result = typeExp.eval(null, "user");
        Assert.assertTrue(result);
    }
}
