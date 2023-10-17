## Data Parsing

As the current open-source version primarily reads data from CSV files, CSV lacks the ability to indicate the data
format for each column. Therefore, when reading the data, it is treated as strings.
Hence, it is necessary to parse the data from string to tensors required by the model, including parsing the
GraphFeature and
other columns (such as the label column).

For the GraphFeature column, we provide the [PySubGraph](../../../agl/python/data/subgraph/subgraph.py) class for
parsing. The main parsing logic is implemented in C++ to deserialize the protobuf string and assemble it into tensors
required by the model (typically a batch of data).

For non-GraphFeature columns, we provide several [Column](../../../agl/python/data/column.py) classes (AGLDenseColumn,
AGLMultiDenseColumn, AGLRowColumn) to indicate the data format and perform parsing.

Currently, we provide two collate functions that integrate the above parsing methods for data parsing using torch
dataloader (for information on using torch collate_fn, please refer to
the [link](https://pytorch.org/docs/stable/data.html#working-with-collate-fn)).

* [AGLHomoCollateForPyG](../../../agl/python/data/collate.py)

  AGLHomoCollateForPyG is a basic collate data processing class designed for parsing data that contains a column of
  homogeneous graph features. Taking PPI data as an example, the PPI data consists of only one type of node (assuming
  the name is "default") and one type of edge. Each node contains a dense feature of 50 dimensions. We define the
  configuration information for this graph using the following approach:
  ```python
    # node related spec
    node_spec = NodeSpec("default", AGLDType.STR)
    node_spec.AddDenseSpec("feature", DenseFeatureSpec("feature", 50, AGLDType.FLOAT))
    # edge related spec
    edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
  ```
  Note: For heterogeneous data or data with multiple types of features, you only need to add node_spec and feature_spec
  definitions accordingly.

  Similar to typical DNNs, a training sample in AGL may not only have GraphFeatures but also other columns such as ids
  and labels. In the case of typical PPI training samples, each sample represents a small graph with multiple nodes,
  each node having a label that can be considered as a dense feature of 121 dimensions.
  To specify the configuration information for node ids and labels, you can follow the approach below:
  ```python
    # For the label_list column, let's assume that within each node, the labels are separated by a space (" "), and different nodes are separated by a tab ("\t").
    label_column = AGLMultiDenseColumn(
        name="label_list", dim=121, dtype=np.int64, in_sep=" ", out_sep="\t"
    ) 
    # The node_id_list is currently of string type, and it will be returned as is.
    root_id_column = AGLRowColumn(name="node_id_list")

  ```
  It should be noted that after collate function, parsed data will be stored
  in [TorchEgoBatchData](../../../agl/python/data/subgraph/pyg_inputs.py)
  or [TorchSubGraphBatchData](../../../agl/python/data/subgraph/pyg_inputs.py).
  The main difference is how to represent the adjacency matrix for multi-hops. In TorchEgoBatchData, the adj matrix
  changes with the number of hops, with a total of hops_num adj matrices. The number of edges decreases from the first
  layer to the last layer, reducing unnecessary computations theoretically. This is achieved through the configuration
  of ego_edge_index in the collate function.

* [MultiGraphFeatureCollate](../../../agl/python/data/multi_graph_feature_collate.py)

  In certain link-based tasks or heterogeneous graph applications, a single sample may contain multiple GraphFeatures.
  Additionally, preprocessing and postprocessing of the data may be required. To address these requirements, we provide
  a MultiGraphFeatureCollate class for handling such data.

  The primary capability of this class is data parsing. For usage examples, please refer
  to [link1](../../../agl/python/data/collate_test.py)
  and [link2](../../../agl/python/examples/hegnn_acm/model_hegnn.py).

If the current functionality does not meet your requirements, you are welcome to build your own parsing logic based
on [PySubGraph](../../../agl/python/data/subgraph/subgraph.py) and [Column](../../../agl/python/data/column.py). These
classes provide a foundation for creating your customized parsing logic.



