import os
import time
from typing import List, Dict

import torch
import numpy as np
from sklearn import metrics
from torch.utils.data import DataLoader
import torch.nn.functional as F

from agl.python.data.column import AGLDenseColumn, AGLRowColumn
from pyagl.pyagl import (
    AGLDType,
    SparseKVSpec,
    NodeSpec,
    EdgeSpec,
)
from agl.python.model.encoder.hegnn_encoder import HeGNNEncoder
from agl.python.model.layer.linear import AGLLinear
from agl.python.model.layer.initial_embedding_layer import InitialLayer
from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.mutli_graph_feature_collate import MultiGraphFeatureCollate


class SplitGraphFeatures:
    def __init__(self, name: str):
        self._name = name

    def __call__(self, input_dict):
        gfs = input_dict[self._name]
        graph_features = [gf_i.split(";".encode("utf-8")) for gf_i in gfs]
        gf_list = []
        num_mp = len(graph_features[0])
        # print(num_mp)
        for i in range(num_mp):
            cur_list = [gf[i] for gf in graph_features]
            gf_list.append(cur_list)
        input_dict.update({self._name: gf_list})
        return input_dict


class HeGNNModel(torch.nn.Module):
    def __init__(
        self,
        feats_dims: Dict[str, int],
        hidden_dim: int,
        out_dim: int,
        n_hops: int,
        num_mps: int,
    ):
        super().__init__()

        self.num_mps = num_mps

        # initial layer
        self._node_initials = torch.nn.ModuleList(
            [InitialLayer(feats_dims, hidden_dim) for _ in range(num_mps)]
        )

        # encoder layer
        self._encoder = HeGNNEncoder(
            in_dim=hidden_dim,
            hidden_dim=hidden_dim,
            n_hops=n_hops,
            num_mps=num_mps,
        )

        # decoder layer
        self._decoder = AGLLinear(hidden_dim, out_dim)

    def forward(self, subgraphs):
        xs = []
        for i in range(self.num_mps):
            x = self._node_initials[i](
                {k: v.to_dense() for k, v in subgraphs[i].n_feats.features.items()}
            )
            xs.append(x)

        embedding = self._encoder(subgraphs, xs)
        out = self._decoder(embedding)
        return out


train_file_name = "t_xr_acm_train_table_xuanruo_0628.txt"  # "ppi_xr_acm_train_0628.txt"
test_file_name = "t_xr_acm_test_table_xuanruo_0628.txt"  # "ppi_xr_acm_test_0628.txt"
val_file_name = "t_xr_acm_val_table_xuanruo_0628.txt"  # "ppi_xr_acm_val_0628.txt"

script_dir = os.path.dirname(os.path.abspath(__file__))
train_file_name = os.path.join(script_dir, train_file_name)
test_file_name = os.path.join(script_dir, test_file_name)

train_data_set = AGLTorchMapBasedDataset(
    train_file_name,
    format="txt",
    has_schema=False,
    schema=["id", "graph_feature", "label"],
)

val_data_set = AGLTorchMapBasedDataset(
    val_file_name,
    format="txt",
    has_schema=False,
    schema=["id", "graph_feature", "label"],
)

test_data_set = AGLTorchMapBasedDataset(
    test_file_name,
    format="txt",
    has_schema=False,
    schema=["id", "graph_feature", "label"],
)

# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddSparseKVSpec(
    "sparse_kv", SparseKVSpec("sparse_kv", 1902, AGLDType.INT64, AGLDType.FLOAT)
)
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddSparseKVSpec(
    "sparse_kv", SparseKVSpec("sparse_kv", 1, AGLDType.INT64, AGLDType.FLOAT)
)

label_column = AGLDenseColumn(name="label", dim=1, dtype=np.int64, sep=" ")
id_column = AGLRowColumn(name="id")
my_collate = MultiGraphFeatureCollate(
    node_spec,
    edge_spec,
    columns=[label_column, id_column],
    ego_edge_index=True,
    pre_transform=SplitGraphFeatures(name="graph_feature"),
    uncompress=False,
)

# print(label_column)

# train loader
train_loader = DataLoader(
    dataset=train_data_set,
    batch_size=128,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=2,
)

val_loader = DataLoader(
    dataset=val_data_set,
    batch_size=128,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=2,
)

test_loader = DataLoader(
    dataset=test_data_set,
    batch_size=128,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=2,
)

emb_dim = 256
n_class = 3
n_hops = 2
num_mps = 2

model = HeGNNModel(
    feats_dims={"sparse_kv": 1902},
    hidden_dim=emb_dim,
    out_dim=n_class,
    n_hops=n_hops,
    num_mps=num_mps,
)
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
loss_op = torch.nn.BCEWithLogitsLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.0005)


def train():
    model.to(device)
    model.train()

    total_loss = 0
    i = 0
    for j, data in enumerate(train_loader):
        data_device = [x.to(device) for x in data]
        optimizer.zero_grad()

        y = F.one_hot(data[0].y.squeeze(1), n_class).to(device)

        loss = loss_op(model(data_device), y.to(torch.float32))
        total_loss += loss.item()
        i = i + 1
        loss.backward()
        optimizer.step()
    return total_loss / i


def test(loader):
    model.eval()

    total_micro_f1 = 0
    i = 0
    ys, preds = [], []
    for data in loader:
        with torch.no_grad():
            data_gpu = [x.to(device) for x in data]
            out = model(data_gpu)
        pred = torch.argmax(out, dim=1).cpu()
        preds.append(pred)
        ys.append(data[0].y.cpu())

    final_y, final_pred = torch.cat(ys, dim=0).numpy(), torch.cat(preds, dim=0).numpy()
    micro_f1 = metrics.f1_score(final_y, final_pred, average="micro")

    return micro_f1


best_epoch = 0
best_val_f1 = 0.0
best_test_f1 = 0.0

for epoch in range(1, 101):
    t1 = time.time()
    loss = train()
    v_f1 = test(val_loader)
    t_f1 = test(test_loader)
    t2 = time.time()
    print(
        "Epoch: {:02d}, Loss: {:.4f}, val_micro_f1: {:.4f}, test_micro_f1: {:.4f}, time_cost:{:.4f}".format(
            epoch, loss, v_f1, t_f1, t2 - t1
        )
    )

    if v_f1 > best_val_f1:
        best_val_f1 = v_f1
        best_test_f1 = t_f1
        best_epoch = epoch
    print(
        "(Epoch: {:02d}, best_val_micro_f1: {:.4f}, best_test_micro_f1: {:.4f})".format(
            best_epoch, best_val_f1, best_test_f1
        )
    )

print("sucess")
