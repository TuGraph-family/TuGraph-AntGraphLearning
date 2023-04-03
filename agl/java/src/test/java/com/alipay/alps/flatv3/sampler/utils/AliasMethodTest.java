package com.alipay.alps.flatv3.sampler.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

import org.junit.Test;

public class AliasMethodTest {
    @Test
    public void testNextRandom() {
        List<Float> probabilities = new ArrayList<Float>(Arrays.asList(0.6F, 0.8F, 0.2F, 0.4F));

        AliasMethod aliasMethod = new AliasMethod(probabilities, new Random());
        int[] counts = new int[probabilities.size()];

        // Generate 1000000 samples and count occurrences of each bucket
        for (int i = 0; i < 10000000; i++) {
            int bucket = aliasMethod.nextSample();
            counts[bucket]++;
        }
        // Check that the proportions of occurrences are roughly equal to the given probabilities
        float sum = 0;
        for (int i = 0; i < probabilities.size(); i++) {
            sum += probabilities.get(i);
        }
        for (int i = 0; i < probabilities.size(); i++) {
            float proportion = (float) counts[i] / 10000000;
            assertEquals(probabilities.get(i) / sum, proportion, 0.01);
        }
    }
}

