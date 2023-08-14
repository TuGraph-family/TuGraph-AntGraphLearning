# Merit Demo 说明
## Model
    论文：[MERIT: Learning Multi-level Representations on Temporal Graphs](https://www.ijcai.org/proceedings/2022/0288.pdf)

## 说明
这个样例中包含两种 merit 的模式
* python main.py --mode conv
  * 每条样本包含src节点id，dst节点id，动态子图特征，时间戳，以及label。
  * 使用Convolutional aggregator，对应论文中的MERIT_C模型。
* python main.py --mode lstm
  * 每条样本包含src节点id，dst节点id，动态子图特征，时间戳，以及label。
  * 使用Recursive aggregator，对应论文中的MERIT_R模型。

## BenchMark
* 效果
  * TGAT, AP=0.9731, ACC=0.9066
  * MERIT_C, AP=0.9796, ACC=0.9213
  * MERIT_R, AP=0.9852, ACC=0.9354