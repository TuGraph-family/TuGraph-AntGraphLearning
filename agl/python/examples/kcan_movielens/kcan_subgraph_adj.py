import time
from typing import Dict

import numpy as np
from sklearn import metrics
import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader
from torch_geometric.utils import to_undirected

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.multi_graph_feature_collate import MultiGraphFeatureCollate
from agl.python.data.subgraph.pyg_inputs import (
    TorchSubGraphBatchData,
    TorchEdgeIndex,
    TorchDenseFeature,
)
from agl.python.data.column import AGLRowColumn, AGLMultiDenseColumn
from agl.python.model.encoder.kcan import KCANEncoder
from pyagl.pyagl import AGLDType, DenseFeatureSpec, NodeSpec, EdgeSpec


def delete_root_index(subgraph: TorchSubGraphBatchData):
    num_nodes = torch.sum(subgraph.n_num_per_sample)
    edge_index = subgraph.adjs_t.edge_index
    row, col = edge_index
    idx = row.mul(num_nodes).add(col)
    root_nodes = to_undirected(subgraph.root_index.reshape([-1, 2]).T)
    root_idx = root_nodes[0] * num_nodes + root_nodes[1]
    mask = np.logical_not(np.isin(idx, root_idx))
    edge_index = TorchEdgeIndex.create_from_tensor(
        edge_index[:, mask],
        size=subgraph.adjs_t.size,
        edge_indices=subgraph.adjs_t.edge_indices[mask],
    )
    e_fs = subgraph.e_feats
    for k, v in e_fs.features.items():
        if isinstance(v, TorchDenseFeature):
            e_fs.features[k] = TorchDenseFeature.create(v.x[mask])
    e_num = subgraph.e_num_per_sample
    cumsum_e_num = np.cumsum(e_num.numpy())
    e_num = torch.Tensor(
        [np.sum(i) for i in np.split(mask, cumsum_e_num)[:-1]], device=e_num.device
    ).to(e_num.dtype)
    return TorchSubGraphBatchData.create_from_tensor(
        n_feats=subgraph.n_feats,
        e_feats=e_fs,
        y=subgraph.y,
        adjs_t=edge_index,
        root_index=subgraph.root_index,
        n_num_per_sample=subgraph.n_num_per_sample,
        e_num_per_sample=e_num,
        other_feats=subgraph.other_feats,
        other_raw=subgraph.other_raw,
    )


class KCANMovielensModel(torch.nn.Module):
    def __init__(
        self,
        feats_dims: Dict[str, int],
        hidden_dim: int,
        out_dim: int,
        edge_score_type: str,
        residual: bool,
        k_hops: int,
        c_hops: int,
    ):
        super().__init__()

        # initial layer
        self.node_embed_layer = torch.nn.Embedding(
            feats_dims["node_feature"], hidden_dim, max_norm=5, scale_grad_by_freq=True
        )  # 点表征
        self.edge_embed_layer = torch.nn.Embedding(
            feats_dims["edge_feature"], hidden_dim, max_norm=5, scale_grad_by_freq=True
        )  # 图谱模型中的边表征
        self.edge_embed_w_layer = torch.nn.Embedding(
            feats_dims["edge_feature"], hidden_dim, max_norm=5, scale_grad_by_freq=True
        )  # 图谱中的边空间映射权重

        # encoder layer
        self._encoder = KCANEncoder(
            hidden_dim=hidden_dim,
            out_dim=out_dim,
            edge_score_type=edge_score_type,
            residual=residual,
            k_hops=k_hops,
            c_hops=c_hops,
        )

        # decoder layer
        self._decoder = torch.nn.Linear(2 * hidden_dim, out_dim)

    def forward(self, subgraph):
        x = self.node_embed_layer(
            subgraph.n_feats.features["node_feature"].to_dense().reshape([-1])
        )
        x = F.normalize(x, p=2, dim=-1)
        e_w = self.edge_embed_layer(
            subgraph.e_feats.features["edge_feature"].to_dense().reshape([-1])
        )
        e_w = F.normalize(e_w, p=2, dim=-1)
        e_b = self.edge_embed_w_layer(
            subgraph.e_feats.features["edge_feature"].to_dense().reshape([-1])
        )
        e_b = F.normalize(e_b, p=2, dim=-1)

        embedding = self._encoder(subgraph, x, e_w, e_b)

        embedding = embedding.reshape([embedding.shape[0], -1])
        out = self._decoder(embedding)
        return out


