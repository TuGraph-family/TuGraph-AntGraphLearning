import torch
import numpy as np
import torch.nn as nn
from torch import Tensor

from agl.python.model.encoder.tgat_encoder import TGATEncoder,TGATDecoder
from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData


class TGATModel(torch.nn.Module):
    def __init__(self, n_feat, e_feat, num_layers=2, n_head=4, drop_out=0.1, num_ngh=10):
        super().__init__()

        # initial layer
        self.n_feat_th = torch.nn.Parameter(torch.from_numpy(n_feat.astype(np.float32)))
        self.e_feat_th = torch.nn.Parameter(torch.from_numpy(e_feat.astype(np.float32)))
        self.edge_raw_embed = torch.nn.Embedding.from_pretrained(
                self.e_feat_th, padding_idx=0, freeze=True)
        self.node_raw_embed = torch.nn.Embedding.from_pretrained(
                self.n_feat_th, padding_idx=0, freeze=True)
        feat_dim = self.n_feat_th.shape[1]

        # encoder layer
        self._encoder = TGATEncoder(
                n_feat, e_feat, feat_dim=feat_dim, num_layers=num_layers, 
                n_head=n_head, drop_out=drop_out, num_ngh=num_ngh)

        # decoder layer
        self._decoder = TGATDecoder(feat_dim, feat_dim, feat_dim, 1)

    def forward(self, subgraph: TorchSubGraphBatchData):
        src_embed, dst_embed = self._encoder(subgraph, self.node_raw_embed, self.edge_raw_embed)
        score = self._decoder(src_embed, dst_embed)
        return score
