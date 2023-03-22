package com.alipay.alps.flatv3.sampler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

class AliasMethodTest {
    @Test
    void testNextRandom() {
        List<Float> probabilities = new ArrayList<Float>(Arrays.asList(0.3F, 0.4F, 0.1F, 0.2F));

        AliasMethod aliasMethod = new AliasMethod(probabilities);
        int[] counts = new int[probabilities.size()];

        // Generate 1000000 samples and count occurrences of each bucket
        for (int i = 0; i < 1000000; i++) {
            int bucket = aliasMethod.nextRandom();
            counts[bucket]++;
        }

        // Check that the proportions of occurrences are roughly equal to the given probabilities
        for (int i = 0; i < probabilities.size(); i++) {
            double proportion = (double) counts[i] / 1000000;
            assertEquals(probabilities.get(i), proportion, 0.01);
        }
    }
}

