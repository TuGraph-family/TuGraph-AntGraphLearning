package com.alipay.alps.flatv3.sampler;

import org.junit.Test;
import static org.junit.Assert.*;

public class SampleConditionTest {

    @Test
    public void testConstructor() {
        String sampleMeta = "weighted_sample(by=index.$1.weight, limit=5, reverse=true, replace=False)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        assertEquals("weighted_sample", sampleCondition.method);
        assertEquals("index.$1.weight", sampleCondition.key);
        assertEquals(5, sampleCondition.limit);
        assertFalse(sampleCondition.replacement);
        assertTrue(sampleCondition.reverse);
    }

    @Test
    public void testToString() {
        SampleCondition sampleCondition = new SampleCondition("weighted_sample", "index.$1.weight", 5, false, true);
        String expected = "SampleCondition{method='weighted_sample', key='index.$1.weight', limit=5, replacement=false, reverse=true}";
        assertEquals(expected, sampleCondition.toString());
    }
}