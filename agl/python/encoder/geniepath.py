from typing import List, Tuple, Optional, Union, Dict

import torch
from torch import Tensor
from torch_geometric.nn import GATConv
from torch_geometric.typing import OptPairTensor

from agl.python.subgraph.pyg_inputs import TorchBatchOfEgoGraph


class Breadth(torch.nn.Module):
    def __init__(self, in_dim: int, out_dim: int, heads: int = 1):
        super().__init__()
        assert out_dim % heads == 0 and heads > 0
        self.conv = GATConv(in_dim, out_dim // heads, heads=heads)

    def forward(self, x: Union[Tensor, OptPairTensor], edge_index: Tensor, size=Optional[Tuple[int, int]]):
        return torch.tanh(self.conv(x, edge_index, size=size))


class Depth(torch.nn.Module):
    def __init__(self, in_dim, hidden):
        super().__init__()
        self.lstm = torch.nn.LSTM(in_dim, hidden, 1, bias=False)

    def forward(self, x, h, c):
        x, (h, c) = self.lstm(x, (h, c))
        return x, (h, c)


class GeniePathLayer(torch.nn.Module):
    def __init__(self, in_dim, hidden_dim):
        super().__init__()
        self.breadth = Breadth(in_dim, hidden_dim)
        self.depth = Depth(hidden_dim, hidden_dim)

    def forward(self, x, edge_index, h, c, size=None):
        x = self.breadth(x, edge_index, size)
        x = x[None, :]
        x, (h, c) = self.depth(x, h[:, :x.shape[1]], c[:, :x.shape[1]])
        return x[0], (h, c)


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

    def forward(self, subgraph: TorchBatchOfEgoGraph):
        x = torch.cat([self.in_lins[k](v) for k, v in subgraph.n_feats.features.items()], dim=1)
        if subgraph.n_feats.subgraph_index is not None:
            x = x[subgraph.n_feats.subgraph_index]

        h = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
        c = torch.zeros(1, x.shape[0], self.hidden_dim, device=x.device)
        for adj, conv in zip(subgraph.adjs_t, self.convs):
            x_target = x[:adj.size[0]]
            edge_index = torch.flip(adj.edge_index, [1]).T  # origin edge_index = [dst, src], flip to [src, dst]
            x, (h, c) = conv((x, x_target), edge_index, h, c, adj.size[::-1])
        x = self.out_lin(x)
        return x


class GeniepathLazyEncoder(torch.nn.Module):
    def __init__(self, feats_dims: Dict[str, int], hidden_dim: int, out_dim: int, n_hops: int):
        super().__init__()
        hidden_dim = hidden_dim * len(feats_dims)

        self.hidden_dim = hidden_dim
        self.n_hops = n_hops
        self.in_lins = torch.nn.ModuleDict(
            {name: torch.nn.Linear(in_dim, hidden_dim) for name, in_dim in feats_dims.items()})
        self.breadths = torch.nn.ModuleList([Breadth(hidden_dim, hidden_dim) for _ in range(n_hops)])
        self.depths = torch.nn.ModuleList([Depth(hidden_dim, hidden_dim) for _ in range(n_hops)])
        self.out_lin = torch.nn.Linear(hidden_dim, out_dim)

    def forward(self, subgraph: TorchBatchOfEgoGraph):
        x = torch.cat([self.in_lins[k](v) for k, v in subgraph.n_feats.features.items()], dim=1)
        if subgraph.n_feats.subgraph_index is not None:
            x = x[subgraph.n_feats.subgraph_index]
        final_N = subgraph.adjs_t[-1].size[0]

        h_tmps = []
        for conv, adj in zip(self.breadths, subgraph.adjs_t):
            x_target = x[adj.size[0]]
            edge_index = torch.flip(adj.edge_index, [1]).T
            h_tmps.append(conv((x, x_target), edge_index, adj, adj.size[::-1])[:final_N])

        h = x[None, :final_N]
        c = torch.zeros(1, final_N, self.hidden_dim, x.device)
        x = x[None, :final_N]
        for conv, h_i in zip(self.depths, h_tmps):
            x, (h, c) = conv(h_i[None, :], h, c)
        return self.out_lin(x[0])
