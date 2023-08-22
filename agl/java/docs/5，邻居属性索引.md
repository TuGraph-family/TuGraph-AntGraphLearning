# 邻居属性索引
由于子图采样的邻居一层一层扩散的特性，子图大小随跳数呈指数增长，每个节点的邻居过滤和采样会执行很多次，图越稠密过滤和采样的次数也会越多。
为了避免每次过滤和采样时遍历所有邻居节点，我们开发了基于属性的索引功能，能够显著提升邻居过滤和采样性能。

## 准备邻居属性
为了建立邻居属性索引，用户需要先构建邻居属性，这些属性可以来自边上，也可以来自node2_id点上，比如某个特征里的某一个元素。
用户需要把这单个元素单拎出来作为边表里独立的一列。

比如在下面边表中，把edge_feature里的第一个float作为单独一列rate，
把node2_id在某一张辅助性的全量点的属性表中的一个string元素join过来作为单独一列n2_city。

| node1_id  | node2_id  |   edge_feature  |  rate   | n2_city |
| --------- | --------- | --------------- | ------- | ------- |
| user1     | item1     |   0.0 0.1 0.3   |   0.1   |    BJ   |
| user2     | item1     |   0.3 0.5 0.2   |   0.3   |    SH   |
| user3     | item2     |   0.1 0.4 0.7   |   0.1   |    HZ   |

## 邻居hash索引

### hash索引配置
hash索引的配置格式为hash_index:key_name:key_type。由三元组组成分别为：索引类型是hash，索引属性列名和属性值的类型。

### hash索引例子
对于上面的边表我们可以利用n2_city列建立hash索引，配置为："hash_index:n2_city:String"，这样我们就利用n2_city建立了一个hash索引。

### hash索引实现

## 邻居range索引
range索引的配置格式为range_index:key_name:key_type。由三元组组成分别为：索引类型是range，索引属性列名和属性值的类型。
### range索引配置
对于上面的边表我们可以利用rate列建立range索引，配置为："range_index:n2_city:String"，这样我们就利用n2_city建立了一个range索引。

### range索引实现

## 建立多个索引
我们也可以同时建立多个索引，通过配置"hash_index:n2_city:String;range_index:n2_city:String"，我们建立了一个hash索引和一个range索引，多个索引配置的分隔符为';'。
