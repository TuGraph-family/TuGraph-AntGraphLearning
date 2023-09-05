import torch
import numpy as np

from agl.python.model.encoder.merit_encoder import MERITEncoder, MERITDecoder
from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData


class MERITModel(torch.nn.Module):
    def __init__(
        self,
        n_feat,
        e_feat,
        node_dim=None,
        d=None,
        fourier_basis=None,
        num_layers=3,
        n_head=4,
        null_idx=0,
        num_heads=1,
        drop_out=0.1,
        seq_len=None,
        log_max=None,
        kernel_size=3,
        context_type="conv",
    ):
        super().__init__()

        # initial layer
        self.n_feat_th = torch.nn.Parameter(torch.from_numpy(n_feat.astype(np.float32)))
        self.e_feat_th = torch.nn.Parameter(torch.from_numpy(e_feat.astype(np.float32)))
        self.edge_raw_embed = torch.nn.Embedding.from_pretrained(
            self.e_feat_th, padding_idx=0, freeze=True
        )
        self.node_raw_embed = torch.nn.Embedding.from_pretrained(
            self.n_feat_th, padding_idx=0, freeze=True
        )
        feat_dim = self.n_feat_th.shape[1]

        # encoder layer
        self._encoder = MERITEncoder(
            n_feat,
            e_feat,
            node_dim=node_dim,
            feat_dim=feat_dim,
            num_layers=num_layers,
            seq_len=seq_len,
            n_head=n_head,
            drop_out=drop_out,
            d=d,
            fourier_basis=fourier_basis,
            log_max=log_max,
            kernel_size=kernel_size,
            context_type=context_type,
        )

        # decoder layer
        self._decoder = MERITDecoder(node_dim, node_dim, node_dim, 1)

    def forward(self, subgraph: TorchSubGraphBatchData):
        src_embed, dst_embed = self._encoder(
            subgraph, self.node_raw_embed, self.edge_raw_embed
        )
        score = self._decoder(src_embed, dst_embed)
        return score
