package com.alipay.alps.flatv3.index;

import java.util.HashMap;
import java.util.Map;

public class IndexFactory {
    public static final String HASH_INDEX = "hash_index";
    public static final String RANGE_INDEX = "range_index";
    public static final String NO_FILTER = "";
    public static Map<String, BaseIndex> indexesMap = new HashMap<>();

    // indexMeta: index_type:column_name:index_dtype
    public static BaseIndex createIndex(String indexMeta, NeighborDataset neighborDataset) {
        String t[] = indexMeta.split(":");
        String indexColumn = NO_FILTER;
        String indexDtype = "";
        if (t.length > 2) {
            indexColumn = t[1];
            indexDtype = t[2];
        }
        return getOrCreate(t[0], indexColumn, indexDtype, neighborDataset);
    }

    public static void clear() {
        indexesMap.clear();
    }

    // if indexColumn can be found in indexesMap, return the index
    // else create a new index, update indexesMap and return the index
    private static BaseIndex getOrCreate(String indexType, String indexColumn, String indexDtype, NeighborDataset neighborDataset) {
        if (indexesMap.containsKey(indexColumn)) {
            BaseIndex index = indexesMap.get(indexColumn);
            assert index.getIndexType().compareToIgnoreCase(indexType) == 0 &&
                    index.getIndexColumn().compareToIgnoreCase(indexColumn) == 0 &&
                    index.getIndexDtype().compareToIgnoreCase(indexDtype) == 0;
            return index;
        }
        if (indexType.compareToIgnoreCase(HASH_INDEX) == 0) {
            indexesMap.put(indexColumn, new HashIndex(indexType, indexColumn, indexDtype, neighborDataset));
        } else if (indexType.compareToIgnoreCase(RANGE_INDEX) == 0) {
            indexesMap.put(indexColumn, new RangeIndex(indexType, indexColumn, indexDtype, neighborDataset));
        } else if (indexType.compareToIgnoreCase(NO_FILTER) == 0) {
            indexesMap.put(indexColumn, new BaseIndex(NO_FILTER, indexColumn, "", neighborDataset));
        } else {
            throw new RuntimeException("Not support hash_range_index yet");
        }
        return indexesMap.get(indexColumn);
    }
}
