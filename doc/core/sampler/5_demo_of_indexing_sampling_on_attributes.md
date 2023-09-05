# 属性索引和采样的应用
AGL提供了基于属性[建立索引](./3_neighbor_attribute_indexing.md)的功能，
进行邻居采样时可以基于属性进行高效的[过滤](./2_neighbor_filtering_condition.md)和[采样](./4_neighbor_sampling.md)，来满足不同算法的个性化需求。

## 图数据准备

举个动态图采样例子，图数据点特征为dim=3的Sparse Key特征，边特征为dim=2的Dense Float特征。图数据格式如下：
 ``` 
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


### 输入数据表
动态图的采样规则为：根节点的time大于第一跳节点的time，第一跳节点的time大于第二跳节点的time，以此类推。
举例输入的点表：

| node_id  | node_feature |
| -------- | ------------ |
|    1     |     0 2 4    |
|    2     |     0 3 5    |
|    3     |     1 3 8    |
|    4     |     3 4 6    |

举例输入的边表：

|  node1_id  |  node2_id  | edge_feature | time |
| ---------- | ---------- | ------------ | ---- |
|     1      |     0      |   2.3 3.2    |  2   |
|     0      |     1      |   3.4 4.9    |  10  |
|     1      |     3      |   2.5 0.8    |  21  |
|     2      |     10     |   1.2 2.3    |  34  |

样本表如下：

|  node_id  |    label   |  train_flag   | time |
| --------- | ---------- | ------------- | ---- |
|     0     |  0 0 ... 1 |     train     |  30  |
|     2     |  1 1 ... 0 |     eval      |  20  |
|     5     |  1 0 ... 0 |     test      |  50  |

### 配置索引
因为需要根据种子节点和边上的time属性进行过滤，所以为了加速过滤，可以配置time的range索引：
index="range_index:time:long"

### 配置过滤条件
动态图的采样规则为：根节点的time大于第一跳节点的time，第一跳节点的time大于第二跳节点的time。表达为:
filter_cond="neighbor_1.time < seed.time AND neighbor_2.time < neighbor_1.time"

### 配置采样规则
假设用户的采样规则为选择邻居中time最大的3个邻居，采样规则表达为：
sample_cond="topk(by=time, limit=3, reverse=True)"

## 运行Spark生成子图样本

用户配置spark本地运行命令如下：
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


### 配置说明

|                           配置                                                   |                说明              |
| ------------------------------------------------------------------------------- | -------------------------------- |
| --master local                                                                  | spark本地运行模式                  |
| --class com.alipay.alps.flatv3.spark.NodeLevelSampling                          | spark程序入口：点级别图采样            |
| hop=2                                                                           | 进行2跳邻居采样                     |
| subgraph_spec                                                                   | 定义图数据格式                      |
| sample_cond="topk(by=time, limit=3, reverse=True)"                              | 每个节点采样time最大3个邻居节点       |
| filter_cond="neighbor_1.time < seed.time AND neighbor_2.time < neighbor_1.time" | 邻居过滤：根节点的time大于第一跳节点的time，第一跳节点的time大于第二跳节点的time |
| input_node_feature="file:////path_to/node_table.csv"                            | 前缀file:///表示后续接着本地路径      |
