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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RangeIndexTest {

    @Test
    public void testComparisonFilterWithIndexValue() throws Exception {
        // create a RangeIndex object with some test data
        NeighborDataset neighborDataset = new NeighborDataset(3);
        neighborDataset.addAttributeList("weight", Arrays.asList(new Float[]{3.0F, 2.0F, 1.0F}));
        BaseIndex rangeIndex = IndexFactory.createIndex("range_index:weight:float", neighborDataset);

        // check if the index is built correctly
        int[] expectedOriginIndex = new int[]{2, 1, 0};
        int[] actualOriginIndex = rangeIndex.getOriginIndices();
        assertArrayEquals(expectedOriginIndex, actualOriginIndex);

        List<Float> expectedIndexedData = Arrays.asList(new Float[]{3.0F, 2.0F, 1.0F});
        List<Float> actualIndexedData = neighborDataset.getAttributeList("weight");
        assertEquals(expectedIndexedData, actualIndexedData);

        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        String filterCond = "index.weight < 1.4";
        LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        AbstractIndexResult indexResult = rangeIndex.search(new ArithmeticCmpWrapper(cmpExp), inputVariables);
        assertArrayEquals(Arrays.asList(2).toArray(), indexResult.getIndices().toArray());
    }

    @Test
    public void testComparisonFilterWithIndexAndSeedValues() throws Exception {
        NeighborDataset neighborDataset = new NeighborDataset(5);
        neighborDataset.addAttributeList("timestamp", Arrays.asList(new Long[]{3L, 5L, 1L, 2L, 9L}));
        BaseIndex rangeIndex = IndexFactory.createIndex("range_index:timestamp:long", neighborDataset);

        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        Map<String, Element.Number> seedVariableMap = new HashMap<>();
        seedVariableMap.put("1", Element.Number.newBuilder().setI(2).build());
        inputVariables.put(VariableSource.SEED, seedVariableMap);

        String filterCond = "index.timestamp - seed.1 >= 2";
        LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        AbstractIndexResult indexResult = rangeIndex.search(new ArithmeticCmpWrapper(cmpExp), inputVariables);
        System.out.println("------indexResult:" + Arrays.toString(indexResult.getIndices().toArray()));
        assertArrayEquals(Arrays.asList(1, 4).toArray(), indexResult.getIndices().toArray());
    }
}
