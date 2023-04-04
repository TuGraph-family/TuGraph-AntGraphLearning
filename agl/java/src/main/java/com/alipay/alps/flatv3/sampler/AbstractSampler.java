package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/**
 Sampler is an abstract class that provides a template for sampling data from an AbstractIndexResult
 */
public abstract class AbstractSampler {
    // SampleCondition object that stores the properties used for sampling
    private final SampleCondition sampleCondition;
    // neighborDataset object that stores the data used for sampling
    private final NeighborDataset neighborDataset;
    // Random object used to generate a random seed
    private final Random rand = new Random();

    // If it is larger than this threshold, we will delete the sampled node from the candidate set physically
    // If sample ratio is less than this threshold, 
    // we will maintain a distinct set of samples to perform sampling with replacement without deleting the sampled node physically.
    protected final Float sampleCountToCandidateCountRatio = 0.25f;

    /**
     * Constructor for Sampler class with two parameters
     * @param sampleCondition SampleCondition object for storing the properties used for sampling
     * @param neighborDataset NeighborDataset object for storing the data used for sampling
     */
    public AbstractSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        this.sampleCondition = sampleCondition;
        this.neighborDataset = neighborDataset;
    }

    /**
     * Abstract method used to sample data from an indexResult
     * @param indexResult IndexResult object used when sampling data
     * @return ArrayList of integers containing the sampled data
     */
    protected abstract List<Integer> sampleImpl(AbstractIndexResult indexResult);

    public List<Integer> sample(AbstractIndexResult indexResult) {
        List<Integer> neighborIndices = sampleImpl(indexResult);
        if (indexResult instanceof CommonIndexResult) {
            return neighborIndices;
        }
        Integer[] originIndex = indexResult.getOriginIndice();
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

    // Getter for random object
    public Random getRand() {
        return rand;
    }

    // Getter for NeighborDataset object
    public NeighborDataset getNeighborDataset() {
        return neighborDataset;
    }

    public int getNextRandomInt(int bound) {
        return rand.nextInt(bound);
    }
}
