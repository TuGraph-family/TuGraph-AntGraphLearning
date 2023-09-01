# 数据准备

## 图数据模型
### 点表和边表描述
AGL支持异构属性特征图，图的基本元素为点和边，点和边都具有类型和特征，点由node_id标识，边是有向的由edge_id标识，即相同的两点之间可以同时具有多条相同类型的边。

点的标识node_id、边的标识edge_id的类型均为String，后期会支持int64类型。

点和边的类型均采用String描述。

点和边可以拥有特征，均采用String描述。特征分为四类：Dense特征，SparseKV特征，SparseK特征（后续扩展Binary特征）；他们的格式如下：

| 特征类型     | 配置项说明                  |                     举例说明                            |
| ----------- | ------------------------- | ----------------------------------------------------- |
| Dense       | value的dtype，dim          |  配置value=float32,dim=4，特征为0.6 0.1 0.3 0.4         |
| SparseKV    | key和value各自的dtype，dim  |  配置key=int64,value=float32,dim=10，特征为1:2.1 9:3.2  |
| SparseK     | key的dtype，dim            |  配置key=int64,dim=10，特征为 1 3 4 6                   |
| Binary      |                           |  一个苹果                                              |

 ```
分隔符：用空格分隔多个元素直接，用冒号分隔单个key:value对
 ```
每个节点或者边可以拥有多个特征，用tab连接起来，例如一个商品节点可以有Dense特征（最近7天销量）和一个Binary特征（描述），则他的特征表示为：
 ```
 1 6 3 4 3 2 9\t苹果
 ``` 
样本可以分为3类：点级别，边级别，图级别。
特别的，样本和边也可以拥有一些属性，分为三类：float，int64，String。属性的作用是为选择邻居节点提供依据，比如：如果针对每个节点只选择交易金额最高的k个邻居，则可以将交易金额作为边属性，取当前节点的topk的邻居节点。

举一个图数据的例子：
### 图数据格式
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
          'key': 'int64'
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
图数据格式用JSON格式表达，由点和边量大部分组成，分别存储在JSON对象的"node_spec"、"edge_spec"中。
#### node_spec
列表中每个元素定义了一类节点的格式：
  * node_name表示节点类型，
  * id_type表示id类型（目前支持String），
  * features描述了该类型节点的特征列表。
#### edge_spec
列表中每个元素定义了一类边的格式：
  * edge_name表示边类型，
  * n1_name表示终点类型，
  * n2_name表示起点类型，
  * id_type表示id类型（目前支持String），
  * features描述了该类型节点的特征列表。
#### features
列表中每个元素定义了一类特征的格式：
  * name表示特征名称，
  * type表示特征类型，可选项为：dense，sparse_kv，sparse_k（后续扩展binary）
  * dim：dense类型下dim为元素个数，sparse_kv和sparse_k类型下dim为最大key值+1
  * key的dtype取值范围：int64（dense类型不适用）
  * value的dtype取值范围：float32,float64,int64（sparse_k类型不适用）
  
#### edge_attr
定义了边上的属性的名称和对应类型

#### label
定义了样本的属性的名称和对应类型

#### 点表
上面图数据格式对应的点表，举例如下：

| node_id    | node_feature    |    type   |
| ---------- | --------------- | --------- |
| user1      |   0:1.0 1:1.3   |    user   |
| user2      |   2:0.34        |    user   |
| user3      |   1:1.3 3:0.5   |    user   |
| item1      | 3.1 6.3\t2:4.6  |    item   |
| item2      | 0.2 0.4\t1:2.3  |    item   |
| item3      | 0.4 1.3\t2:0.9  |    item   |
 ```
对于同构图的情况点和边类型都默认为default，不需要存储type这一列
 ``` 

#### 边表
| node1_id   | node2_id   | edge_id | edge_feature    |    type   |
| ---------- | ---------- | ------- | --------------- | --------- |
| user1      | item1      |    e1   |      0 1 3      |   click   |
| user2      | item1      |    e2   |      0 2        |   click   |
| user3      | item2      |    e3   |      1          |   click   |
| user2      | item3      |    e4   |      2 3        |   click   |
| user1      | user2      |    e5   |                 |  friends  |
| user2      | user1      |    e6   |                 |  friends  |

## 样本表说明
通常在训练阶段，样本节点只占全图节点的10%或者更少，训练阶段仅需产出有Label节点的图样本即可（预测阶段才有可能需要全量节点的图样本）。
而训练过程往往伴随图数据的多轮迭代，即不断修改图结构、图特征的过程。若每次图数据迭代后只生成Label节点的图样本，可大大提高图训练过程的迭代效率。

### 节点表征
假设在100个节点的分类子任务中，只有1和3节点有训练Label，我们把这2个节点称为"种子"，只需要生成这2个节点的子图样本。
样本表举例如下：

