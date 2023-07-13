from typing import List, Tuple, Optional, Union, Dict

import torch
from torch import Tensor
from torch_geometric.nn import GATConv
from torch_geometric.typing import OptPairTensor
from sklearn import metrics

import os
import numpy as np
from torch.utils.data import DataLoader

from agl.python.data.dataset import PPITorchDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn, AGLMultiDenseColumn
from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray
from agl.python.encoder.geniepath import Breadth, Depth, GeniePathLayer
from agl.python.subgraph.pyg_inputs import TorchSubGraphBatchData


class GeniepathEncoder(torch.nn.Module):
    def __init__(self, feats_dims: Dict[str, int], hidden_dim: int, out_dim: int, n_hops: int):
        super().__init__()
        hidden_dim = hidden_dim * len(feats_dims)

        self.hidden_dim = hidden_dim
        self.n_hops = n_hops
        self.in_lins = torch.nn.ModuleDict(
            {name: torch.nn.Linear(in_dim, hidden_dim) for name, in_dim in feats_dims.items()})
        self.convs = torch.nn.ModuleList([GeniePathLayer(hidden_dim, hidden_dim) for _ in range(n_hops)])
        self.out_lin = torch.nn.Linear(hidden_dim, out_dim)

    def forward(self, subgraph: TorchSubGraphBatchData):
        x = torch.cat([self.in_lins[k](v.to_dense()) for k, v in subgraph.n_feats.features.items()], dim=1)
        # if subgraph.n_feats.subgraph_index is not None:
        #    x = x[subgraph.n_feats.subgraph_index]

        h = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
        c = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
        for i, conv in enumerate(self.convs):
            x, (h, c) = conv(x, subgraph.adjs_t.edge_index, h, c)
        # for adj, conv in zip(subgraph.adjs_t, self.convs):
        #     # x_target = x[:adj.size[0]]
        #     # edge_index = torch.flip(adj.edge_index, [1]).T  # origin edge_index = [dst, src], flip to [src, dst]
        #     # x, (h, c) = conv((x, x_target), edge_index, h, c, adj.size[::-1])
        #     x, (h, c) = conv(x, adj.edge_index, h, c)
        x = self.out_lin(x)
        return x


class GeniepathLazyEncoder(torch.nn.Module):
    def __init__(self, feats_dims: Dict[str, int], hidden_dim: int, out_dim: int, n_hops: int, residual: bool):
        super().__init__()
        hidden_dim = hidden_dim * len(feats_dims)

        self.hidden_dim = hidden_dim
        self.n_hops = n_hops
        self.residual = residual
        self.in_lins = torch.nn.ModuleDict(
            {name: torch.nn.Linear(in_dim, hidden_dim) for name, in_dim in feats_dims.items()})
        self.breadths = torch.nn.ModuleList([Breadth(hidden_dim, hidden_dim) for _ in range(n_hops)])
        self.depths = torch.nn.ModuleList(
            [Depth(hidden_dim * 2, hidden_dim) if self.residual else Depth(hidden_dim, hidden_dim) for _ in range(n_hops)])
        self.out_lin = torch.nn.Linear(hidden_dim, out_dim)

    def forward(self, subgraph: TorchSubGraphBatchData):
        x = torch.cat([self.in_lins[k](v.to_dense()) for k, v in subgraph.n_feats.features.items()], dim=1)
        # if subgraph.n_feats.subgraph_index is not None:
        #    x = x[subgraph.n_feats.subgraph_index]
        # final_N = subgraph.adjs_t[-1].size[0]
        h = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
        c = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)

        h_tmps = []
        for i, conv in enumerate(self.breadths):
            h_tmps.append(conv(x, subgraph.adjs_t.edge_index, None))
        # for conv, adj in zip(self.breadths, subgraph.adjs_t):
        #     # x_target = x[adj.size[0]]
        #     # edge_index = torch.flip(adj.edge_index, [1]).T
        #     # h_tmps.append(conv((x, x_target), edge_index, adj, adj.size[::-1])[:final_N])
        #     h_tmps.append(conv(x, adj.edge_index, None))

        # h = x[None, :final_N]
        # c = torch.zeros(1, final_N, self.hidden_dim, x.device)
        # x = x[None, :final_N]
        x = x[None, :]
        if self.residual:
            for conv, h_i in zip(self.depths, h_tmps):
                in_cat = torch.cat((h_i[None, :], x), -1)
                x, (h, c) = conv(in_cat, h, c)
        else:
            for conv, h_i in zip(self.depths, h_tmps):
                x, (h, c) = conv(h_i[None, :], h, c)
        return self.out_lin(x[0])


