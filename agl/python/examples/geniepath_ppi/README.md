# GeniePath Demo 说明

## Model

    论文：[GeniePath: Graph Neural Networks with Adaptive Receptive Paths](https://arxiv.org/abs/1802.00910)
    实现参考：[1] <https://github.com/shuowang-ai/GeniePath-pytorch>
            [2] <https://github.com/pyg-team/pytorch_geometric/blob/master/examples/geniepath.py>

## 说明

* 数据下载：
    从 https://github.com/sufeidechabei/PPI-Inductive/tree/master/ppi 下载,放到data_process/ppi/目录下
* 数据预处理与子图采样：
    运行submit.sh进行数据预处理和spark采样,得到训练集测试集验证集
* 模型
    python train_geniepath_ppi.py

## Benchmark

* 效果
    * micro-f1 ~0.985 (400 epoch)
