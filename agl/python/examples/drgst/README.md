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

* 数据下载：
    从 https://github.com/tkipf/gcn/tree/master/gcn/data 下载ind.citeseer.开头的数据文件,放在data_process/data/目录下
* 数据预处理与子图采样：
    运行submit.sh进行数据预处理和spark采样,得到训练集测试集验证集
* 模型
    python drgst_citeseer.py
* 效果
```
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
```
