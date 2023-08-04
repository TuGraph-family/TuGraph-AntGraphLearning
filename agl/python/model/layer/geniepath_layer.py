from typing import Tuple, Optional, Union, Dict

import torch
from torch import Tensor
from torch_geometric.nn import GATConv
from torch_geometric.typing import OptPairTensor


class Breadth(torch.nn.Module):
    def __init__(self, in_dim: int, out_dim: int, heads: int = 1):
        super().__init__()
        assert out_dim % heads == 0 and heads > 0
        self.conv = GATConv(in_dim, out_dim // heads, heads=heads)

    def forward(
        self,
        x: Union[Tensor, OptPairTensor],
        edge_index: Tensor,
        size=Optional[Tuple[int, int]],
    ):
        return torch.tanh(self.conv(x, edge_index, size=size))


class Depth(torch.nn.Module):
    def __init__(self, in_dim, hidden):
        super().__init__()
        self.lstm = torch.nn.LSTM(in_dim, hidden, 1, bias=False)

    def forward(self, x, h, c):
        x, (h, c) = self.lstm(x, (h, c))
        return x, (h, c)


class GeniePathLayer(torch.nn.Module):
    """
    GeniePath: Graph Neural Networks with Adaptive Receptive Paths
    https://arxiv.org/abs/1802.00910
    """

    def __init__(self, in_dim, hidden_dim):
        super().__init__()
        self.breadth = Breadth(in_dim, hidden_dim)
        self.depth = Depth(hidden_dim, hidden_dim)

    def forward(self, x, edge_index, h, c, size=None):
        x = self.breadth(x, edge_index, size)
        x = x[None, :]
        x, (h, c) = self.depth(
            x, h, c
        )  # self.depth(x, h[:, :x.shape[1]], c[:, :x.shape[1]])
        return x[0], (h, c)
