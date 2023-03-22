package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.IndexResult;
import com.alipay.alps.flatv3.index.Range;
import com.alipay.alps.flatv3.index.RangeIndexResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 TopKSampler is a class extending Sampler class.
 It is used to get the top K elements from a given set of data that meets a given condition.
 It has a PriorityQueue to store the top K elements and a Comparator to compare two pairs of keys and values.
 The sample() method is overridden to find the top K elements from the given data.
 The factor attribute is used to determine the order of the elements in the PriorityQueue.
 */
public class TopKSampler<T> extends Sampler {
    private PriorityQueue<Node> priorityQueue;
    private Comparator<Node> comparator;
    private ArrayList<Integer> cachedIndex = null;
    public static class Node<T extends Comparable<T>> {
        int index;
        T weight;

        public Node(int index, T weight) {
            this.index = index;
            this.weight = weight;
        }
    }

    public TopKSampler(SampleCondition sampleCondition, Map<String, BaseIndex> indexes) {
        super(sampleCondition, indexes);
        setup(sampleCondition);
    }

    public TopKSampler(SampleCondition sampleCondition, BaseIndex index) {
        super(sampleCondition, index);
        setup(sampleCondition);
    }

    private void setup(SampleCondition sampleCondition) {
        for (String indexName : this.getIndexes().keySet()) {
            BaseIndex index = this.getIndexes().get(indexName);
            if (sampleCondition.key.compareTo(index.getIndexColumn()) != 0) {
                int factor = sampleCondition.reverse ? -1 : 1;
                comparator = new Comparator<Node>() {
                    @Override
                    public int compare(Node o1, Node o2) {
                        return o1.weight.compareTo(o2.weight) * factor;
                    }
                };
                priorityQueue = new PriorityQueue<>(sampleCondition.limit, comparator);
            }
            break;
        }
    }
    @Override
    public List<Integer> sampleImpl(IndexResult indexResult) {
        int sampleCount = this.getSampleCondition().limit;
        int candidateCount = indexResult.getSize();
        if (candidateCount <= sampleCount) {
            return indexResult.getIndices();
        }

        ArrayList<Integer> sampledIndex = new ArrayList<>();
        if (indexResult.indexes.containsKey(getSampleCondition().key)) {
            List<Range> sortedIntervals = ((RangeIndexResult) indexResult).sortedIntervals;
            if (getSampleCondition().reverse) {
                for (Range range : sortedIntervals) {
                    for (int i = range.getLow(); i <= range.getHigh(); i++) {
                        sampledIndex.add(i);
                        if (sampledIndex.size() >= sampleCount) {
                            break;
                        }
                    }
                }
            } else {
                for (int i = sortedIntervals.size() - 1; i >= 0; i--) {
                    Range range = sortedIntervals.get(i);
                    for (int j = range.getHigh(); j >= range.getLow(); j--) {
                        sampledIndex.add(j);
                        if (sampledIndex.size() >= sampleCount) {
                            break;
                        }
                    }
                }
            }
        } else {
            BaseIndex baseIndex = null;
            for (String indexName : indexResult.indexes.keySet()) {
                baseIndex = indexResult.indexes.get(indexName);
                if (baseIndex.getSamplingColumn() != null && baseIndex.getSamplingColumn().compareTo(getSampleCondition().key) == 0) {
                    break;
                }
            }
            if (baseIndex.getIndexColumn() == null && cachedIndex != null) {
                return new ArrayList<>(sampledIndex);
            }
            priorityQueue.clear();
            List<Range> sortedIntervals = ((RangeIndexResult) indexResult).sortedIntervals;

            if (baseIndex.getSamplingDtype().compareToIgnoreCase("float") == 0) {
                List<Float> weights = baseIndex.getFloatAttributes(getSampleCondition().key);
                for (Range range : sortedIntervals) {
                    for (int i = range.getLow(); i < range.getHigh(); i++) {
                        priorityQueue.add(new Node(i, weights.get(i)));
                    }
                }
            } else if (baseIndex.getSamplingDtype().compareToIgnoreCase("long") == 0) {
                List<Long> weights = baseIndex.getLongAttributes(getSampleCondition().key);
                for (Range range : sortedIntervals) {
                    for (int i = range.getLow(); i < range.getHigh(); i++) {
                        priorityQueue.add(new Node(i, weights.get(i)));
                    }
                }
            } else {
                List<String> weights = baseIndex.getStringAttributes(getSampleCondition().key);
                for (Range range : sortedIntervals) {
                    for (int i = range.getLow(); i < range.getHigh(); i++) {
                        priorityQueue.add(new Node(i, weights.get(i)));
                    }
                }
            }

            for (int i = 0; i < sampleCount; i++) {
                Node p = priorityQueue.poll();
                sampledIndex.add(p.index);
            }
            if (baseIndex.getIndexColumn() == null && cachedIndex == null) {
                cachedIndex = new ArrayList<>(sampledIndex);
            }
        }
        return sampledIndex;
    }
}
