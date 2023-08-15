from typing import List

import torch
from agl.python.model.encoder.base import AGLAlgorithm
from agl.python.data.subgraph.pyg_inputs import TorchEgoBatchData
from agl.python.model.encoder.geniepath_encoder import (
    GeniePathLazyEncoder,
)


class HeGNNEncoder(AGLAlgorithm):
    def __init__(self, in_dim: int, hidden_dim: int, n_hops: int, num_mps: int):
        """
        refer to:
        <<Cash-out User Detection based on Attributed Heterogeneous Information Network with
        a Hierarchical Attention Mechanism>>
        https://dl.acm.org/doi/pdf/10.1609/aaai.v33i01.3301946

        <<Loan Default Analysis with Multiplex Graph Learning>>
        https://dl.acm.org/doi/10.1145/3340531.3412724

        Args:
            in_dim(int): input dimension
            hidden_dim(int): dimension of hidden embedding
            n_hops(int): number of gnn layers
            num_mps(bool): number of meta-paths
        """

        super().__init__()

        self.num_mps = num_mps

        self.convs = torch.nn.ModuleList(
            [
                GeniePathLazyEncoder(in_dim, hidden_dim, n_hops, True)
                for _ in range(self.num_mps)
            ]
        )

        self.hegnn_aggr = HeGNNAggregator(hidden_dim, hidden_dim)

    def forward(self, subgraphs: List[TorchEgoBatchData], xs: List[torch.Tensor]):
        node_embeds = [
            self.convs[i](subgraphs[i], xs[i])[subgraphs[i].root_index]
            for i in range(self.num_mps)
        ]

        x = self.hegnn_aggr(node_embeds)

        return x


class HeGNNAggregator(torch.nn.Module):
    def __init__(self, in_dim: int, hidden_dim: int):
        """

        Args:
            in_dim(int): input dimension
            hidden_dim(int): dimension of hidden embedding
        """

        super().__init__()
        self.trans_1 = torch.nn.Linear(in_dim, hidden_dim)
        self.act = torch.nn.Tanh()
        self.trans_2 = torch.nn.Linear(hidden_dim, 1)
        self.softmax = torch.nn.Softmax(dim=1)

    def forward(self, node_embed_list: List[torch.Tensor]):
        node_embeds = torch.stack(node_embed_list, 1)

        hiddens = self.act(self.trans_1(node_embeds))

        atts = self.softmax(self.trans_2(hiddens))

        return torch.sum(node_embeds * atts, 1)
