#    Copyright 2023 AntGroup CO., Ltd.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

import torch
import torch.nn as nn

from agl.python.model.encoder.base import AGLAlgorithm
from agl.python.model.utils.merit_utils import (
    MergeLayer,
    MercerEncode,
    ConvAttnModel,
    LstmAttnModel,
)
from agl.python.data.subgraph.pyg_inputs import (
    TorchFeatures,
    TorchEdgeIndex,
    TorchSubGraphBatchData,
)


class MERITEncoder(AGLAlgorithm):
    def __init__(
        self,
        n_feat,
        e_feat,
        node_dim=None,
        feat_dim=None,
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
        context_type="lstm",
    ):
        super().__init__()
        self.num_layers = num_layers
        self.num_ngh = seq_len
        self.d = d
        self.fourier_basis = fourier_basis
        self.time_dim = self.d * self.fourier_basis
        self.context_type = context_type

        self.n_feat_dim = feat_dim
        self.e_feat_dim = feat_dim
        self.model_dim = node_dim
        self.log_max = log_max
        self.kernel_size = kernel_size

        self.input_node_layer = nn.Linear(feat_dim, self.model_dim, bias=True)
        self.input_edge_layer = nn.Linear(feat_dim, self.model_dim, bias=True)

        if self.context_type == "lstm":
            self.attn_model_list = torch.nn.ModuleList(
                [
                    LstmAttnModel(
                        self.model_dim,
                        self.model_dim,
                        self.time_dim,
                        n_head=n_head,
                        drop_out=drop_out,
                        kernel_size=kernel_size,
                    )
                    for _ in range(num_layers)
                ]
            )
        else:
            self.attn_model_list = torch.nn.ModuleList(
                [
                    ConvAttnModel(
                        self.model_dim,
                        self.model_dim,
                        self.time_dim,
                        n_head=n_head,
                        drop_out=drop_out,
                        kernel_size=kernel_size,
                    )
                    for _ in range(num_layers)
                ]
            )

        self.time_encoder = MercerEncode(
            k=self.fourier_basis, d=self.d, n_feat_dim=self.model_dim
        )

    def tem_conv(
        self,
        node_indices,
        curr_times,
        n_feats: TorchFeatures,
        e_feats: TorchFeatures,
        other_feat,
        node_raw_embed,
        edge_raw_embed,
        adj: TorchEdgeIndex,
        curr_layers,
        node_indices_layer=1,
    ):
        assert curr_layers >= 0

        batch_size = len(node_indices)

        node_ids = n_feats.features["node_id"].to_dense()[node_indices]
        curr_times_th = torch.unsqueeze(curr_times, dim=1)
        node_feat = self.input_node_layer(node_raw_embed(node_ids.view([-1])))
        node_time_embed = self.time_encoder(torch.zeros_like(curr_times_th), node_feat)

        if curr_layers == 0:
            return node_feat
        else:
            node_conv_feat = self.tem_conv(
                node_indices,
                curr_times,
                n_feats,
                e_feats,
                other_feat,
                node_raw_embed,
                edge_raw_embed,
                adj,
                curr_layers - 1,
                node_indices_layer,
            )

            layer_index = node_indices_layer

            ngh_node_indices = other_feat[f"ngh_node_indices_{layer_index}"]
            ngh_node_ids = other_feat[f"ngh_node_ids_{layer_index}"]
            ngh_edge_ids = other_feat[f"ngh_edge_ids_{layer_index}"]
            ngh_edge_times = other_feat[f"ngh_edge_times_{layer_index}"]

            ngh_time_delta = other_feat[f"ngh_time_delta_{layer_index}"]

            # get neighbors' features
            ngh_node_indices_flat = ngh_node_indices.flatten()  # (batch_size, -1)
            ngh_edge_times_flat = torch.reshape(
                ngh_edge_times.flatten(), [-1, 1]
            )  # (batch_size, -1)

            ngh_node_conv_feat = self.tem_conv(
                ngh_node_indices_flat,
                ngh_edge_times_flat,
                n_feats,
                e_feats,
                other_feat,
                node_raw_embed,
                edge_raw_embed,
                adj,
                curr_layers - 1,
                node_indices_layer - 1,
            )

            ngh_node_feat = ngh_node_conv_feat.view(batch_size, self.num_ngh, -1)

            # get edge time features and node features
            ngh_time_embed = self.time_encoder(ngh_time_delta, ngh_node_feat)
            ngh_edge_feat = self.input_edge_layer(edge_raw_embed(ngh_edge_ids))

            # attention aggregation
            mask = ngh_node_indices == 0
            attn_m = self.attn_model_list[curr_layers - 1]

            local, weight = attn_m(
                node_conv_feat,
                node_time_embed,
                ngh_node_feat,
                ngh_time_embed,
                ngh_edge_feat,
                mask,
            )
            return local

    def encode(
        self, subgraph: TorchSubGraphBatchData, node_raw_embed, edge_raw_embed, **kwargs
    ):
        root_idx = subgraph.root_index
        cut_time = subgraph.other_feats["time"]
        batch_size = len(cut_time)
        cut_time = cut_time.repeat(2, 1)
        n_feats = subgraph.n_feats
        e_feats = subgraph.e_feats
        other_feat = subgraph.other_feats
        adj = subgraph.adjs_t
        root_embed = self.tem_conv(
            root_idx,
            cut_time,
            n_feats,
            e_feats,
            other_feat,
            node_raw_embed,
            edge_raw_embed,
            adj,
            self.num_layers,
            self.num_layers - 1,
        )
        src_embed = root_embed.view(batch_size, 2, -1)[:, 0, :]
        dst_embed = root_embed.view(batch_size, 2, -1)[:, 1, :]
        return src_embed, dst_embed

    def forward(self, subgraph: TorchSubGraphBatchData, node_raw_embed, edge_raw_embed):
        src_embed, dst_embed = self.encode(subgraph, node_raw_embed, edge_raw_embed)
        return src_embed, dst_embed


class MERITDecoder(torch.nn.Module):
    def __init__(self, dim1, dim2, dim3, dim4):
        super().__init__()
        self._merge_layer = MergeLayer(dim1, dim2, dim3, dim4)

    def forward(self, x1, x2):
        score = self._merge_layer(x1, x2).squeeze(dim=-1)
        return score.sigmoid()
