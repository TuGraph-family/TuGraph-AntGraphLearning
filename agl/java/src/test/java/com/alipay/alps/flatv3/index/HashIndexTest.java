package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter_exp.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter_exp.FilterConditionParser;
import com.alipay.alps.flatv3.index.result.IndexResult;
import com.antfin.agl.proto.sampler.CmpExp;
import com.antfin.agl.proto.sampler.Element;
import com.antfin.agl.proto.sampler.LogicExps;
import com.antfin.agl.proto.sampler.VariableSource;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashIndexTest {
    @Test
    public void testHashIndex() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> edgeIDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset<Integer> neighborDataset = new NeighborDataset<>(node2IDs, edgeIDs);

        // Add some attributes to the neighbor dataset
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributes("node_type", typeList);

        // Create a hash index
        HashIndex hashIndex = new HashIndex("hash_index:node_type:string", neighborDataset);

        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        String filterCond = "index.node_type in (user, shop)";
        FilterConditionParser filterConditionParser = new FilterConditionParser();
        LogicExps logicExps = filterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        IndexResult indexResult = hashIndex.search(new CategoryCmpWrapper(cmpExp), inputVariables);
        assertArrayEquals(Arrays.asList(1, 2, 4).toArray(), indexResult.getIndices().toArray());
    }
}

