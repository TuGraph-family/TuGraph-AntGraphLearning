package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.NeighborDataset;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * TopKSampler is a class extending Sampler class.
 * It is used to get the top K elements from a given set of data that meets a given condition.
 * It has a PriorityQueue to store the top K elements and a comparator.
 * The sample() method is overridden to find the top K elements from the given data.
 * The isReverse attribute is used to determine the order of the elements in the PriorityQueue.
 */
public class TopKSampler<T extends Comparable<T>> extends AbstractSampler {
    private List<T> weights = null;
    private PriorityQueue<Integer> priorityQueue = null;
    private Comparator<Integer> comparator = null;
    private ArrayList<Integer> cachedIndex = null;

    public TopKSampler(SampleCondition sampleCondition, NeighborDataset neighborDataset) {
        super(sampleCondition, neighborDataset);
    }

    private void setupPriorityQueue(SampleCondition sampleCondition) {
        int isReverse = sampleCondition.isReverse() ? -1 : 1;
        comparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return weights.get(o1).compareTo(weights.get(o2)) * isReverse;
            }
        };
        priorityQueue = new PriorityQueue<>(sampleCondition.getLimit(), comparator);
    }

    @Override
    public List<Integer> sample(AbstractIndexResult indexResult) {
        int sampleCount = this.getSampleCondition().getLimit();
        ArrayList<Integer> sampledIndex = new ArrayList<>();
        String originIndexColumn = indexResult.getIndex().getIndexColumn();
        if (indexResult instanceof RangeIndexResult && originIndexColumn != null && originIndexColumn.compareTo(getSampleCondition().getKey()) == 0) {
            // reuse sorted neighbors in indexing stage
            if (indexResult instanceof RangeIndexResult) {
                List<Range> sortedIntervals = ((RangeIndexResult) indexResult).getRangeList();
                if (getSampleCondition().isReverse()) {
                    for (int i = sortedIntervals.size() - 1; i >= 0; i--) {
                        Range range = sortedIntervals.get(i);
                        for (int j = range.getHigh(); j >= range.getLow() && sampledIndex.size() < sampleCount; j--) {
                            sampledIndex.add(indexResult.getOriginIndex(j));
                        }
                    }
                } else {
                    for (Range range : sortedIntervals) {
                        for (int i = range.getLow(); i <= range.getHigh() && sampledIndex.size() < sampleCount; i++) {
                            sampledIndex.add(indexResult.getOriginIndex(i));
                        }
                    }
                }
            } else {
                List<Integer> sortedIndices = indexResult.getIndices();
                if (sortedIndices.size() <= sampleCount) {
                    sampledIndex.addAll(sortedIndices);
                    if (getSampleCondition().isReverse()) {
                        Collections.reverse(sampledIndex);
                    }
                }
                if (getSampleCondition().isReverse()) {
                    for (int i = sortedIndices.size() - 1; i >= 0 && sampledIndex.size() < sampleCount; i--) {
                        sampledIndex.add(sortedIndices.get(i));
                    }
                } else {
                    for (int i = 0; i < sortedIndices.size() && sampledIndex.size() < sampleCount; i++) {
                        sampledIndex.add(sortedIndices.get(i));
                    }
                }
            }
        } else {
            // sort neighbors by attribute at runtime
            // if there is no filter condition, the selected samples are always the same, we can cache the result
            if (originIndexColumn == null && cachedIndex != null) {
                return new ArrayList<>(cachedIndex);
            }
            if (weights == null) {
                weights = getNeighborDataset().getAttributeList(getSampleCondition().getKey());
            }
            if (priorityQueue == null) {
                setupPriorityQueue(getSampleCondition());
            }
            priorityQueue.clear();

            for (int idx : indexResult.getIndices()) {
                priorityQueue.add(idx);
            }
            for (int i = 0; i < sampleCount && !priorityQueue.isEmpty(); i++) {
                sampledIndex.add(priorityQueue.poll());
            }
            if (originIndexColumn == null && cachedIndex == null) {
                cachedIndex = new ArrayList<>(sampledIndex);
            }
        }
        return sampledIndex;
    }
}
