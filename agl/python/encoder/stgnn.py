from typing import List, Dict

import torch
from agl.python.subgraph.pyg_inputs import TorchBatchOfEgoGraph
from agl.python.encoder.geniepath import GeniepathLazyEncoder


class STGNN(torch.nn.Module):
    def __init__(self, feats_dims: Dict[str, int], hidden_dim: int, out_dim: int, n_hops: int,
                 seq_len: int, num_heads: int, pooling: str):
        super().__init__()
        self.conv = GeniepathLazyEncoder(feats_dims, hidden_dim, out_dim, n_hops)
        self.temporal_aggr = STGNNAggregator(num_heads, hidden_dim, seq_len, pooling)

    def forward(self, subgraphs: List[TorchBatchOfEgoGraph]):
        node_embeds = [self.conv(subgraph) for subgraph in subgraphs]
        return self.temporal_aggr(node_embeds)


class STGNNAggregator(torch.nn.Module):
    def __init__(self, num_heads: int, hidden_dim: int, seq_len: int, pooling: str):
        super().__init__()
        self.num_heads = num_heads
        self.lstm_convs = [torch.nn.LSTM(hidden_dim, hidden_dim, 1) for _ in range(seq_len)]
        self.transformer = torch.nn.TransformerEncoderLayer(hidden_dim, num_heads, batch_first=True)
        self.pooling = pooling
        assert self.pooling in {"max", "mean", "min", "sum"}

        self.position_embedding = torch.nn.Parameter(torch.Tensor(seq_len, hidden_dim))
        self.reset_parameters()

    def reset_parameters(self):
        torch.nn.init.kaiming_uniform(self.position_embedding)

    def forward(self, node_embed_list: List[torch.Tensor]):
        assert len(node_embed_list) == len(self.grus)
        states = []
        # initial h, c
        h = torch.zeros(1, node_embed_list[0].shape[0], self.hidden_dim, device=node_embed_list[0].device)
        c = torch.zeros(1, node_embed_list[0].shape[0], self.hidden_dim, device=node_embed_list[0].device)
        for i, conv, node_embed in enumerate(zip(self.lstm_convs, node_embed_list)):
            _, (h, c) = conv(node_embed[None, :], (h, c))
            states.append(node_embed)
            states.append(h[0])

        states_sequence = torch.stack(states, dim=0)  # [2L, N, D]
        states_sequence = torch.permute(states_sequence, dims=[1, 0, 2])  # batch_first => [N, 2L, D]
        states_sequence = states_sequence + self.position_embedding  # position encoding
        out = self.transformer(states_sequence)  # out.shape=[N, 2L, D]
        if self.pooling == "max":
            return torch.amax(out, 1)
        elif self.pooling == "mean":
            return torch.mean(out, 1)
        elif self.pooling == "min":
            return torch.amin(out, 1)
        elif self.pooling == "sum":
            return torch.sum(out, 1)
        else:
            raise NotImplementedError("not support")