# step 1: 构建dataset
train_file_name = "./data/subgraph_kcan_movielens_train_new.csv"
test_file_name = "./data/subgraph_kcan_movielens_test_new.csv"

# train data set and test data set
# train_data_set = AGLIterableDataset(train_file_name,
#                                      schema=["seed", "node1_id", "node2_id", "label", "train_flag", "graph_feature"],
#                                      batch_size=256)
# test_data_set = AGLIterableDataset(test_file_name,
#                                     schema=["seed", "node1_id", "node2_id", "label", "train_flag", "graph_feature"],
#                                     batch_size=256)
train_data_set = AGLTorchMapBasedDataset(
    train_file_name,
    has_schema=False,
    schema=["seed", "graph_feature", "node1_id", "node2_id", "label", "train_flag"],
    column_sep=",",
)
test_data_set = AGLTorchMapBasedDataset(
    test_file_name,
    schema=["seed", "graph_feature", "node1_id", "node2_id", "label", "train_flag"],
    column_sep=",",
)

# step 2: 构建collate function
# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddDenseSpec(
    "node_feature", DenseFeatureSpec("node_feature", 1, AGLDType.INT64)
)
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddDenseSpec(
    "edge_feature", DenseFeatureSpec("edge_feature", 1, AGLDType.INT64)
)

label_column = AGLMultiDenseColumn(name="label", dim=1, dtype=np.int64)
# root_id_column = AGLMultiDenseColumn(name="roots_id", dim=2, dtype=np.int64, in_sep="\t")
node1_id_column = AGLRowColumn(name="node1_id")
node2_id_column = AGLRowColumn(name="node2_id")
my_collate = MultiGraphFeatureCollate(
    node_spec,
    edge_spec,
    columns=[node1_id_column, node2_id_column, label_column],
    need_node_and_edge_num=True,
    label_name="label",
    hops=2,
    uncompress=True,
    after_transform=delete_root_index,
)

# step 3: 构建 dataloader
# train loader
train_loader = DataLoader(
    dataset=train_data_set,
    collate_fn=my_collate,
    num_workers=3,
    persistent_workers=True,
    shuffle=True,
    batch_size=256,
)
test_loader = DataLoader(
    dataset=test_data_set,
    collate_fn=my_collate,
    num_workers=3,
    persistent_workers=True,
    shuffle=False,
    batch_size=256,
)

# step 4: 模型相关以及训练与测试
model = KCANMovielensModel(
    feats_dims={"node_feature": 188047, "edge_feature": 26},
    hidden_dim=64,
    out_dim=1,
    edge_score_type="transH",
    residual=True,
    k_hops=1,
    c_hops=1,
)
print(model)
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print("in device: ", device)
loss_op = torch.nn.BCEWithLogitsLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.0005)


def train():
    model.to(device)
    model.train()

    total_loss = 0
    i = 0
    t1 = time.time()
    for j, data in enumerate(train_loader):
        data = data.to(device)
        optimizer.zero_grad()
        preds = model(data)
        loss = loss_op(preds, data.y.to(torch.float32))
        total_loss += loss.item()
        i = i + 1
        loss.backward()
        optimizer.step()
        t2 = time.time()
        if j % 100 == 0:
            print(f"batch {j}, loss:{loss.item()}, time_cost:{t2 - t1}")
    return total_loss / i


def test(loader):
    model.eval()

    total_micro_f1 = 0
    i = 0
    ys, preds = [], []
    for data in loader:
        with torch.no_grad():
            data_gpu = data.to(device)  # 只有第一层的 device 信息是ok的
            out = model(data_gpu)
        pred = out.float().cpu().numpy()
        preds.extend(pred)
        ys.extend(data.y.cpu().numpy())
    auc = metrics.roc_auc_score(ys, preds)
    return auc


best_auc = 0.0
for epoch in range(1, 101):
    t0 = time.time()
    loss = train()
    t1 = time.time()
    auc = test(test_loader)
    if auc > best_auc:
        best_auc = auc
    t2 = time.time()
    print(
        "Epoch: {:02d}, Loss: {:.4f}, auc: {:.4f}, best_auc: {:.4f}, train_time: {:4f}, val_time: {:4f}".format(
            epoch, loss, auc, best_auc, t1 - t0, t2 - t1
        )
    )
