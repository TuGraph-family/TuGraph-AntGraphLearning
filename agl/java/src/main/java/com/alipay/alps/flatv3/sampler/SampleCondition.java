package com.alipay.alps.flatv3.sampler;

import java.util.StringTokenizer;

/**
 * This class represents a condition for sampling data, which is constructed based on a string representation.
 * It contains method name, key to sample by, sample limit, replacement flag, and reverse flag.
 */
public class SampleCondition {
    public String method;
    public String key;
    public int limit;
    public boolean replacement = false;
    public boolean reverse = false;

    /**
     * Constructs a SampleCondition object.
     *
     * @param method      The name of the sampling method.
     * @param key         The key to sample by.
     * @param limit       The sample limit.
     * @param replacement Whether or not replacement is allowed.
     * @param reverse     Whether or not to reverse the sample.
     */
    public SampleCondition(String method, String key, int limit, boolean replacement, boolean reverse) {
        this.method = method;
        this.key = key;
        this.limit = limit;
        this.replacement = replacement;
        this.reverse = reverse;
    }

    /**
     * Constructs a SampleCondition object from a string representation.
     * @param sampleMeta The string representation of the SampleCondition object.
     */
    public SampleCondition(String sampleMeta) {
        // Remove all whitespace characters from the string representation.
        sampleMeta = sampleMeta.replaceAll("\\s", "");
        String delimitor = "(),";
        StringTokenizer x = new StringTokenizer(sampleMeta, delimitor, true);

        // Parse the string representation and set the fields of the SampleCondition object.
        while (x.hasMoreTokens()) {
            String p = x.nextToken();
            if (!delimitor.contains(p)) {
                int sepPos = p.indexOf('=');
                if (sepPos == -1) {
                    this.method = p;
                } else {
                    String a1 = p.substring(0, sepPos);
                    String a2 = p.substring(sepPos + 1);
                    if (a1.compareToIgnoreCase("by") == 0) {
                        this.key = a2;
                    } else if (a1.compareToIgnoreCase("limit") == 0) {
                        this.limit = Integer.valueOf(a2);
                    } else if (a1.compareToIgnoreCase("replace") == 0) {
                        this.replacement = Boolean.valueOf(a2);
                    } else if (a1.compareToIgnoreCase("reverse") == 0) {
                        this.reverse = Boolean.valueOf(a2);
                    }
                }
            }
        }
    }

    /**
     * Returns a string representation of the SampleCondition object.
     * @return A string representation of the SampleCondition object.
     */
    @Override
    public String toString() {
        return "SampleCondition{" +
                "method='" + method + '\'' +
                ", key='" + key + '\'' +
                ", limit=" + limit +
                ", replacement=" + replacement +
                ", reverse=" + reverse +
                '}';
    }

    public static void main(String[] args) throws Exception {
        String sampleMeta = "weighted_sample(by=index.$1.weight, limit=5, reverse=true, replace=False)";
        SampleCondition sampleCondition = new SampleCondition(sampleMeta);
        System.out.println(sampleCondition);
    }
}
