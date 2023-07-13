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
from agl.python.data.column import AGLDenseColumn, AGLRowColumn
from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray
from agl.python.encoder.geniepath import GeniepathLazyEncoder, GeniepathEncoder
# 
# class Breadth(torch.nn.Module):
#     def __init__(self, in_dim: int, out_dim: int, heads: int = 1):
#         super().__init__()
#         assert out_dim % heads == 0 and heads > 0
#         self.conv = GATConv(in_dim, out_dim // heads, heads=heads)
# 
#     def forward(self, x: Union[Tensor, OptPairTensor], edge_index: Tensor, size=Optional[Tuple[int, int]]):
#         return torch.tanh(self.conv(x, edge_index, size=size))
# 
# 
# class Depth(torch.nn.Module):
#     def __init__(self, in_dim, hidden):
#         super().__init__()
#         self.lstm = torch.nn.LSTM(in_dim, hidden, 1, bias=False)
# 
#     def forward(self, x, h, c):
#         x, (h, c) = self.lstm(x, (h, c))
#         return x, (h, c)
# 
# 
# class GeniePathLayer(torch.nn.Module):
#     def __init__(self, in_dim, hidden_dim):
#         super().__init__()
#         self.breadth = Breadth(in_dim, hidden_dim)
#         self.depth = Depth(hidden_dim, hidden_dim)
# 
#     def forward(self, x, edge_index, h, c, size=None):
#         x = self.breadth(x, edge_index, size)
#         x = x[None, :]
#         x, (h, c) = self.depth(x, h, c)
#         return x[0], (h, c)
# 
# 
# class AGLGeniepathEncoder(torch.nn.Module):
#     def __init__(self, feats_dims: Dict[str, int], hidden_dim: int, out_dim: int, n_hops: int):
#         super().__init__()
#         hidden_dim = hidden_dim * len(feats_dims)
#         self.hidden_dim = hidden_dim
#         self.n_hops = n_hops
#         self.in_lins = torch.nn.ModuleDict(
#             {name: torch.nn.Linear(in_dim, hidden_dim) for name, in_dim in feats_dims.items()})
#         self.convs = torch.nn.ModuleList([GeniePathLayer(hidden_dim, hidden_dim) for _ in range(n_hops)])
#         self.out_lin = torch.nn.Linear(hidden_dim, out_dim)
# 
#     def forward(self, homodata):
#         d_device = homodata.label.device
#         node_x = homodata.agl["node_feature"]["sparse_kv"]["sparse_kv"].to(d_device)
#         x = self.in_lins["sparse_kv"](node_x)
#         # todo no edge_feature now
#         h = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
#         c = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
#         for adj, conv in zip(homodata.edge_index, self.convs):
#             #x_target = x  # [:adj.size[0]]
#             # edge_index = torch.flip(adj.edge_index, [1]).T  # origin edge_index = [dst, src], flip to [src, dst]
#             x, (h, c) = conv(x, adj, h, c)
#         x = self.out_lin(x)
#         return x
# 
# 
# class GeniepathLazyEncoder(torch.nn.Module):
#     def __init__(self, feats_dims: Dict[str, int], hidden_dim: int, out_dim: int, n_hops: int):
#         super().__init__()
#         hidden_dim = hidden_dim * len(feats_dims)
# 
#         self.hidden_dim = hidden_dim
#         self.n_hops = n_hops
#         self.in_lins = torch.nn.ModuleDict(
#             {name: torch.nn.Linear(in_dim, hidden_dim) for name, in_dim in feats_dims.items()})
#         self.breadths = torch.nn.ModuleList([Breadth(hidden_dim, hidden_dim) for _ in range(n_hops)])
#         self.depths = torch.nn.ModuleList([Depth(hidden_dim*2, hidden_dim) for _ in range(n_hops)])
#         self.out_lin = torch.nn.Linear(hidden_dim, out_dim)
# 
#     def forward(self, homodata):
#         # x = torch.cat([self.in_lins[k](v) for k, v in subgraph.n_feats.features.items()], dim=1)
#         # if subgraph.n_feats.subgraph_index is not None:
#         #     x = x[subgraph.n_feats.subgraph_index]
#         # final_N = subgraph.adjs_t[-1].size[0]
#         d_device = homodata.label.device
#         node_x = homodata.agl["node_feature"]["sparse_kv"]["sparse_kv"].to(d_device)
#         x = self.in_lins["sparse_kv"](node_x)
#         h = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
#         c = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
# 
#         h_tmps = []
#         for conv, adj in zip(self.breadths, homodata.edge_index):
#             #x_target = x[adj.size[0]]
#             #edge_index = torch.flip(adj.edge_index, [1]).T
#             #h_tmps.append(conv((x, x_target), edge_index, adj, adj.size[::-1])[:final_N])
#             h_tmps.append(conv(x, adj, None))
# 
#         x = x[None, :]
#         for i, l in enumerate(self.depths):
#             in_cat = torch.cat((h_tmps[i][None, :], x), -1)
#             x, (h, c) = self.depths[i](in_cat, h, c)
#         x = self.out_lin(x[0])
# 
# 
#         # h = x[None, :final_N]
#         # c = torch.zeros(1, final_N, self.hidden_dim, x.device)
#         # x = x[None, :final_N]
#         # for conv, h_i in zip(self.depths, h_tmps):
#         #     x, (h, c) = conv(h_i[None, :], h, c)
#         return x