# step 1: 构建dataset

train_file_name = "ppi_subgraph_merged_0530_train.txt"
test_file_name = "ppi_subgraph_merged_0530_test.txt"

script_dir = os.path.dirname(os.path.abspath(__file__))
train_file_name = os.path.join(script_dir, train_file_name)
test_file_name = os.path.join(script_dir, test_file_name)

# train data set and test data set
train_data_set = PPITorchDataset(train_file_name, True, script_dir, processed_file_suffix="subgraph_merged_0530_train",
                                 has_schema=False,
                                 schema=["graph_id", "roots_id", "graph_feature", "labels"])
test_data_set = PPITorchDataset(test_file_name, True, script_dir, processed_file_suffix="subgraph_merged_0530_test",
                                has_schema=False,
                                schema=["graph_id", "roots_id", "graph_feature", "labels"])

# step 2: 构建collate function
# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddDenseSpec("dense_feature", DenseFeatureSpec("dense_feature", 50, AGLDType.FLOAT))
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)


label_column = AGLMultiDenseColumn(name="labels", dim=121, dtype=np.int64)
root_id_column = AGLRowColumn(name="roots_id")
graph_id_column = AGLRowColumn(name="graph_id")
my_collate = AGLHomoCollateForPyG(node_spec, edge_spec, columns=[label_column, root_id_column, graph_id_column], 
                                  label_name="labels", uncompress=False)

# step 3: 构建 dataloader
# train loader
train_loader = DataLoader(dataset=train_data_set,
                          batch_size=2,
                          shuffle=False,
                          collate_fn=my_collate,
                          num_workers=2,
                          persistent_workers=True)

test_loader = DataLoader(dataset=test_data_set,
                         batch_size=2,
                         shuffle=False,
                         collate_fn=my_collate,
                         num_workers=1,
                         persistent_workers=True)

# step 4: 模型相关以及训练与测试
model = GeniepathLazyEncoder(feats_dims={"dense_feature":50}, hidden_dim=256, out_dim=121, n_hops=4, residual=True)
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
loss_op = torch.nn.BCEWithLogitsLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.003)

import time
def train():
    model.to(device)
    model.train()

    total_loss = 0
    i = 0
    # for data in train_loader:
    # print(f"train_data_set len: {train_data_set.len()}")
    for j, data in enumerate(train_loader):
        t1 = time.time()
        # todo gpu to device 目前先 hard code， 后面考虑换成 pyg_inputs
        data = data.to(device)  # 只有第一层的 device 信息是ok的
        optimizer.zero_grad()
        loss = loss_op(model(data)[data.root_index], data.y.to(torch.float32))
        total_loss += loss.item()
        i = i+1
        loss.backward()
        optimizer.step()
        t2 = time.time()
        #print(f"batch {j}, loss:{loss}, time_cost:{t2-t1}")
    return total_loss / i


def test(loader):
    model.eval()

    total_micro_f1 = 0
    i = 0
    ys, preds = [], []
    for data in loader:
        with torch.no_grad():
            data_gpu = data.to(device)  # 只有第一层的 device 信息是ok的
            out = model(data_gpu)[data_gpu.root_index]
        pred = (out > 0).float().cpu()
        preds.append(pred)
        ys.append(data.y.cpu())

    final_y, final_pred = torch.cat(ys, dim=0).numpy(), torch.cat(preds, dim=0).numpy()
    micro_f1 = metrics.f1_score(final_y, final_pred, average='micro')

    return micro_f1


for epoch in range(1, 101):
    t0 = time.time()
    loss = train()
    t1 = time.time()
    t_f1 = test(test_loader)
    t2 = time.time()
    print('Epoch: {:02d}, Loss: {:.4f}, micro_f1: {:.4f}, train_time: {:4f}, val_time: {:4f}'.format(epoch, loss, t_f1, t1-t0, t2-t1))
