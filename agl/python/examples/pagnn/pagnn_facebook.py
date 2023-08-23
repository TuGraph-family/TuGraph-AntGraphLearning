import numpy as np
from sklearn import metrics
import torch
from torch.utils.data import DataLoader

from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn, AGLMultiDenseColumn
from pyagl.pyagl import (
    AGLDType,
    DenseFeatureSpec,
    SparseKVSpec,
    SparseKSpec,
    NodeSpec,
    EdgeSpec,
    SubGraph,
    NDArray,
)
from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData
from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.model.encoder.pagnn_encoder import PaGNNEncoder


class PaGNNModel(torch.nn.Module):
    """
    Paper: Inductive Link Prediction with Interactive Structure Learning on Attributed Graph
            https://2021.ecmlpkdd.org/wp-content/uploads/2021/07/sub_635.pdf
    """

    def __init__(
        self,
        node_feat,
        node_dim: int,
        edge_dim: int,
        hidden_dim: int,
        out_dim: int,
        n_hops: int,
    ):
        super().__init__()

        self.node_feature_dim = node_dim
        self.edge_feature_dim = edge_dim
        self.embedding_size = hidden_dim
        self.output_dim = out_dim
        self.n_hops = n_hops

        # Initial Layer
        self.n_feat_th = torch.nn.Parameter(node_feat)
        self._node_initial_ori = torch.nn.Embedding.from_pretrained(
            self.n_feat_th, padding_idx=0, freeze=True
        )
        self._node_init_layer = torch.nn.Linear(
            self.node_feature_dim, self.embedding_size
        )

        # Encoder
        self._encoder = PaGNNEncoder(
            node_dim=self.node_feature_dim,
            edge_dim=self.edge_feature_dim,
            hidden_dim=self.embedding_size,
            n_hops=self.n_hops,
        )

        # Decoder
        self._decoder = torch.nn.Linear(self.embedding_size * 2, self.output_dim)

    def forward(self, subgraph: TorchSubGraphBatchData):
        nodes_id = subgraph.n_feats.features["node_feature"].x.reshape((1, -1))
        ori_node_feat = self._node_initial_ori(nodes_id).squeeze()
        node_feat = self._node_init_layer(ori_node_feat)
        embedding = self._encoder(subgraph, node_feat)
        link_embd = self._decoder(embedding)

        return link_embd


# step 1: dataset define
train_file_name = "./facebook_pagnn_train.csv"
val_file_name = "./facebook_pagnn_val.csv"
test_file_name = "./facebook_pagnn_test.csv"
node_feat_np = np.load("./facebook_nodefeat.npy")

train_data_set = AGLTorchMapBasedDataset(
    train_file_name,
    format="csv",
    has_schema=True,
    column_sep=",",
    schema=["link_id", "graph_feature", "node1_id", "node2_id", "label"],
)

val_data_set = AGLTorchMapBasedDataset(
    val_file_name,
    format="csv",
    has_schema=True,
    column_sep=",",
    schema=["link_id", "graph_feature", "node1_id", "node2_id", "label"],
)
test_data_set = AGLTorchMapBasedDataset(
    test_file_name,
    format="csv",
    has_schema=True,
    column_sep=",",
    schema=["link_id", "graph_feature", "node1_id", "node2_id", "label"],
)

# step 2: collate function
# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddDenseSpec(
    "node_feature", DenseFeatureSpec("node_feature", 1, AGLDType.INT64)
)

# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)

label_column = AGLDenseColumn(name="label", dim=2, dtype=np.int64, sep=" ")
id_column = AGLRowColumn(name="link_id")
my_collate = AGLHomoCollateForPyG(
    node_spec,
    edge_spec,
    columns=[label_column, id_column],
    label_name="label",
    need_node_and_edge_num=True,
)

# step 3: dataloader
# train loader
train_loader = DataLoader(
    dataset=train_data_set,
    batch_size=48,
    shuffle=True,
    collate_fn=my_collate,
    num_workers=4,
)

val_loader = DataLoader(
    dataset=val_data_set,
    batch_size=32,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=2,
)

test_loader = DataLoader(
    dataset=test_data_set,
    batch_size=32,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=2,
)

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# step 4: model training
# Initial node embedding
node_feat = torch.from_numpy(node_feat_np.astype(np.float32)).to(device)

model = PaGNNModel(
    node_feat, node_dim=1283, edge_dim=0, hidden_dim=32, out_dim=2, n_hops=2
)
loss_op = torch.nn.BCEWithLogitsLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.0001)

import time


def train():
    model.to(device)
    model.train()

    total_loss = 0
    i = 0
    for j, data in enumerate(train_loader):
        data = data.to(device)
        optimizer.zero_grad()
        loss = loss_op(model(data), data.y.to(torch.float32))
        total_loss += loss.item()
        i = i + 1
        loss.backward()
        optimizer.step()
    return total_loss / i


def test(loader):
    model.eval()

    ys, preds = [], []
    for data in loader:
        with torch.no_grad():
            data_gpu = data.to(device)
            out = model(data_gpu)
        pred = out[:, 1].cpu()
        preds.append(pred)
        ys.append(data.y[:, 1].cpu())

    final_y, final_pred = torch.cat(ys, dim=0).numpy(), torch.cat(preds, dim=0).numpy()
    auc = metrics.roc_auc_score(final_y, final_pred)
    return auc


log_file = open("./log_file.txt", "a")

best_val_auc, best_test_auc = 0.0, 0.0

for epoch in range(1, 50):
    t0 = time.time()
    loss = train()
    t1 = time.time()
    val_auc = test(val_loader)
    test_auc = test(test_loader)
    if val_auc > best_val_auc:
        best_val_auc = val_auc
        best_test_auc = test_auc
    t2 = time.time()
    res_txt = (
        "Epoch: {:02d}, Loss: {:.4f}, Val_AUC: {:.4f}, "
        "Test_AUC: {:.4f}, Final_AUC: {:.4f}, train_time: {:4f}, val_time: {:4f}".format(
            epoch, loss, val_auc, test_auc, best_test_auc, t1 - t0, t2 - t1
        )
    )
    print(res_txt)
    log_file.write(res_txt + "\n")

print("Final AUC on Test Dataset: {:.4f}".format(best_test_auc))
log_file.write("\n")
log_file.close()
