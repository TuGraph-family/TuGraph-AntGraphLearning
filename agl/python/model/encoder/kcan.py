from typing import Dict

import torch
import torch.nn.functional as F
from torch_geometric.nn import GATConv

from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData
from agl.python.model.layer.kcan_layer import KCANKnowldgeConv, KCANConditionConv


class KCANEncoder(torch.nn.Module):
    def __init__(
        self,
        hidden_dim: int,
        out_dim: int,
        edge_score_type: str,
        residual: bool,
        k_hops: int,
        c_hops: int,
    ):
        super().__init__()
        self.hidden_dim = hidden_dim
        self.k_hops = k_hops  # knowledge aggregator hops
        self.c_hops = c_hops  # conditional aggregator hops
        self.residual = residual
        assert edge_score_type in ["dnn", "transH", "gat"]
        self.edge_score_type = edge_score_type

        if edge_score_type == "gat":  # baseline
            self.convs = torch.nn.ModuleList(
                GATConv(hidden_dim, hidden_dim) for _ in range(k_hops + c_hops)
            )
        else:
            if k_hops > 0:
                self.k_convs = torch.nn.ModuleList(
                    [
                        KCANKnowldgeConv(
                            hidden_dim,
                            hidden_dim,
                            edge_score_type=edge_score_type,
                            residual=self.residual,
                            activation="leaky_relu",
                        )
                        for _ in range(k_hops)
                    ]
                )
            if c_hops > 0:
                self.c_convs = torch.nn.ModuleList(
                    [
                        KCANConditionConv(
                            hidden_dim,
                            hidden_dim,
                            residual=self.residual,
                            activation="leaky_relu",
                        )
                        for _ in range(c_hops)
                    ]
                )

    def expand_target_to_edge(self, subgraph, target_embeds):
        """expand target embedding with batch_size shape to edge shape"""
        return torch.repeat_interleave(target_embeds, subgraph.e_num_per_sample, dim=0)

    def forward(
        self,
        subgraph: TorchSubGraphBatchData,
        x: torch.Tensor,
        e_w: torch.Tensor,
        e_b: torch.Tensor,
    ):
        alpha = None

        if self.edge_score_type == "gat":
            for i, conv in enumerate(self.convs):
                x = conv(x, subgraph.adjs_t.edge_index)
        else:
            if self.k_hops > 0:
                for i, conv in enumerate(self.k_convs):
                    x = conv(x, e_w, e_b, subgraph.adjs_t.edge_index)
                    alpha = conv.alpha
            target_embeds = x[subgraph.root_index.reshape([-1, 2])]
            target_embeds = self.expand_target_to_edge(subgraph, target_embeds)
            if self.c_hops > 0:
                for i, conv in enumerate(self.c_convs):
                    x = conv(
                        x, subgraph.adjs_t.edge_index, target_embeds, pre_alpha=alpha
                    )
        x = x[subgraph.root_index.reshape([-1, 2])]
        return x
