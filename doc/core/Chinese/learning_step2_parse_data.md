## 数据解析

由于目前开源版本主要从 csv 文件中读取数据，csv缺乏指示 每列column数据格式的能力，读取的时候都按照字符串进行读取。
因此需要把数据解析成模型所需的 tensor, 包括对 GraphFeature 的解析，以及其他 column （如 label 列）的解析。

对于 GraphFeature 列， 我们提供了 [PySubGraph](../../../agl/python/data/subgraph/subgraph.py) 类进行解析，主要解析逻辑由c++实现，反序列化protobuf
string,
然后组装成模型所需的 tensor (通常是一个batch的数据).

对于非 GraphFeature 列，我们提供一些 [Column](../../../agl/python/data/column.py) (AGLDenseColumn, AGLMultiDenseColumn,
AGLRowColumn)
类用于指示数据格式，并进行解析。

目前我们提供两个 Collate, 整合了上述解析方法，用于配合 torch dataloader 进行数据解析 (torch collate_fn
使用参见[link](https://pytorch.org/docs/stable/data.html#working-with-collate-fn))。

* [AGLHomoCollateForPyG](../../../agl/python/data/collate.py)

  AGLHomoCollateForPyG 是一个基础的 collate 数据处理类，主要针对数据中包含一列同构 graph feature
  的数据进行解析。以 PPI 数据为例，PPI 数据中只有一种类型点（名称假设为 "default"），一种类型边，其中点上包含一个 50维的 dense 特征。我们使用以下方式来定义这个图上的配置信息：

  ```python
    # node related spec
    node_spec = NodeSpec("default", AGLDType.STR)
    node_spec.AddDenseSpec("feature", DenseFeatureSpec("feature", 50, AGLDType.FLOAT))
    # edge related spec
    edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
  ```
  Note: 对于异构数据，或者具有多中类型特征的，只需要增加 node_spec 定义以及 feature spec 即可。

  和典型的DNN类似，一条训练样本，可能不仅仅具有 GraphFeature, 可能还有 ids, labels 列。目前典型的PPI训练样本是一个小图，里面有多个带 label 的节点，每个 label 可以看做是一个121维的
  dense
  特征。
  对于节点 ids, labels 可以通过下述方式指定相应的配置信息：
  ```python
  # label_list 列，假设每个节点内通过 " " 分割，不同节点间通过 "\t" 分割
  label_column = AGLMultiDenseColumn(
        name="label_list", dim=121, dtype=np.int64, in_sep=" ", out_sep="\t"
    ) 
  # 带 label节点列，目前是string, 原样返回
  root_id_column = AGLRowColumn(name="node_id_list")

  ```
  把这些配置信息，传入到 collate 中便可以通过串联 dataset, dataloader 把数据转换为所需要的 tensor.

  ```python
  my_collate = AGLHomoCollateForPyG(
        node_spec,
        edge_spec,
        columns=[label_column, root_id_column],
        label_name="label_list",
    )
  train_loader = DataLoader(
        dataset=train_data_set,
        batch_size=2,
        collate_fn=my_collate,
        num_workers=2,
        persistent_workers=True,
    )
  ```
  需要指出的是，collate 解析后，数据会存放在 [TorchEgoBatchData](../../../agl/python/data/subgraph/pyg_inputs.py)
  或者 [TorchSubGraphBatchData](../../../agl/python/data/subgraph/pyg_inputs.py)
  中。主要区别是是否 ego edge index 来表达 邻接矩阵 （TorchEgoBatchData 中，adj 随 hops 的变化而变化，共有hops_num个adj,
  从第一层到最后一层边的数目不断减少，理论上减少不必要的计算。
  通过 collate 中 ego_edge_index 配置）。

* [MultiGraphFeatureCollate](../../../agl/python/data/multi_graph_feature_collate.py)

  在一些link类任务中，或者异构图应用中，可能一条样本中包含多个 GraphFeature。同时，需要对数据进行前处理和后处理。因此我们也提供了这样一个 MultiGraphFeatureCollate 用于处理这样的数据。
  核心能力还是进行数据解析，相应的使用方法参见 [link1](../../../agl/python/data/collate_test.py)
  , [link2](../../../agl/python/examples/hegnn_acm/model_hegnn.py)。

当前提供的功能如果不能满足您的需求，欢迎基于[PySubGraph](../../../agl/python/data/subgraph/subgraph.py)
和 [Column](../../../agl/python/data/column.py) 构建您自己的解析逻辑。

[next 模型构建](learning_step3_model.md)