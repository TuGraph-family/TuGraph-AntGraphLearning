package com.alipay.alps.flatv3.sampler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SampleConditionTest {

    @Test
    public void testConstructor() {
        String sampleMeta = "weighted_sample(by=index.$1.weight, limit=5, reverse=true, replacement=False)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        assertEquals("weighted_sample", sampleCondition.getMethod());
        assertEquals("index.$1.weight", sampleCondition.getKey());
        assertEquals(5, sampleCondition.getLimit());
        assertFalse(sampleCondition.isReplacement());
        assertTrue(sampleCondition.isReverse());
    }

    @Test
    public void testToString() {
        SampleCondition sampleCondition = new SampleCondition("weighted_sample", "index.$1.weight", 5, false, true, 34);
        String expected = "SampleCondition{method='weighted_sample', key='index.$1.weight', limit=5, replacement=false, reverse=true}";
        assertEquals(expected, sampleCondition.toString());
    }
}