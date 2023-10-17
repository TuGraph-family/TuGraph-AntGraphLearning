## 数据读取

AGL 样本构建完成之后，目前数据以 csv 的形式存储，每条样本包含几个 column， 其中一个 column 存储 序列化后的 GraphFeature. 其他 column 可能是样本id, label，样本级别特征等信息。

基于Pytorch, AGL提供两个简单的 Dataset，读取这些csv文件，并构建模型所需的训练/验证/测试集。
假设你构建好的 PPI 训练接名称为 ppi_train.csv:

* [AGLTorchMapBasedDataset](../../../agl/python/dataset/map_based_dataset.py) （map-style dataset）

   ```python
    from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
    train_data_set = AGLTorchMapBasedDataset("/your_path_to/ppi_train.csv")
    print(train_data_set[0]) # 查看第一条数据
   ```

* [AGLIterableDataset](../../../agl/python/dataset/iterable_dataset.py) （iterable-stype dataset）

  和上述 AGLTorchMapBasedDataset 使用方法类似，但需要指定batch_size (dataloader 中不要再设置batch_size)

   ```python
   from agl.python.dataset.iterable_dataset import AGLIterableDataset
   train_data_set = AGLIterableDataset(file="/your_path_to/ppi_train_.csv")
   for data in iterable_dataset:
      print(f"data : {data}")
      break
   ```

不失一般性，在后面的样例中，我们都以AGLTorchMapBasedDataset作为读取数据的方式。

[next 数据解析](learning_step2_parse_data.md)