# test set
# http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/zdl_sync%2Fdata%2Fppi_subgraph_debugx_with_lable_test.txt
# train set
# http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/zdl_sync%2Fdata%2Fppi_subgraph_debugx_with_lable_train.txt
train_file_name = "ppi_subgraph_debugx_with_lable_train.txt"
test_file_name = "ppi_subgraph_debugx_with_lable_test.txt"

train_file_name = "ppi_real_train.txt"
test_file_name = "ppi_real_test.txt"

train_file_name = "subgraph_ppi_3hop_limit100_train.txt"
test_file_name = "subgraph_ppi_3hop_limit100_test.txt"

train_file_name = "ppi_3_hop_100_real_train.txt"
test_file_name = "ppi_3_hop_100_real_test.txt"

train_file_name = "ppi_hop2_limit50_real_0526_train.txt"
test_file_name = "ppi_hop2_limit50_real_0526_test.txt"

script_dir = os.path.dirname(os.path.abspath(__file__))
train_file_name = os.path.join(script_dir, train_file_name)
test_file_name = os.path.join(script_dir, test_file_name)

# train data set and test data set
train_data_set = PPITorchDataset(train_file_name, True, script_dir, processed_file_suffix="hop2_limit50_real_0526_train", has_schema = False)
test_data_set = PPITorchDataset(test_file_name, True, script_dir, processed_file_suffix="hop2_limit50_real_0526_test", has_schema = False)

# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddSparseKVSpec("sparse_kv", SparseKVSpec("sparse_kv", 50, AGLDType.INT64, AGLDType.FLOAT))
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddSparseKVSpec("sparse_kv", SparseKVSpec("sparse_kv", 1, AGLDType.INT64, AGLDType.FLOAT))

label_column = AGLDenseColumn(name="label", dim=121, dtype=np.int64, sep=" ")
id_column = AGLRowColumn(name="id")
my_collate = AGLHomoCollateForPyG(node_spec, edge_spec, columns=[label_column, id_column], ego_edge_index=True, uncompress=False)

# train loader
train_loader = DataLoader(dataset=train_data_set,
                          batch_size=128,
                          shuffle=False,
                          collate_fn=my_collate,
                          num_workers=2)

test_loader = DataLoader(dataset=test_data_set,
                         batch_size=128,
                         shuffle=False,
                         collate_fn=my_collate,
                         num_workers=2)

model = GeniepathLazyEncoder(feats_dims={"sparse_kv":50}, hidden_dim=256, out_dim=121, n_hops=2, residual=True)
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
loss_op = torch.nn.BCEWithLogitsLoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.0005)


import time
def train():
    model.to(device)
    model.train()

    total_loss = 0
    i = 0
    # for data in train_loader:
    print(f"train_data_set len: {train_data_set.len()}")
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
        print(f"batch {j}, loss:{loss}, time_cost:{t2-t1}")
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
    loss = train()
    t_f1 = test(test_loader)
    print('Epoch: {:02d}, Loss: {:.4f}, micro_f1: {:.4f}'.format(epoch, loss, t_f1))
