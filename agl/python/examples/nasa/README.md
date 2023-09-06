# NASA 模型说明

## Model
    论文：[Regularizing graph neural networks via consistency-diversity graph augmentations]<https://ojs.aaai.org/index.php/AAAI/article/view/20307/20066>

```
    @inproceedings{bo2022regularizing,
    title={Regularizing graph neural networks via consistency-diversity graph augmentations},
    author={Bo, Deyu and Hu, BinBin and Wang, Xiao and Zhang, Zhiqiang and Shi, Chuan and Zhou, Jun},
    booktitle={Proceedings of the AAAI Conference on Artificial Intelligence},
    volume={36},
    number={4},
    pages={3913--3921},
    year={2022}
    }
```

## 说明

### 数据预处理
以 `Amazon Photo` 数据集为例子

* 数据下载：
    从 https://github.com/BUPT-GAMMA/NASA/tree/main/dataset 下载photo.npz文件,放到data_process/dataset/目录下
* 数据预处理与子图采样：
    运行submit.sh进行数据预处理和spark采样,得到每条样本的子图
    将得到的csv表 `graph_features.csv`，包含 'seed', 'graph_feature', 'node_id_list', 'label_list', 'flag_list' 三个字段，其中 'flag_list' 字段中以 'train', 'val', 'test' 标记训练集数据、验证集数据和测试集数据。然后，执行下述指令，转换flag_list的格式以便于后续处理：
```
sed -i 's/\btrain\b/0/g' graph_features.csv
sed -i 's/\tval/\t1/g' graph_features.csv
sed -i 's/test/2/g' graph_features.csv
```

### 模型运行
```
python nasa.py
```

## BenchMark
* 效果
  * NASA 经调参，300 epoch, Amazon Photo, F1 ~ 0.92 左右。
