import time
import torch
import os
import numpy as np
from sklearn import metrics
from typing import Dict, Union
from torch.utils.data import DataLoader

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLRowColumn, AGLMultiDenseColumn
from pyagl.pyagl import (
    AGLDType,
    DenseFeatureSpec,
    NodeSpec,
    EdgeSpec,
)
from agl.python.model.layer.linear import AGLLinear
from agl.python.model.layer.initial_embedding_layer import InitialLayer
from agl.python.model.encoder.geniepath_encoder import (
    GeniePathLazyEncoder,
)


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


def main():
    # step 1: 构建dataset
    train_file_name = "data_process/ppi_train.csv"
    test_file_name = "data_process/ppi_test.csv"

    script_dir = os.path.dirname(os.path.abspath(__file__))
    train_file_name = os.path.join(script_dir, train_file_name)
    test_file_name = os.path.join(script_dir, test_file_name)

    train_data_set = AGLTorchMapBasedDataset(
        train_file_name,
        format="csv",
        has_schema=True,
        schema=[
            "seed",
            "graph_feature",
            "node_id_list",
            "label_list",
            "train_flag_list",
        ],
        column_sep=",",
    )
    test_data_set = AGLTorchMapBasedDataset(
        test_file_name,
        format="csv",
        has_schema=True,
        schema=[
            "seed",
            "graph_feature",
            "node_id_list",
            "label_list",
            "train_flag_list",
        ],
        column_sep=",",
    )

    # step 2: 构建collate function
    # node related spec
    node_spec = NodeSpec("default", AGLDType.STR)
    node_spec.AddDenseSpec("feature", DenseFeatureSpec("feature", 50, AGLDType.FLOAT))
    # edge related spec
    edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)

    label_column = AGLMultiDenseColumn(
        name="label_list", dim=121, dtype=np.int64, in_sep=" ", out_sep="\t"
    )
    root_id_column = AGLRowColumn(name="node_id_list")
    graph_id_column = AGLRowColumn(name="seed")
    train_flag_column = AGLRowColumn(name="train_flag_list")
    my_collate = AGLHomoCollateForPyG(
        node_spec,
        edge_spec,
        columns=[label_column, root_id_column, graph_id_column, train_flag_column],
        label_name="label_list",
        ego_edge_index=True,
        hops=3,
        uncompress=True,
    )

    # step 3: 构建 dataloader
    # train loader
    train_loader = DataLoader(
        dataset=train_data_set,
        batch_size=2,
        shuffle=False,
        collate_fn=my_collate,
        num_workers=2,
        persistent_workers=True,
    )

    test_loader = DataLoader(
        dataset=test_data_set,
        batch_size=2,
        shuffle=False,
        collate_fn=my_collate,
        num_workers=1,
        persistent_workers=True,
    )

    # step 4: 模型相关以及训练与测试
    model = GeniePathPPIModel(
        feats_dims={"feature": 50},
        hidden_dim=256,
        out_dim=121,
        n_hops=3,
        residual=True,
    )

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    loss_op = torch.nn.BCEWithLogitsLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.001)

    def train():
        model.to(device)
        model.train()

        total_loss = 0
        i = 0
        for j, data in enumerate(train_loader):
            t1 = time.time()
            data = data.to(device)
            optimizer.zero_grad()
            loss = loss_op(model(data)[data.root_index], data.y.to(torch.float32))
            total_loss += loss.item()
            i = i + 1
            loss.backward()
            optimizer.step()
            t2 = time.time()
        return total_loss / i

    def test(loader):
        model.eval()
        ys, preds = [], []
        for data in loader:
            with torch.no_grad():
                data_gpu = data.to(device)
                out = model(data_gpu)[data_gpu.root_index]
            pred = (out > 0).float().cpu()
            preds.append(pred)
            ys.append(data.y.cpu())

        final_y, final_pred = (
            torch.cat(ys, dim=0).numpy(),
            torch.cat(preds, dim=0).numpy(),
        )
        micro_f1 = metrics.f1_score(final_y, final_pred, average="micro")

        return micro_f1

    for epoch in range(1, 401):
        t0 = time.time()
        loss = train()
        t1 = time.time()
        t_f1 = test(test_loader)
        t2 = time.time()
        print(
            "Epoch: {:02d}, Loss: {:.4f}, micro_f1: {:.4f}, train_time: {:4f}, val_time: {:4f}".format(
                epoch, loss, t_f1, t1 - t0, t2 - t1
            )
        )


if __name__ == "__main__":
    main()
