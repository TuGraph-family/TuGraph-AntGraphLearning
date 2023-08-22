package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.filter.result.AbstractResult;
import com.alipay.alps.flatv3.filter.result.CommonResult;
import com.alipay.alps.flatv3.filter.result.RangeResult;
import com.alipay.alps.flatv3.index.HeteroDataset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * This class represents a random sampler that performs random sampling on an input IndexResult object.
 * The class extends the Sampler abstract class.
 * It uses a random number generator to select a subset of the IndexResult object based on the provided SampleCondition.
 */
public class RandomSampler extends AbstractSampler {
    public RandomSampler(SampleCondition sampleCondition, HeteroDataset neighborDataset) {
        super(sampleCondition, neighborDataset);
    }

    /**
     * Perform random sampling on an input IndexResult object.
     * If the sample size is smaller than a quarter of the candidate count, it selects elements randomly without replacement.
     * Otherwise, it performs Fisher-Yates shuffle to randomly select a subset of elements.
     *
     * @param indexResult An IndexResult object containing the data to be sampled
     * @return An ArrayList<Integer> object containing the indices of the selected elements
     */
    @Override
    public List<Integer> sample(AbstractResult indexResult) {
        int candidateCount = indexResult.getSize();
        int sampleCount = this.getSampleCondition().getLimit();
        // If the number of samples requested is less than 1/sampleCountToCandidateCountRatio of the input size,
        // simply select samples at random without replacement using a HashSet.
        List<Integer> sampled = sampleWithCount(candidateCount, sampleCount);
        List<Integer> sampledIndex = new ArrayList<>(sampled.size());
        if (indexResult instanceof CommonResult) {
            List<Integer> originIndices = indexResult.getIndices();
            for (int i = 0; i < sampled.size(); i++) {
                sampledIndex.add(originIndices.get(sampled.get(i)));
            }
        } else if (indexResult instanceof RangeResult) {
            for (int i = 0; i < sampled.size(); i++) {
                int idx = ((RangeResult) indexResult).getRangeIndex(sampled.get(i));
                sampledIndex.add(indexResult.getOriginIndex(idx));
            }
        } else {
            throw new RuntimeException("error AbstractResult");
        }
        return sampledIndex;
    }

    private List<Integer> sampleWithCount(int candidateCount, int sampleCount) {
        if (getSampleCondition().isReplacement()) {
            List<Integer> out = new ArrayList<>(sampleCount);
            for (int i = 0; i < sampleCount; i++) {
                out.add(getNextRandomInt(candidateCount));
            }
            return out;
        }
        if (sampleCount >= candidateCount) {
            List<Integer> out = new ArrayList<>(candidateCount);
            for (int i = 0; i < candidateCount; i++) {
                out.add(i);
            }
            return out;
        }
        List<Integer> out = new ArrayList<>(sampleCount);
        if (sampleCount <= candidateCount * sampleCountToCandidateCountRatio) {
            if (sampleCount < 64) {
                for (int i = 0; i < sampleCount; i++) {
                    int value = getNextRandomInt(candidateCount);
                    if (out.contains(value)) {
                        i--;
                    } else {
                        out.add(value);
                    }
                }
            } else {
                Set<Integer> selected = new HashSet<>();
                while (selected.size() < sampleCount) {
                    selected.add(getNextRandomInt(candidateCount));
                }
                out.addAll(selected);
            }
        } else {
            int[] seq = IntStream.range(0, candidateCount).toArray();
            for (int i = 0; i < sampleCount; i++) {
                int j = i + getNextRandomInt(candidateCount - i);
                int temp = seq[i];
                seq[i] = seq[j];
                seq[j] = temp;
            }
            for (int i = 0; i < sampleCount; i++) {
                out.add(seq[i]);
            }

        }
        return out;
    }
}

