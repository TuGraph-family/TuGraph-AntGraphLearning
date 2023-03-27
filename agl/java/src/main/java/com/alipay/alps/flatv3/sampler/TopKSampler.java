package com.alipay.alps.flatv3.sampler;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.result.CommonIndexResult;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.alipay.alps.flatv3.index.result.Range;
import com.alipay.alps.flatv3.index.result.RangeIndexResult;

import java.util.ArrayList;
import java.util.Collections;
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
            if (sampleCondition.getKey().compareTo(index.getIndexColumn()) != 0) {
                int factor = sampleCondition.isReverse() ? -1 : 1;
                comparator = new Comparator<Node>() {
                    @Override
                    public int compare(Node o1, Node o2) {
                        return o1.weight.compareTo(o2.weight) * factor;
                    }
                };
                priorityQueue = new PriorityQueue<>(sampleCondition.getLimit(), comparator);
            }
            break;
        }
    }

    @Override
    protected List<Integer> sampleImpl(IndexResult indexResult) {
        int sampleCount = this.getSampleCondition().getLimit();
        int candidateCount = indexResult.getSize();
        if (candidateCount <= sampleCount) {
            return indexResult.getIndices();
        }

        ArrayList<Integer> sampledIndex = new ArrayList<>();
        if (indexResult.getIndexes().containsKey(getSampleCondition().getKey())) {
            // reuse sorted neighbors in indexing stage
            if (indexResult instanceof RangeIndexResult) {
                List<Range> sortedIntervals = ((RangeIndexResult) indexResult).getRangeList();
                if (getSampleCondition().isReverse()) {
                    for (int i = sortedIntervals.size() - 1; i >= 0; i--) {
                        Range range = sortedIntervals.get(i);
                        for (int j = range.getHigh(); j >= range.getLow() && sampledIndex.size() < sampleCount; j--) {
                            sampledIndex.add(j);
                        }
                    }
                } else {
                    for (Range range : sortedIntervals) {
                        for (int i = range.getLow(); i <= range.getHigh() && sampledIndex.size() < sampleCount; i++) {
                            sampledIndex.add(i);
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
            BaseIndex baseIndex = null;
            String dtype = null;
            for (String indexName : indexResult.getIndexes().keySet()) {
                baseIndex = indexResult.getIndexes().get(indexName);
                dtype = baseIndex.getNeighborDataset().getDtype(getSampleCondition().getKey());
                if (dtype != null) {
                    break;
                }
            }
            if (baseIndex.getIndexColumn() == null && cachedIndex != null) {
                return new ArrayList<>(sampledIndex);
            }
            priorityQueue.clear();
            List<Range> sortedIntervals = ((RangeIndexResult) indexResult).getRangeList();

            if (dtype.compareToIgnoreCase("float") == 0) {
                List<Float> weights = baseIndex.getNeighborDataset().getFloatAttributes(getSampleCondition().getKey());
                for (Range range : sortedIntervals) {
                    for (int i = range.getLow(); i < range.getHigh(); i++) {
                        priorityQueue.add(new Node(i, weights.get(i)));
                    }
                }
            } else if (dtype.compareToIgnoreCase("long") == 0) {
                List<Long> weights = baseIndex.getNeighborDataset().getLongAttributes(getSampleCondition().getKey());
                for (Range range : sortedIntervals) {
                    for (int i = range.getLow(); i < range.getHigh(); i++) {
                        priorityQueue.add(new Node(i, weights.get(i)));
                    }
                }
            } else {
                List<String> weights = baseIndex.getNeighborDataset().getStringAttributes(getSampleCondition().getKey());
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
