package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.ArithmeticCmpWrapper;

import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.result.AbstractIndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RangeIndexTest {

    @Test
    public void testComparisonFilter1() throws Exception {
        // create a RangeIndex object with some test data
        List<String> node2IDs = Arrays.asList(new String[]{"A", "B", "C"});
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());
        neighborDataset.addAttributeList("weight", Arrays.asList(new Float[]{3.0F, 2.0F, 1.0F}));
        RangeIndex rangeIndex = new RangeIndex("range_index:weight:float", neighborDataset);

        // check if the index is built correctly
        Integer[] expectedOriginIndex = new Integer[]{2, 1, 0};
        Integer[] actualOriginIndex = rangeIndex.getOriginIndex();
        assertArrayEquals(expectedOriginIndex, actualOriginIndex);

        List<Float> expectedIndexedData = Arrays.asList(new Float[]{3.0F, 2.0F, 1.0F});
        List<Float> actualIndexedData = neighborDataset.getAttributeList("weight");
        assertEquals(expectedIndexedData, actualIndexedData);

        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        String filterCond = "index.weight < 1.4";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        AbstractIndexResult indexResult = rangeIndex.search(new ArithmeticCmpWrapper(cmpExp), inputVariables);
        assertArrayEquals(Arrays.asList(2).toArray(), indexResult.getIndices().toArray());
    }

    @Test
    public void testSearch() throws Exception {
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());
        neighborDataset.addAttributeList("timestamp", Arrays.asList(new Long[]{3L, 5L, 1L, 2L, 9L}));
        RangeIndex rangeIndex = new RangeIndex("range_index:timestamp:long", neighborDataset);

        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        seedVariableMap.put("1", Element.Number.newBuilder().setI(2).build());
        inputVariables.put(VariableSource.SEED, seedVariableMap);

        String filterCond = "index.timestamp - seed.1 >= 2";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        AbstractIndexResult indexResult = rangeIndex.search(new ArithmeticCmpWrapper(cmpExp), inputVariables);
        assertArrayEquals(Arrays.asList(1,4).toArray(), indexResult.getIndices().toArray());
    }
}
