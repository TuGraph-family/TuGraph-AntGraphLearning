package com.alipay.alps.flatv3.sampler;

import java.util.List;
import java.util.Random;

/**
 A data structure that implements the Alias method for generating random samples from a discrete probability distribution.
 The algorithm creates two tables, one with probabilities and one with corresponding indices for each element in the distribution.
 The alias table is created such that the table has entries with probabilities equal to or greater than the average of the distribution, and each entry is paired with another entry with a probability less than the average.
 The data structure is then used to generate random samples from the probability distribution using the nextRandom() method.
 */
public class AliasMethod {
    // Pre-computed probability and alias tables
    public float[] probabilityTable;
    private int[] aliasTable;

    public AliasMethod(List<Float> weights) {
        int n = weights.size();
        probabilityTable = new float[n];
        aliasTable = new int[n];
        Float sum = 0F;
        for (Float w : weights) {
            sum += w;
        }
        for (int i = 0; i < weights.size(); i++) {
            probabilityTable[i] = weights.get(i)/sum;
        }
        initAlias();
    }

    /**
     * Initializes the probability and alias tables for the given probability distribution.
     * 1, All buckets contain 1/n liquids
     * 2, each bucket contains at most 2 different kinds of liquids
     */
    public void initAlias() {
        int n = probabilityTable.length;
        float average = 1.0F / n;
        System.out.println("------initAlias n:" + n + " average:" + average);
        // Set up the probability and alias tables
        int[] small = new int[n];
        int[] large = new int[n];

        int smallSize = 0;
        int largeSize = 0;
        for (int i = 0; i < n; i++) {
            if (probabilityTable[i] >= average) {
                large[largeSize++] = i;
            } else {
                small[smallSize++] = i;
            }
        }

        while (smallSize > 0 && largeSize > 0) {
            int l = small[--smallSize];
            int g = large[--largeSize];
            float gLeft = probabilityTable[l] + probabilityTable[g] - average;
            probabilityTable[l] = average;
            aliasTable[l] = g;

            probabilityTable[g] = gLeft;
            if (gLeft >= average) {
                large[largeSize++] = g;
            } else {
                small[smallSize++] = g;
            }
        }

        System.out.println("-----initAlias left largeSize:" + largeSize);
        while (largeSize > 0) {
            System.out.println("-----initAlias left p:" + probabilityTable[large[largeSize-1]]);
            probabilityTable[large[--largeSize]] = average;
        }
        System.out.println("-----initAlias left smallSize:" + smallSize);
        while (smallSize > 0) {
            System.out.println("-----initAlias left p:" + probabilityTable[small[smallSize-1]]);
            probabilityTable[small[--smallSize]] = average;
        }
    }

    /**
     * Generates a random sample from the probability distribution using the Alias method.
     * @return An integer representing the index of the sampled element.
     */
    public int nextRandom() {
        int n = probabilityTable.length;
        Random rand = new Random();
        int i = rand.nextInt(n);

        double x = rand.nextDouble();
        if (x < probabilityTable[i]) {
            return i;
        } else {
            return aliasTable[i];
        }
    }
}