| seed    | node_id | label    |  other1   |  other2   |
| ------- | ------- | -------- | --------- | --------- |
| 1       | 1       |   0 1    |     23    |     2     |
| 3       | 3       |   1 0    |     15    |     5     |
 ```
必需列为seed,node_id,label。
系统把node_id作为种子，利用边表传播k次得到k跳邻居graph_feature，作为新列输出。
其他列other1,other2,...，保持原样输出。
 ``` 
结果表如下：

| seed    | node_id | label    |  other1   |  other2   | graph_feature |
| ------- | ------- | -------- | --------- | --------- | ------------- |
| 1       | 1       |   0 1    |     23    |     2     | 1的序列化子图   |
| 3       | 3       |   1 0    |     15    |     5     | 3的序列化子图   |
### 链接表征
假设在100条边的链接预测任务中，只有<1,3>和<3,5>两条边有训练Label，我们把这3个节点（1,3,5）作为"种子"，只需要生成这3个节点的子图样本。
样本表举例如下：

| seed    | node1_id | node2_id   | label    |  other1  |
| ------- | -------- | ---------- | -------- | -------- |
| l_1_3   | 1        | 3          |   0 1    |     23   |
| l_3_5   | 3        | 5          |   1 0    |     15   |
 ```
必需列为seed,node1_id,node2_id,label。
系统把node1_id，node2_id的并集去重后作为种子集合，利用边表传播k次得到k跳邻居graph_feature，作为新列输出
其他列other1,...，保持原样输出
 ``` 
如果需要融合node1_id和node2_id的子图（例如KCan模型），结果表如下：

| seed    | node1_id | node2_id   | label    |  other1   | graph_feature |
| ------- | -------- | ---------- | -------- | --------- | ------------- |
| l_1_3   | 1        | 3          |   0 1    |     23    | 1和3的序列化子图 |
| l_3_5   | 3        | 5          |   1 0    |     15    | 3和5的序列化子图 |
融合后的子图会有2个根节点，顺序为node1_id,node2_id

如果不需要融合node1_id和node2_id的子图（例如CD-GNN模型），结果表如下：

| seed    | node1_id | node2_id   | label    |  other1   | graph_feature | graph_feature_2 |
| ------- | -------- | ---------- | -------- | --------- | ------------- | ------------ |
| l_1_3   | 1        | 3          |   0 1    |     23    | 1的序列化子图   | 3的序列化子图   |
| l_3_5   | 3        | 5          |   1 0    |     15    | 3的序列化子图   | 5的序列化子图   |


## 子图级采样
在ppi等数据里多个节点组成一个独立的小图，为了避免每个节点k跳子图的大量重复采样和计算，
可以从多个节点一并传播k跳并融合为一个graph_feature，在此过程中可以进行去重。

### 子图级采样 - 点表征
针对以节点为单位的图学习模型，即单个节点有一个label。
这类的样本表举例如下：

| node_id|   seed     | label    |  other1  |
| ------ | ---------- | -------- | -------- |
| 1      | g1         |   0 1    |     3    |
| 2      | g2         |   1 0    |     23   |
| 3      | g1         |   1 0    |     9    |
| 4      | g2         |   0 1    |     7    |
| 5      | g1         |   0 1    |     10   |
 ```
必需列为node_id,graph_id,label。
系统把node_id节点为种子集合，利用边表传播k次得到k跳邻居，最后按graph_id融合为一个graph_feature，作为新列输出
label和其他列other1,...，按graph_id组合为列表，同时保持了和node_id顺序的对应关系
 ``` 
结果表如下：

| node_id   |   seed     | label            |  other1    | graph_feature | 
| --------- | ---------- | ---------------- | ---------- | ------------ |
| 1 3 5     | g1         | [0 1, 1 0, 0 1]  | [3, 9, 10] | g1的序列化子图 |
| 2 4       | g2         | [1 0, 0 1]       | [23, 7]    | g2的序列化子图 |


### 子图级采样 - 图表征
针对一些以子图为单位的图学习模型，一张小图有一个label。
这类的样本表举例如下：

| node_id    |   seed     | label    |  other_feature  |
| ---------- | ---------- | -------- | --------------- |
| 1 3 5      | g1         |   0 1    |        23       |
| 2 4 6      | g2         |   1 0    |        15       |
 ```
必需列为node_id,graph_id,label。
系统把node_id里的每个节点为种子集合，利用边表传播k次得到k跳邻居，并按graph_id融合为一个graph_feature，作为新列输出
其他列other1,...，保持原样输出
 ``` 
结果表如下：

| node_id   |   seed  | label  |  other_feature  | graph_feature |
| --------- | ------- | ------ | --------------- | ------------ |
| 1 3 5     | g1      |   0 1  |        23       | g1的序列化子图 |
| 2 4 6     | g2      |   1 0  |        15       | g2的序列化子图 |
