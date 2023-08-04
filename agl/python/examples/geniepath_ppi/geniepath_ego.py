import os
import numpy as np
import torch
import time
from sklearn import metrics
from torch.utils.data import DataLoader

from agl.python.dataset.dataset import PPITorchDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn
from pyagl.pyagl import (
    AGLDType,
    SparseKVSpec,
    NodeSpec,
    EdgeSpec,
)
from agl.python.model.encoder import GeniepathLazyEncoder

train_file_name = "ppi_hop2_limit50_real_0526_train.txt"
test_file_name = "ppi_hop2_limit50_real_0526_test.txt"

script_dir = os.path.dirname(os.path.abspath(__file__))
train_file_name = os.path.join(script_dir, train_file_name)
test_file_name = os.path.join(script_dir, test_file_name)

# train data set and test data set
train_data_set = PPITorchDataset(
    train_file_name,
    True,
    script_dir,
    processed_file_suffix="hop2_limit50_real_0526_train",
    has_schema=False,
)
test_data_set = PPITorchDataset(
    test_file_name,
    True,
    script_dir,
    processed_file_suffix="hop2_limit50_real_0526_test",
    has_schema=False,
)

# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddSparseKVSpec(
    "sparse_kv", SparseKVSpec("sparse_kv", 50, AGLDType.INT64, AGLDType.FLOAT)
)
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddSparseKVSpec(
    "sparse_kv", SparseKVSpec("sparse_kv", 1, AGLDType.INT64, AGLDType.FLOAT)
)

label_column = AGLDenseColumn(name="label", dim=121, dtype=np.int64, sep=" ")
id_column = AGLRowColumn(name="id")
my_collate = AGLHomoCollateForPyG(
    node_spec,
    edge_spec,
    columns=[label_column, id_column],
    ego_edge_index=True,
    uncompress=False,
)

# train loader
train_loader = DataLoader(
    dataset=train_data_set,
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

model = GeniepathLazyEncoder(
    feats_dims={"sparse_kv": 50}, hidden_dim=256, out_dim=121, n_hops=2, residual=True
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
        t1 = time.time()
        data = data.to(device)
        optimizer.zero_grad()
        loss = loss_op(model(data)[data.root_index], data.y.to(torch.float32))
        total_loss += loss.item()
        i = i + 1
        loss.backward()
        optimizer.step()
        t2 = time.time()
        print(f"batch {j}, loss:{loss}, time_cost:{t2 - t1}")
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

    final_y, final_pred = torch.cat(ys, dim=0).numpy(), torch.cat(preds, dim=0).numpy()
    micro_f1 = metrics.f1_score(final_y, final_pred, average="micro")

    return micro_f1


for epoch in range(1, 101):
    loss = train()
    t_f1 = test(test_loader)
    print("Epoch: {:02d}, Loss: {:.4f}, micro_f1: {:.4f}".format(epoch, loss, t_f1))
