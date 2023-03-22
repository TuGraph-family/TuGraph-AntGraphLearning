package com.alipay.alps.flatv3;

import com.alipay.alps.flatv3.index.BaseIndex;
import com.alipay.alps.flatv3.index.HashIndex;
import com.alipay.alps.flatv3.index.RangeIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class FilterAndSamplerTest {
    @Test
    public void test1() throws Exception {
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11";
        String indexMeta = "range_index:time:long,";
        String sampleCond = "topk(by=time, limit=2)";

        FilterAndSampler filterAndSampler = new FilterAndSampler("", filterCond, sampleCond);

        List<String> ids = new ArrayList<>();
        List<Long> timestamp = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            timestamp.add(2L * i);
        }

        RangeIndex weightIndex = new RangeIndex(indexMeta);
        weightIndex.setNode2IDs(ids);
        weightIndex.addAttributes("time", timestamp);
        weightIndex.buildIndex();
        List<BaseIndex> indexList = new ArrayList<>();
        indexList.add(weightIndex);

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }
        List<List<Integer>> result = filterAndSampler.process(seedIds, seedAttrs, indexList, null, null);
        // assert
    }

    @Test
    public void test2() throws Exception {
        String filterCond = "index.time - seed.1 >= 0.5 AND index.time <= seed.1 + 11 OR index2.type in ('item', 'user')";
        String indexMeta1 = "range_index:time:long,";
        String indexMeta2 = "hash_index:type:string,";
        String sampleCond = "topk(by=time, limit=2)";

        FilterAndSampler filterAndSampler = new FilterAndSampler("", filterCond, sampleCond);

        List<String> ids = new ArrayList<>();
        List<Long> timestamp = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(String.valueOf(i));
            timestamp.add(2L * i);
        }
        List<String> types = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (i % 3 == 0) {
                types.add("user");
            } else if (i % 3 == 1) {
                types.add("shop");
            } else {
                types.add("item");
            }
        }

        BaseIndex timeIndex = new RangeIndex(indexMeta1);
        timeIndex.setNode2IDs(ids);
        timeIndex.addAttributes("time", timestamp);
        timeIndex.buildIndex();

        BaseIndex typeIndex = new HashIndex(indexMeta2);
        typeIndex.setNode2IDs(ids);
        typeIndex.addAttributes("type", types);
        typeIndex.buildIndex();

        List<BaseIndex> indexList = new ArrayList<>();
        indexList.add(timeIndex);
        indexList.add(typeIndex);

        List<String> seedIds = Arrays.asList("1", "2", "3", "4", "5");
        List<List<Object>> seedAttrs = new ArrayList<>();
        for (int i = 0; i < seedIds.size(); i++) {
            seedAttrs.add(Arrays.asList((i+1) * 1L));
        }
        List<List<Integer>> result = filterAndSampler.process(seedIds, seedAttrs, indexList, null, null);
        // assert
    }
}