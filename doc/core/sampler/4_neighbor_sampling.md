# 邻居采样
我们实现了目前图学习里主流的采样能力：随机采样，topk采样，加权概率采样。

## 采样配置
采样表达方式sample_cond类似于python函数调用，格式为：
random_method(by=column_name, limit=k, replacement=True/False, reverse=True/False)。

|      配置     |                                       说明                                         |
| ------------- | ---------------------------------------------------------------------------------- |
| random_method |  可选值为random_sample,topk,weighted_sample，依次为随机采样、topk采样和加权概率采样|
| limit         |  每个节点的直接邻居采样个数限制                                                    |
| by            |  根据边表里某一列进行采样，例如根据rate进行加权概率采样,random_sample模式不适用    |
| replacement   |  有放回采样还是无放回采样，topk模式不适用                                          |
| reverse       |  在topk情况下明确正排序还是反排序，只在topk模式适用                                |

### 随机采样
random_method配置为random_sample时为随机采样，从n个候选集中随机采样k个，可以选择有放回或者无放回。

### Topk采样
random_method配置为topk时为Topk采样，根据属性值（by配置项）取最大或者最小的k个邻居
值得一提的是，如果建立索引阶段已经对该属性进行了排序，可以复用排序直接返回topk的邻居。

### 加权概率采样
random_method配置为weighted_sample时为加权概率采样，样本被选中的概率正比于其权重，可以选择有放回或者无放回。
