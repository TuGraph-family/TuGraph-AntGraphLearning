from typing import List, Tuple, Optional, Union, Dict
import torch
import torch.nn.functional as F
import torch.nn as nn
from torch import Tensor
from torch_geometric.nn import GCNConv, SAGEConv
from torch_geometric.typing import OptPairTensor
from torch.nn.parameter import Parameter

from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData


class DRGSTEncoder(torch.nn.Module):
    def __init__(self, feats_dim: int, hidden_dim: int, out_dim: int, k_hops: int):
        super().__init__()
        self.feats_dim = feats_dim
        self.hidden_dim = hidden_dim
        self.out_dim = out_dim
        self.k_hops = k_hops
        self.convs = torch.nn.ModuleList()
        self.convs.append(GCNConv(self.feats_dim, self.hidden_dim))
        for _ in range(self.k_hops - 2):
            self.convs.append(GCNConv(self.hidden_dim, self.hidden_dim))
        self.convs.append(GCNConv(self.hidden_dim, self.out_dim))
        self.dropout = nn.Dropout(p=0.5)

    def forward(self, subgraph: TorchSubGraphBatchData, features):
        adj = subgraph.adjs_t.edge_index
        h = features
        for i, conv in enumerate(self.convs):
            h = self.dropout(h)
            h = conv(h, adj)
            if i != self.k_hops - 1:
                h = F.relu(h)
        return h

    def reset_parameters(self):
        for conv in self.convs:
            conv.reset_parameters()

    def reset_dropout(self, droprate):
        self.dropout = nn.Dropout(p=droprate)
