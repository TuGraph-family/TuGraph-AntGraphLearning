package com.alipay.alps.flatv3.index;

import com.alipay.alps.flatv3.filter.parser.CategoryCmpWrapper;
import com.alipay.alps.flatv3.filter.parser.FilterConditionParser;
import com.alipay.alps.flatv3.filter.result.AbstractResult;
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

public class HashIndexTest {
    @Test
    public void testHashIndex() throws Exception {
        // Create a neighbor dataset
        List<Integer> node2IDs = Arrays.asList(0, 1, 2, 3, 4);
        NeighborDataset neighborDataset = new NeighborDataset(node2IDs.size());

        // Add some attributes to the neighbor dataset
        List<String> typeList = Arrays.asList("item", "shop", "user", "item", "user");
        neighborDataset.addAttributeList("node_type", typeList);

        // Create a hash index
        BaseIndex hashIndex = IndexFactory.createIndex("hash_index:node_type:string", neighborDataset);

        Map<VariableSource, Map<String, Element.Number>> inputVariables = new HashMap<>();
        String filterCond = "index.node_type in (user, shop)";
        LogicExps logicExps = FilterConditionParser.parseFilterCondition(filterCond);
        CmpExp cmpExp = logicExps.getExpRPN(0).getExp();
        AbstractResult indexResult = hashIndex.search(new CategoryCmpWrapper(cmpExp), inputVariables);
        assertArrayEquals(Arrays.asList(1, 2, 4).toArray(), indexResult.getIndices().toArray());
    }
}

