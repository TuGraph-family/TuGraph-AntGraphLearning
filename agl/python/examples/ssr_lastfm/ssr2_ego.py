import os
import time
from typing import List, Dict

import numpy as np
import torch
from torch import Tensor
from sklearn import metrics
from torch.utils.data import DataLoader, TensorDataset

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.multi_graph_feature_collate import MultiGraphFeatureCollate
from agl.python.data.column import AGLMultiDenseColumn
from pyagl.pyagl import AGLDType, DenseFeatureSpec, NodeSpec, EdgeSpec

from agl.python.model.encoder.ssr import SSR2Encoder


def get_features(filename, node_num):
    basename = os.path.basename(filename)
    if os.path.isdir(filename):
        for f in os.listdir(filename):
            if f.endswith("csv"):
                filename = os.path.abspath(filename) + "/" + f
                break
    print("read from ", filename)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    data_filename = os.path.join(script_dir, filename)

    dataset = AGLTorchMapBasedDataset(
        data_filename, has_schema=True, column_sep=",", schema=["node_id", "subgraph"]
    )
    node_spec = NodeSpec("default", AGLDType.STR)
    node_spec.AddDenseSpec("features", DenseFeatureSpec("features", 64, AGLDType.FLOAT))
    edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
    label_column = AGLMultiDenseColumn(name="node_id", dim=1, dtype=np.int64)
    my_collate = MultiGraphFeatureCollate(
        node_spec,
        edge_spec,
        columns=[label_column],
        need_node_and_edge_num=False,
        graph_feature_name="subgraph",
        label_name="node_id",
        hops=0,
        uncompress=False,
        ego_edge_index=False,
    )
    data_loader = DataLoader(dataset=dataset, batch_size=256, collate_fn=my_collate)
    res = np.zeros((node_num, 64), dtype=np.float32)
    n = 0
    for data in data_loader:
        X = data.n_feats.features["features"].x.numpy()
        ids = data.y.numpy().reshape([-1])
        res[ids] = X
        n += 1
    return res


user_feat_filenames = [
    './result/out_node_features_ui',
    './result/out_node_features_uu',
    './result/out_node_features_uiu']
item_feat_filenames = [
    './result/out_node_features_iu',
    './result/out_node_features_iui',
    './result/out_node_features_iuu']
feat_filename = './result/features.txt'
user_num = 2101
item_num = 18746
node_num = user_num + item_num

X = np.zeros((node_num, 64), dtype=np.float32)
with open(feat_filename, "r") as f:
    f.readline()
    for line in f:
        id, x = line.strip().split(",")
        x = list(map(float, x.split(" ")))
        X[int(id)] = x
user_feats = [X[:user_num]]
for f in user_feat_filenames:
    temp_x = get_features(f, node_num)
    user_feats.append(temp_x[:user_num])
item_feats = [X[user_num:]]
for f in item_feat_filenames:
    temp_x = get_features(f, node_num)
    item_feats.append(temp_x[user_num:])

train_filename = "./data_process/subgraph_ssr_lastfm_train.csv"
test_filename = "./data_process/subgraph_ssr_lastfm_test.csv"

f = open(train_filename, "r")
edges = []
ys = []
f.readline()
for line in f:
    l = line.strip().split(",")
    edges.append([int(l[1]), int(l[2])])
    ys.append(int(l[5]))
f.close()
dataset = TensorDataset(torch.LongTensor(edges), torch.LongTensor(ys))
train_loader = DataLoader(
    dataset=dataset,
    batch_size=256,
    shuffle=False,
    num_workers=3,
    persistent_workers=True,
)


class SSR2LastfmModel(torch.nn.Module):
    def __init__(
        self,
        hidden_dim: int,
        neg_sample_size: int = 1,
        tau: float = 0.2,
        reg_infonce: float = 0.01,
        view_nums: int = 4,
    ):
        super().__init__()

        # encoder layer
        self._encoder = SSR2Encoder(
            hidden_dim=hidden_dim,
            neg_sample_size=neg_sample_size,
            tau=tau,
            reg_infonce=reg_infonce,
            view_nums=view_nums,
        )

    def forward(self, user_feats: List[Tensor], item_feats: List[Tensor], mode="train"):
        return self._encoder(user_feats, item_feats, mode)


model = SSR2LastfmModel(hidden_dim=64, neg_sample_size=3, tau=0.5, reg_infonce=0.5)
print(model)
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print("in device: ", device)
loss_op = torch.nn.BCEWithLogitsLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.0001)

user_feats = [torch.Tensor(i).to(device) for i in user_feats]
item_feats = [torch.Tensor(i).to(device) for i in item_feats]


def train():
    model.to(device)
    model.train()

    total_loss = 0
    i = 0
    for data in train_loader:
        t1 = time.time()
        x, y = data
        x = x.to(device)
        y = y.to(device)
        user_feat = [f[x[:, 0]] for f in user_feats]
        item_feat = [f[x[:, 1] - user_num] for f in item_feats]
        user_embed, item_embed, infonce_loss = model(user_feat, item_feat)
        preds = torch.sum(user_embed * item_embed, -1).reshape([-1, 1])
        pred_loss = loss_op(preds, y.reshape([-1, 1]).to(torch.float32))
        loss = pred_loss + infonce_loss
        total_loss += loss.item()
        i = i + 1
        loss.backward()
        optimizer.step()
        t2 = time.time()
        if i % 100 == 0:
            print(
                f"batch {i}, loss:{pred_loss.item()}+{infonce_loss.item()}, time_cost:{t2 - t1}"
            )
    return total_loss / i


f = open(test_filename, "r")
edges = []
ys = []
f.readline()
for line in f:
    l = line.strip().split(",")
    edges.append([int(l[1]), int(l[2])])
    ys.append(int(l[5]))
f.close()
dataset = TensorDataset(torch.LongTensor(edges), torch.LongTensor(ys))
test_loader = DataLoader(
    dataset=dataset,
    batch_size=256,
    shuffle=False,
    num_workers=3,
    persistent_workers=True,
)


def test(loader):
    model.eval()

    i = 0
    ys, preds = [], []
    for data in loader:
        with torch.no_grad():
            x, y = data
            x = x.to(device)
            y = y.to(device)
            user_feat = [f[x[:, 0]] for f in user_feats]
            item_feat = [f[x[:, 1] - user_num] for f in item_feats]
            user_embed, item_embed, _ = model(user_feat, item_feat, "test")
            out = torch.sum(user_embed * item_embed, -1).reshape([-1, 1])
        pred = out.float().cpu().numpy()
        preds.extend(pred)
        ys.extend(y.cpu().numpy())

    auc = metrics.roc_auc_score(ys, preds)

    return auc


best_auc = 0.0
for epoch in range(1, 51):
    t0 = time.time()
    loss = train()
    t1 = time.time()
    auc = test(test_loader)
    t2 = time.time()
    if auc > best_auc:
        best_auc = auc
        torch.save(model, "result/model2.pt")
        torch.save(model(user_feats, None, "test")[0], "result/model2_user_embed.pt")
        torch.save(model(None, item_feats, "test")[1], "result/model2_item_embed.pt")
    print(
        "Epoch: {:02d}, Loss: {:.4f}, auc: {:.4f}, best_auc: {:.4f}, train_time: {:4f}, val_time: {:4f}".format(
            epoch, loss, auc, best_auc, t1 - t0, t2 - t1
        )
    )
