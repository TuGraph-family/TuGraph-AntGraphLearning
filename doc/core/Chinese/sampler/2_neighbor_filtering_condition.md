# 邻居过滤
我们发现图学习的算法发展越来越精细化和丰富化，针对节点和边的属性进行过滤采样的需求越来越多，所以AGL支持根据点和边的属性进行邻居过滤功能。
例如节点A和很多节点发生了交易，可以配置neighbor.trade_amount>=100，过滤掉与A交易金额小于100的邻居。
同时提供了索引功能，优化了过滤采样效率，本文主要介绍过滤功能，索引和采样后续会详细介绍。

## 属性格式
过滤依赖的属性的格式为SOURCE.COLUMN，表示SOURCE数据的COLUMN属性。SOURCE取值范围为：

|   SOURCE    |                含义          |
| ----------- | --------------------------- |
|   neighbor  | 当前邻居（不区分第几跳）的某列属性 |
|  neighbor_1 |       第1跳邻居的某列属性       |
|  neighbor_k |       第k跳邻居的某列属性       |
|    seed     |       种子节点的某列属性        |

## 类型过滤条件
基于某列属性的类型的过滤，支持的算子为 in, not in, =, ==, !=。
举几个例子：
  * neighbor.user_level in (黄金，白金)。表明只考虑用户级别为 "黄金"或者"白金"的邻居。
  * neighbor.city == seed.city。表明邻居的city属性必需和种子的相同。
  * neighbor.type != 'merchant'。表明不考虑用户类型为merchant的邻居。 

## 数值范围过滤条件
基于某列属性的范围的过滤，支持的算子为 <, <=, >, >=。
举几个例子：
  * neighbor.trade_amount >= 100。表明不考虑与当前节点的交易金额小于100的邻居。
  * neighbor.time > seed.time。表明邻居的time属性必需大于种子的time，适用于动态图场景。
  * neighbor_2.time > neighbor_1.time。表明第二跳邻居的time必需大于其上一跳的time。

## 逻辑组合多个过滤条件
我们支持类似SQL WHERE的方式表达复合的过滤条件，即用逻辑算子组合多个过滤条件，支持的算子为 AND, OR和()。
举几个例子：
  * neighbor.company = seed.company AND ( neighbor.trade_amount >= 100 OR neighbor.user_level in (黄金，白金))。
  在()的作用下会优先计算OR运算符，再计算AND运算符。
  
## 每跳邻居采样对应各自的过滤条件
用户在进行多跳采样时可以配置一个过滤条件适用于全局，也可以为每跳配置各自的过滤条件，用分号分隔。
  * neighbor_1.time > seed.time; neighbor_2.time > neighbor_1.time。
  表明种子time小于第一跳邻居，同时第一跳邻居的time小于其下一跳邻居的time。
  * neighbor.type = 'user-merchant'; neighbor.type = 'merchant-item'。
  描述了metapath的采样要求为user-merchant-item。
  * neighbor.type in ('user-merchant', 'user-item'); neighbor.type in ('merchant-item', 'item-item') 。
  描述了metapath的采样要求为user-merchant-item或者user-item-item。