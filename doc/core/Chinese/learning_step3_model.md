## 构建模型

解析后的图数据格式(如 edge index), 可以直接对接到 PyG的一些 conv layer 中，因此在多数情况下，可以直接使用使用 PyG 中的 conv 层构建模型。（我们也在积极对接DGL中）

同时，我们基于实际的需求也封装了一些基础的 conv layer, 并基于这些 layer，提供了一些开箱即用的 algorithm (encoder). 您可以直接基于这些 algorithm 构建您的模型。

关于 conv (layer), algorithm (encoder), model 三个层次的边界，以方便复用：

* conv (layer)
    * 一层 GNN 逻辑，或者等价于 一层GNN逻辑
* algorithm (encoder)
    * 多层GNN的逻辑，一些算法不是简单的layer叠加，或者无法通过简单layer叠加，也放在这一层进行处理。主要做特征，结构 到 embedding的映射。
    * **不要**在这一层写 initial embedding的逻辑，在不同数据集上时可能initial embedding 逻辑是不一样的。
    * **不要**把下游分类任务，回归任务的逻辑写在里面，比如把embedding 映射到class上，其他任务复用的时候会造成问题
* model
    * 定义并 call InitialLayer 做 原始特征到 initial embedding的映射
    * 定义并 call encoder层，完成图特征（结构）到embedding的映射
    * 定义并 call decoder，完成embedding到 class 的映射

以 GeniePath 为例，我们提供了 [GeniePathLazyEncoder](../../../agl/python/model/encoder/geniepath_encoder.py),
组装模型的[样例](../../../agl/python/examples/geniepath_ppi/train_geniepath_ppi.py)如下：

  ```python
  class GeniePathPPIModel(torch.nn.Module):
    def __init__(
            self,
            feats_dims: Dict[str, int],
            hidden_dim: int,
            out_dim: int,
            n_hops: int,
            residual: bool,
    ):
        super().__init__()

        # initial layer
        self._node_initial = InitialLayer(feats_dims, hidden_dim)

        # encoder layer
        self._encoder = GeniePathLazyEncoder(
            in_dim=hidden_dim,
            hidden_dim=hidden_dim,
            hops=n_hops,
            residual=residual,
        )

        # decoder layer
        self._decoder = AGLLinear(hidden_dim, out_dim)

    def forward(self, subgraph):
        x = self._node_initial(
            {k: v.get() for k, v in subgraph.n_feats.features.items()}
        )
        embedding = self._encoder(subgraph, x)
        out = self._decoder(embedding)
        return out
  ```

**注意**：

* 模型 forward 的输入便是 collate 输出的对象，根据您的配置是 [TorchSubGraphBatchData](../../../agl/python/data/subgraph/pyg_inputs.py)
  或 [TorchEgoBatchData](../../../agl/python/data/subgraph/pyg_inputs.py)。
* AGL 每条样本是从 带 label 节点（root ids）出发扩散得到的，label 也与 root 节点相对应。因此模型输出的信息，在计算 loss 时，需要根据 root id index 进行 lookup
  才能与 label 对应上，也即计算 loss 时需要：
  ```python
  loss_op(model(data)[data.root_index], data.y)
  ```

接下来串联模型的训练pipeline的工作和正常torch上的任务基本一致，可以参考[PPI上GeniePath的样例](../../../agl/python/examples/geniepath_ppi)进行处理。