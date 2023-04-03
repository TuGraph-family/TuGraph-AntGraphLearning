package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.IndexResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
/**
 Sampler is an abstract class that provides a template for sampling data from an indexResult
 */
public abstract class Sampler {
    // SampleCondition object that stores the properties used for sampling
    private SampleCondition sampleCondition = null;
    // IndexResult object used when sampling data
    private Map<String, BaseIndex> indexes = null;
    // Random object used to generate a random seed
    private Random rand = new Random();

    // If it is larger than this threshold, we will delete the sampled node from the candidate set physically
    // If sample ratio is less than this threshold, 
    // we will maintain a distinct set of samples to perform sampling with replacement without deleting the sampled node physically.
    protected final Float sampleCountToCandidateCountRatio = 0.25f;

    public Sampler(SampleCondition sampleCondition, BaseIndex index) {
        this.sampleCondition = sampleCondition;
        if (indexes == null) {
            indexes = new HashMap<>();
        }
        this.indexes.put(index.getIndexColumn(), index);
    }
    /**
     * Constructor for Sampler class with two parameters
     * @param sampleCondition SampleCondition object for storing the properties used for sampling
     * @param indexes Index object used when sampling data
     */
    public Sampler(SampleCondition sampleCondition, Map<String, BaseIndex> indexes) {
        this.sampleCondition = sampleCondition;
        this.indexes = indexes;
    }

    /**
     * Abstract method used to sample data from an indexResult
     * @param indexResult IndexResult object used when sampling data
     * @return ArrayList of integers containing the sampled data
     */
    protected abstract List<Integer> sampleImpl(IndexResult indexResult);

    public List<Integer> sample(IndexResult indexResult) {
        List<Integer> neighborIndices = sampleImpl(indexResult);
        if (indexResult instanceof CommonIndexResult) {
            return neighborIndices;
        }
        Integer[] originIndex = indexResult.getOriginIndex();
        if (originIndex != null) {
            List<Integer> originIndices = new ArrayList<>();
            for (int idx : neighborIndices) {
                originIndices.add(originIndex[idx]);
            }
            return originIndices;
        }
        return neighborIndices;
    }

    // Getter for SampleCondition object
    public SampleCondition getSampleCondition() {
        return sampleCondition;
    }

    // Getter for IndexResult object
    public Map<String, BaseIndex> getIndexes() {
        return indexes;
    }

    // Getter for random object
    public Random getRand() {
        return rand;
    }

    public float getNextFloat() {
        return rand.nextFloat();
    }
    public int getNextInt(int bound) {
        return rand.nextInt(bound);
    }
}
