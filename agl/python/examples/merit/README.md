# Merit Demo 说明

## Model

    论文：[MERIT: Learning Multi-level Representations on Temporal Graphs](https://www.ijcai.org/proceedings/2022/0288.pdf)

## 说明

* 数据下载：
  程序会自动从http://snap.stanford.edu/jodie/wikipedia.csv下载
* 数据预处理与子图采样：
  运行submit.sh进行数据预处理和spark采样,得到训练集测试集验证集

这个样例中包含两种 merit 的模式

* python main.py --model merit --agg_type conv
    * 每条样本包含src节点id，dst节点id，动态子图特征，时间戳，以及label。
    * 使用Convolutional aggregator，对应论文中的MERIT_C模型。
* python main.py --model merit --agg_type lstm
    * 每条样本包含src节点id，dst节点id，动态子图特征，时间戳，以及label。
    * 使用Recursive aggregator，对应论文中的MERIT_R模型。

## Benchmark

* 效果
    * TGAT, AP=0.9680, ACC=0.8945
    * MERIT_C, AP=0.9772, ACC=0.9158
    * MERIT_R, AP=0.9766, ACC=0.9141
