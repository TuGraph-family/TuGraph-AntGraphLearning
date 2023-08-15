# DRGST说明

## Model

```
论文：[Confidence May Cheat: Self-Training on Graph Neural Networks under Distribution Shift](https://dl.acm.org/doi/abs/10.1145/3485447.3512172)
```

#### Hyperparameter

```
dropout = 0.5
num_forward = 50
threshold = 0.7
beta = 1 / 3
droprate = 0.5
stage = 6
```

## Benchmark

- 数据

训练集: http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/graph_feature_1.csv

验证集: http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/graph_feature_2.csv

测试集: http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/graph_feature_0.csv

无标签数据集: http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/graph_feature_-1.csv

- 效果

In stage 0
test loss:0.9647, test acc:0.7070
In stage 1
test loss:0.8359, test acc:0.7320
In stage 2
test loss:0.8548, test acc:0.7440
In stage 3
test loss:0.9179, test acc:0.7460
In stage 4
test loss:0.9230, test acc:0.7440
In stage 5
test loss:0.9430, test acc:0.7540