# Data preparation

## Graph data model
### Description of node table and edge table
AGL supports heterogeneous attribute feature graphs, where the basic elements are nodes and edges. Nodes are identified by node_id, and edges are directed and identified by edge_id. That is, the same two nodes can have multiple edges of the same type at the same time.

The types of node_id and edge_id are both strings, and int64 type will be supported in the future.

The types of nodes and edges are both described in strings.

Nodes and edges can have features, both of which are described in strings. Features are divided into four categories: Dense, SparseKV, SparseK (Binary will be supported in the future) features; their formats are as follows:

| Feature type | Configuration description | Example description |
|---|---|---|
| Dense | value's dtype, dim | value=float32, dim=4, feature=0.6 0.1 0.3 0.4 |
| SparseKV | key's and value's dtype, dim | key=int64, value=float32, dim=10, feature=1:2.1 9:3.2 |
| SparseK | key's dtype, dim | key=int64, dim=10, feature=1 3 4 6 |
| Binary | | A apple |

```
Separator: Separate multiple elements with spaces, and separate key and value with colons
```

Both node and edge can have multiple features, which are concatenated with tabs. For example, a product node can have a Dense feature (sales in the past 7 days) and a Binary feature (description), so its feature is expressed as:

```
1 6 3 4 3 2 9\tapple
```

Samples can be divided into three categories: point-level, edge-level, and graph-level.

In particular, samples and edges can also have some attributes, which are divided into three categories: float, int64, and String. The role of attributes is to provide a basis for selecting neighboring nodes, such as: if we only select the topk neighbors of each node based on the transaction amount, we can take the transaction amount as the edge attribute and take the current node's topk neighbors.

Here is an example of graph data:

### Graph data format
```
{
  'node_spec': [
    {
      'node_name': 'user',
      'id_type': 'string',
      'features': [
        {
          'name': 'f1',
          'type': 'sparse_kv',
          'dim': 4,
          'key': 'int64',
          'value': 'float32'
        }
      ]
    },
    {
      'node_name': 'item',
      'id_type': 'string',
      'features': [
        {
          'name': 'f2',
          'type': 'dense',
          'dim': 2,
          'value': 'float32'
        }, 
        {
          'name': 'f3',
          'type': 'sparse_kv',
          'dim': 3,
          'key': "int64',
          'value': 'float32'
        }
      ]
    }
  ],
  'edge_spec': [
    {
      'edge_name': 'click',
      'n1_name': 'user',
      'n2_name': 'item',
      'id_type': 'string',
      'features': [
        {
          'name': 'relation',
          'type': 'sparse_k',
          'dim': 4,
          'key': 'int64'
        }
      ]
    },
    {
      'edge_name': 'friends',
      'n1_name': 'user',
      'n2_name': 'user',
      'id_type': 'string',
      'features': [
      ]
    }
  ],
  'edge_attr': [
     {
       'field': 'time',
       'dtype': 'long'
     }
  ],
  'label': {
      'attr': [
       {
          'field': 'time',
          'dtype': 'long'
       }
     ]
  }
}
```

Graph data format is expressed in JSON format, consisting mainly of nodes, edges and labels, which are stored in the "node_spec", "edge_spec" and "label" objects of the JSON object.

#### node_spec
Each element in the list defines the format of a type of node:

* `node_name` indicates the node type,
* `id_type` indicates the id type (currently only supports String),
* `features` describes the list of features for this type of node.

#### edge_spec
Each element in the list defines the format of a type of edge:

* `edge_name` indicates the edge type,
* `n1_name` indicates the end point type,
* `n2_name` indicates the start point type,
* `id_type` indicates the id type (currently only supports String),
* `features` describes the list of features for this type of edge.

#### features
Each element in the list defines the format of a type of feature:

* `name` indicates the feature name,
* `type` indicates the feature type, which can be one of: dense, sparse_kv, sparse_k (binary will be supported in the future)
* `dim`: `dim` is the number of elements in the dense type, and `dim` - 1 is the maximum key value in the sparse_kv and sparse_k types.
* `key` dtype range: int64 (not applicable to dense type)
* `value` dtype range: float32, float64, int64 (not applicable to sparse_k type)

#### edge_attr
Defines the names and corresponding types of attributes on the edge.

#### label
Defines the names and corresponding types of attributes of the sample.


#### Point table

The point table corresponding to the graph data format is as follows:

| node_id | node_feature | type |
|---|---|---|
| user1 | 0:1.0 1:1.3 | user |
| user2 | 2:0.34 | user |
| user3 | 1:1.3 3:0.5 | user |
| item1 | 3.1 6.3\t2:4.6 | item |
| item2 | 0.2 0.4\t1:2.3 | item |
 ``` 
For the case of homogeneous graphs, the types of points and edges are both defaulted to "default", and the "type" column does not need to be stored
 ``` 
#### Edge table

| node1_id | node2_id | edge_id | edge_feature | type |
|---|---|---|---|---|
| user1 | item1 | e1 | 0 1 3 | click |
| user2 | item1 | e2 | 0 2 | click |
| user3 | item2 | e3 | 1 | click |
| user2 | item3 | e4 | 2 3 | click |
| user1 | user2 | e5 | | friends |
| user2 | user1 | e6 | | friends |

## Sample table description

### General sample table

