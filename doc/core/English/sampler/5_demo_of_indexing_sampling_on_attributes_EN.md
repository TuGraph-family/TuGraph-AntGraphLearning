# Application of Attribute Indexing and Sampling

AGL provides functionality for [building indexes](../../English/sampler/3_neighbor_attribute_indexing_EN.md) based on
attributes.
When performing neighbor sampling, efficient [filtering](../../English/sampler/2_neighbor_filtering_condition_EN.md)
and [sampling](../../English/sampler/4_neighbor_sampling_EN.md) based on attributes can be applied to meet the personalized
requirements of different algorithms.

## Preparing Graph Data

Let's take an example of dynamic graph sampling, where the graph data consists of Sparse KV features with dim=3 for
nodes and Dense Float features with dim=2 for edges. The graph data format is as follows:

```json
{
  'node_spec': [
    {
      'node_name': 'default',
      'id_type': 'string',
      'features': [
        {
          'name': 'sparse_kv',
          'type': 'sparse_kv',
          'dim': 3,
          'key': 'int64',
          'value': 'float32'
        }
      ]
    }
  ],
  'edge_spec': [
    {
      'edge_name': 'default',
      'n1_name': 'default',
      'n2_name': 'default',
      'id_type': 'string',
      'features': [
        {
          'name': 'feature',
          'type': 'dense',
          'dim': 2,
          'value': 'float32'
        }
      ]
    }
  ],
  'edge_attr': [
    {
      'field': 'time',
      'dtype': 'long'
    }
  ],
  'seed': {
    'attr': [
      {
        'field': 'time',
        'dtype': 'long'
      }
    ]
  }
}
```

### Input Data Tables

The sampling rule for the dynamic graph is as follows: the time of the root node is greater than the time of the
first-hop node, the time of the first-hop node is greater than the time of the second-hop node, and so on.
Here are examples of input tables:

Node Table:

| node_id  | node_feature |
| -------- | ------------ |
|    1     |     0 2 4    |
|    2     |     0 3 5    |
|    3     |     1 3 8    |
|    4     |     3 4 6    |

Edge Table:

|  node1_id  |  node2_id  | edge_feature | time |
| ---------- | ---------- | ------------ | ---- |
|     1      |     0      |   2.3 3.2    |  2   |
|     0      |     1      |   3.4 4.9    |  10  |
|     1      |     3      |   2.5 0.8    |  21  |
|     2      |     10     |   1.2 2.3    |  34  |

Sample Table:

|  node_id  |    label   |  train_flag   | time |
| --------- | ---------- | ------------- | ---- |
|     0     |  0 0 ... 1 |     train     |  30  |
|     2     |  1 1 ... 0 |     eval      |  20  |
|     5     |  1 0 ... 0 |     test      |  50  |

### Configuring Index

To accelerate filtering based on "time" attribute on both seed nodes and edges, you can configure a range index for "
time":
index="range_index:time:long"

### Configuring Filtering Conditions

The sampling rule for dynamic graphs is as follows: the time of the root node is greater than the time of the first-hop
node, and the time of the first-hop node is greater than the time of the second-hop node.
This can be expressed as:
filter_cond="neighbor_1.time < seed.time AND neighbor_2.time < neighbor_1.time"

### Configuring Sampling Rules

Suppose the user's sampling rule is to select the top 3 neighbors with the highest "time" attribute value. The sampling
rule can be expressed as:
sample_cond="topk(by=time, limit=3, reverse=True)"

## Running Spark to Generate Subgraph Samples

Here is the user configuration for running Spark locally:

 ``` 
/path_to/spark-3.1.1-odps0.34.1/bin/spark-submit  --master local --class com.alipay.alps.flatv3.spark_back.DynamicGraph \
    /path_to/agl.jar hop=2 \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'sparse_kv','type':'sparse_kv','dim':3,'key':'int64','value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'feature','type':'dense','dim':2,'value':'float32'}]}],'edge_attr':[{'field':'time','dtype':'long'}],'seed':{'attr':[{'field':'time','dtype':'long'}]}}"  \
    sample_cond="topk(by=time, limit=3, reverse=True)"   \
    filter_cond="neighbor_1.time < seed.time AND neighbor_2.time < neighbor_1.time" \
    input_node_feature="file:////path_to/node_table.csv" \
    input_edge="file:////path_to/edge_table.csv" \
    input_label="file:////path_to/label.csv" \
    output_results='file:////path_to/output_subgraph' 2>&1 | tee logfile.txt
 ``` 

### Configuration Explanation

| Configuration | Description |
| --- | --- |
| --master local | Spark runs in local mode. |
| --class com.alipay.alps.flatv3.spark.NodeLevelSampling | Spark program entry point for node-level graph sampling. |
| hop=2 | Perform 2-hop neighbor sampling. |
| subgraph_spec | Defines the graph data format. |
| sample_cond="topk(by=time, limit=3, reverse=True)" | Samples the top 3 neighbors with the highest "time" for each node. |
| filter_cond="neighbor_1.time < seed.time AND neighbor_2.time < neighbor_1.time" | Filtering condition: the time of the root node is greater than the time of the first-hop node, and the time of the first-hop node is greater than the time of the second-hop node. |
| input_node_feature="file:////path_to/node_table.csv" | Specifies the local path for the node table. |
