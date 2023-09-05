/**
 * Copyright 2023 AntGroup CO., Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.alipay.alps.flatv3.spark.utils;

import java.util.HashMap;
import java.util.Map;

public class Constants {

  public static final String NODE = "node";
  public static final String EDGE = "edge";
  public static final String ROOT = "root";
  public static final int NODE_INT = 0;
  public static final int EDGE_INT = 1;
  public static final int ROOT_INT = 2;
  public static final String LIST_STR = "array<string>";
  public static final String LIST_FLOAT = "array<float>";
  public static final String LIST_DOUBLE = "array<double>";
  public static final String LIST_LONG = "array<bigint>";

  public static final String D2_PROJECT = "d2_project";
  public static final String SEED_TABLE = "seed_table";
  public static final String NEIGHBORS_TABLE = "neighbors_table";
  public static final String NODE_TABLE = "node_table";
  public static final String EDGE_TABLE = "edge_table";
  public static final String RESULT_TABLE = "result_table";
  public static final String INDEX_METAS = "index_metas";
  public static final String FILTER_COND = "filter_cond";
  public static final String SAMPLE_COND = "sample_cond";
  public static final String OTHER_OUTPUT_SCHEMA = "other_output_schema";
  public static final String HOP = "hop";
  public static final String SUBGRAPH_SPEC = "subgraph_spec";
  public static final String OUTPUT_TABLE = "output_table";
  public static final String HAS_TYPE = "has_type";
  public static final String HAS_EDGE_WEIGHT = "has_edge_weight";
  public static final String INPUT_LABEL = "input_label";
  public static final String INPUT_LINK = "input_link";
  public static final String INPUT_NEIGHBOR = "input_neighbor";
  public static final String INPUT_EDGE = "input_edge";
  public static final String INPUT_FEATURE = "input_feature";
  public static final String OUTPUT_RESULTS = "output_results";
  public static final String INPUT_NODE_FEATURE = "input_node_feature";
  public static final String INPUT_EDGE_FEATURE = "input_edge_feature";
  public static final String HEGNN_MODE = "hegnn_mode";
  public static final String HEGNN_TWO_END = "two_end";
  public static final String HEGNN_PATH = "path";

  public static final String STORE_IDS = "store_id_in_subgraph";
  public static final String STORE_IDS_DEFAULT = "true";
  public static final String TRAIN_FLAG = "train_flag";

  public static final String REMOVE_TARGET_EDGE = "remove_target_edge";

  public static final String ENTRY_SEED = "seed";
  public static final String ENTRY_TYPE = "entryType";
  public static final String ENTRY_NODE1 = "node1";
  public static final String ENTRY_NODE2 = "node2";
  public static final String ENTRY_ID = "id";
  public static final String ENTRY_FEATURE = "feature";
  public static final String ENTRY_KIND = "type";

  public static final String LABEL_TYPE = "label_type";
  public static final String NODE_LEVEL = "node_level";
  public static final String EDGE_LEVEL = "edge_level";
  public static final String GRAPH_LEVEL = "graph_level";
  public static final String MERGE_SUBGRAPH = "merge_subgraph";
  public static final String MERGE_SUBGRAPH_DEFAULT = "true";
  public static final String REMOVE_EDGE_AMONG_ROOTS = "remove_edge_among_roots";
  public static final String REMOVE_EDGE_AMONG_ROOTS_DEFAULT = "false";

  public static final String LABEL_LEVEL = "label_level";
  public static final String LABEL_LEVEL_ON_NODE = "label_on_node";
  public static final String LABEL_LEVEL_ON_LINK = "label_on_link";
  public static final String LABEL_LEVEL_ON_GRAPH = "label_on_graph";

  public static Map<String, Integer> ELEMENT_FIELD_INDEX;

  static {
    ELEMENT_FIELD_INDEX = new HashMap<>();
    ELEMENT_FIELD_INDEX.put(Constants.ENTRY_SEED, 0);
    ELEMENT_FIELD_INDEX.put(Constants.ENTRY_TYPE, 1);
    ELEMENT_FIELD_INDEX.put(Constants.ENTRY_NODE1, 2);
    ELEMENT_FIELD_INDEX.put(Constants.ENTRY_NODE2, 3);
    ELEMENT_FIELD_INDEX.put(Constants.ENTRY_ID, 4);
    ELEMENT_FIELD_INDEX.put(Constants.ENTRY_FEATURE, 5);
    ELEMENT_FIELD_INDEX.put(Constants.ENTRY_KIND, 6);
  }
}
