# GeniePath Demo 说明

## Model

    论文：[GeniePath: Graph Neural Networks with Adaptive Receptive Paths](https://arxiv.org/abs/1802.00910)
    实现参考：[1] <https://github.com/shuowang-ai/GeniePath-pytorch>
            [2] <https://github.com/pyg-team/pytorch_geometric/blob/master/examples/geniepath.py>

## 说明

* 数据：
    * wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/ppi_train_docker.csv
    * wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/ppi_val_docker.csv
    * wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/ppi_test_docker.csv
* python train_geniepath_ppi.py

## Benchmark

* 效果
    * micro-f1 ~0.985 (400 epoch)
