## Building Models

The parsed graph data format, such as the edge index, can be directly connected to some conv layers in PyG. Therefore,
in most cases, you can directly use the conv layers in PyG to build your models. (We are also actively integrating with
DGL.)

At the same time, we have encapsulated some basic conv layers based on practical needs and provided some ready-to-use
algorithms (encoders) based on these layers. You can directly use these algorithms to build your models.

To facilitate reusability and clarify the boundaries between conv layers, algorithms (encoders), and models, we have
established a clear distinction between them：

* conv
    * Represents a single layer of GNN logic, or equivalently, a single GNN layer.
* algorithm (encoder)
    * Represents the logic for multiple layers of GNN. Though some algorithms are not simply the stacking of layers, or
      cannot be achieved through simple layer stacking. The "algorithm" layer mainly used to perform the mapping from
      features
      and structure to embeddings.
    * **Do not** put the logic for initial embeddings in this layer, as the logic for initializing embeddings may
      vary across different datasets.
    * **Do not** put the logic for downstream classification or regression tasks in this layer. For example, mapping
      embeddings to classes. Including such logic can cause issues when reusing the model for other tasks.
* model
    * Defines and calls the InitialLayer to map the raw features to initial embeddings.
    * Defines and calls the encoder layers to map the graph features (structure) to embeddings.
    * Defines and calls the decoder to map embeddings to classes.

Taking GeniePath as an example, we
provide [GeniePathLazyEncoder](../../../agl/python/model/encoder/geniepath_encoder.py). Here is an example of assembling
the [model using GeniePath](../../../agl/python/examples/geniepath_ppi/train_geniepath_ppi.py):

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

**Note:**

* The input to the model's forward function is the object outputted by the collate function, which, based on your
  configuration, is either [TorchSubGraphBatchData](../../../agl/python/data/subgraph/pyg_inputs.py)
  or [TorchEgoBatchData](../../../agl/python/data/subgraph/pyg_inputs.py).
* In AGL, each sample is obtained by propagating from labeled nodes (root ids), and the labels are also in the same
  order as the root nodes. Therefore, when calculating the loss, it is necessary to perform a lookup based on the root
  id index in order to establish a one-to-one correspondence between the model's output information and the labels. In
  other words, during loss calculation, it is required to:
  ```python
  loss_op(model(data)[data.root_index], data.y)
  ```

The subsequent steps of building the training pipeline for connecting the model are similar to regular tasks on PyTorch.
You can refer to the [GeniePath example](../../../agl/python/examples/geniepath_ppi) on PPI for further guidance on
handling this.