import time
import torch
import os
import numpy as np
from torch.utils.data import DataLoader
from sklearn import metrics

from agl.python.dataset.dataset import PPITorchDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLRowColumn, AGLMultiDenseColumn
from pyagl.pyagl import (
    AGLDType,
    DenseFeatureSpec,
    NodeSpec,
    EdgeSpec,
)
from agl.python.model.encoder import GeniepathLazyEncoder

# step 1: 构建dataset

train_file_name = "ppi_subgraph_merged_0530_train.txt"
test_file_name = "ppi_subgraph_merged_0530_test.txt"

script_dir = os.path.dirname(os.path.abspath(__file__))
train_file_name = os.path.join(script_dir, train_file_name)
test_file_name = os.path.join(script_dir, test_file_name)

# train data set and test data set
train_data_set = PPITorchDataset(
    train_file_name,
    True,
    script_dir,
    processed_file_suffix="subgraph_merged_0530_train",
    has_schema=False,
    schema=["graph_id", "roots_id", "graph_feature", "labels"],
)
test_data_set = PPITorchDataset(
    test_file_name,
    True,
    script_dir,
    processed_file_suffix="subgraph_merged_0530_test",
    has_schema=False,
    schema=["graph_id", "roots_id", "graph_feature", "labels"],
)

# step 2: 构建collate function
# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddDenseSpec(
    "dense_feature", DenseFeatureSpec("dense_feature", 50, AGLDType.FLOAT)
)
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)

label_column = AGLMultiDenseColumn(name="labels", dim=121, dtype=np.int64)
root_id_column = AGLRowColumn(name="roots_id")
graph_id_column = AGLRowColumn(name="graph_id")
my_collate = AGLHomoCollateForPyG(
    node_spec,
    edge_spec,
    columns=[label_column, root_id_column, graph_id_column],
    label_name="labels",
    ego_edge_index=True,
    hops=3,
    uncompress=False,
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
model = GeniepathLazyEncoder(
    feats_dims={"dense_feature": 50},
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
    time_train = 0.0
    tt0 = time.time()
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
        time_train = time_train + t2 - t1
        # print(f"batch {j}, loss:{loss}, time_cost:{t2-t1}")
    tt1 = time.time()
    # print(f" train_time: {time_train:.4f}, epoch_time: {tt1-tt0:.4f}")
    return total_loss / i


def test(loader):
    model.eval()

    total_micro_f1 = 0
    i = 0
    ys, preds = [], []
    time_train = 0.0
    tt0 = time.time()
    for data in loader:
        t1 = time.time()
        ys.append(data.y)
        with torch.no_grad():
            data_gpu = data.to(device)
            out = model(data_gpu)[data_gpu.root_index]
        preds.append((out > 0).float().cpu())
        t2 = time.time()
        time_train = time_train + t2 - t1
    tt1 = time.time()
    final_y, final_pred = torch.cat(ys, dim=0).numpy(), torch.cat(preds, dim=0).numpy()
    micro_f1 = metrics.f1_score(final_y, final_pred, average="micro")
    tt2 = time.time()
    # print(f" val_time: {time_train:.4f}, val_epoch_time: {tt1 - tt0:.4f}, metric_time: {tt2-tt1:.4f}")
    return micro_f1


for epoch in range(1, 401):
    t0 = time.time()
    loss = train()
    t1 = time.time()
    t_f1 = test(test_loader)
    t2 = time.time()
    print(
        "Epoch: {:02d}, Loss: {:.4f}, micro_f1: {:.4f}, train_time:{:.4f}, val_time: {:.4f}".format(
            epoch, loss, t_f1, t1 - t0, t2 - t1
        )
    )