In the training stage, the sample nodes usually only account for 10% or less of the total graph nodes. 
In the training stage, only the graph feature with the label nodes need to be produced (the full-scale graph samples may be needed in the prediction stage).

The training process often involves multiple rounds of iteration of graph data, that is, the process of constantly modifying the graph structure and graph features. 
If only the graph feature of the Label nodes are generated in each iteration of the graph data, the efficiency of the graph training process can be greatly improved

### Node Representation
In a classification subtask with 100 nodes, only nodes 1 and 3 have training labels. We call these two nodes "seeds" and only need to generate subgraph samples for these two nodes.

The label table is as follows:

| seed | node_id | label | other1 | other2 |
|---|---|---|---|---|
| 1 | 1 | 0 1 | 23 | 2 |
| 3 | 3 | 1 0 | 15 | 5 |
 ```
Required columns are seed, node_id, and label.
The system uses node_id as the seed and propagates k times using the edge table to get k-hop neighbors, as a new column output.
Other columns, such as other1, other2, etc., are output as is.
 ```
 
The results table is as follows:

| seed | node_id | label | other1 | other2 | graph_feature |
|---|---|---|---|---|---|
| 1 | 1 | 0 1 | 23 | 2 | 1's serialized subgraph |
| 3 | 3 | 1 0 | 15 | 5 | 3's serialized subgraph |

### Link-level sample table

In the task of link prediction with 100 edges, only two edges <1,3> and <3,5> have training labels. We take these three nodes (1,3,5) as "seeds" and only need to generate the subgraph feature of these three nodes.

The label table is as follows:

| seed | node1_id | node2_id | label | other1 |
|---|---|---|---|---|
| l_1_3 | 1 | 3 | 0 1 | 23 |
| l_3_5 | 3 | 5 | 1 0 | 15 |
 ```
Required columns are seed, node1_id, node2_id, and label.
The system takes the union of node1_id and node2_id without duplicates as the seed set, and propagates k times using the edge table to get k-hop neighbors, as a new column output.
Other columns, such as other1, etc., are output as is.
 ```
If we want to merge the subgraphs of both node1_id and node2_id, (for example, the KCAN model), the results table is as follows:

| seed | node1_id | node2_id | label | other1 | graph_feature |
|---|---|---|---|---|---|
| l_1_3 | 1 | 3 | 0 1 | 23 | Serialized subgraph of 1 and 3 |
| l_3_5 | 3 | 5 | 1 0 | 15 | Serialized subgraph of 3 and 5 |

The merged subgraph will have two root nodes, in the order of node1_id and node2_id.

If the subgraphs of node1_id and node2_id does not need to be merged (for example, the CD-GNN model), the result table is as follows:

| seed    | node1_id | node2_id   | label    |  other1   | graph_feature | graph_feature_2 |
| ------- | -------- | ---------- | -------- | --------- | ------------- | ------------ |
| l_1_3   | 1        | 3          |   0 1    |     23    | 1's serialized subgraph | 3's serialized subgraph |
| l_3_5   | 3        | 5          |   1 0    |     15    | 3's serialized subgraph | 5's serialized subgraph |

## Subgraph Sampling

In PPI and other data sets, a group of nodes can form an independent small graph. To avoid the repeated sampling and calculation of the k-hop subgraph for each node,
we can propagate k-hop from a group of nodes and merge them into a graph_feature, and deduplication can be performed in this process.

### Subgraph Sampling for Node Representation

This is for graph learning models based on nodes, i.e., each node has a label.
This label table is as follows:

| node_id | seed | label | other1 |
|---|---|---|---|
| 1 | g1 | 0 1 | 3 |
| 2 | g2 | 1 0 | 23 |
| 3 | g1 | 1 0 | 9 |
| 4 | g2 | 0 1 | 7 |
| 5 | g1 | 0 1 | 10 |

```
The required columns are node_id, graph_id, and label.
The system takes the node_ids as the seed set, uses the edge table to propagate k hops to get k-hop neighbors, and finally merges them into a graph_feature as a new column output.
label and other columns like other1,..., are combined into a list grouped by the graph_id, while maintaining the corresponding relationship with node_id.
```

The results table is as follows:

| node_id | seed | label | other1 | graph_feature |
|---|---|---|---|---|
| 1 3 5 | g1 | [0 1, 1 0, 0 1] | [3, 9, 10] | g1's serialized subgraph |
| 2 4 | g2 | [1 0, 0 1] | [23, 7] | g2's serialized subgraph |

### Subgraph Sampling for Graph Representation

For some graph learning models based on subgraphs, a small graph has a label.
This type of sample is as follows:

| node_id | seed | label | other_feature |
|---|---|---|---|
| 1 3 5 | g1 | 0 1 | 23 |
| 2 4 6 | g2 | 1 0 | 15 |

```
The required columns are node_id, graph_id, and label.
The system takes all node_ids as the seed set, uses the edge table to propagate k hops to get k-hop neighbors, and merges them into a graph_feature as a new column output.
Other columns other1,..., are output as is.
```

The results table is as follows:

| node_id | seed | label | other_feature | graph_feature |
|---|---|---|---|---|
| 1 3 5 | g1 | 0 1 | 23 | g1's serialized subgraph |
| 2 4 6 | g2 | 1 0 | 15 | g2's serialized subgraph |
