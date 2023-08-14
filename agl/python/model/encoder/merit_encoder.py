import torch
import numpy as np
import torch.nn as nn
import torch.nn.functional as F

from agl.python.model.encoder.base import AGLAlgorithm
from agl.python.model.utils.merit_utils import MergeLayer,MercerEncode,ConvAttnModel,LstmAttnModel
from agl.python.data.subgraph.pyg_inputs import TorchFeatures,TorchEdgeIndex,TorchSubGraphBatchData


class MERITEncoder(AGLAlgorithm):
    def __init__(self, n_feat, e_feat, node_dim=None, feat_dim=None, d=None, fourier_basis=None, 
                 num_layers=3, n_head=4, null_idx=0, num_heads=1, drop_out=0.1, seq_len=None,
                 log_max=None, kernel_size=3, context_type="lstm"):
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
            self.attn_model_list = torch.nn.ModuleList([LstmAttnModel(
                        self.model_dim,
                        self.model_dim,
                        self.time_dim,
                        n_head=n_head,
                        drop_out=drop_out,
                        kernel_size=kernel_size) for _ in range(num_layers)])
        else:
            self.attn_model_list = torch.nn.ModuleList([ConvAttnModel(
                        self.model_dim,
                        self.model_dim,
                        self.time_dim,
                        n_head=n_head,
                        drop_out=drop_out,
                        kernel_size=kernel_size) for _ in range(num_layers)])

        self.time_encoder = MercerEncode(
                k=self.fourier_basis, d=self.d, n_feat_dim=self.model_dim)

    def get_temporal_neighbor(
            self, node_indices: np.ndarray, n_feats: TorchFeatures, e_feats: TorchFeatures,
            adj: TorchEdgeIndex, num_ngh):
        ngh_node_indices = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_edge_times = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_edge_indices = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_node_ids = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_edge_ids = np.zeros((len(node_indices), num_ngh)).astype(np.int32)

        row_ptr = adj.row_ptr.cpu().numpy()
        col = adj.col.cpu().numpy()
        edge_indices = adj.edge_indices.cpu().numpy()

        for i, node_idx in enumerate(node_indices):
            i_ngh_range = range(row_ptr[node_idx], row_ptr[node_idx+1])
            i_ngh_num = len(i_ngh_range)
            if i_ngh_num >= num_ngh:
                i_ngh_num = num_ngh
            i_ngh_node_indices = col[i_ngh_range][:i_ngh_num]
            i_ngh_edge_indices = edge_indices[i_ngh_range][:i_ngh_num]
            i_ngh_edge_times = e_feats.features['time'].to_dense().cpu().numpy()[i_ngh_edge_indices].reshape([-1])

            # sort neighbors by time
            if len(i_ngh_edge_times) > 0:
                i_ngh_edge_times, i_ngh_node_indices, i_ngh_edge_indices = \
                    list(zip(*sorted(zip(i_ngh_edge_times, i_ngh_node_indices, i_ngh_edge_indices))))
                i_ngh_edge_times = list(i_ngh_edge_times)
                i_ngh_node_indices = list(i_ngh_node_indices)
                i_ngh_edge_indices = list(i_ngh_edge_indices)

                i_ngh_node_ids = n_feats.features['node_id'].to_dense().cpu().numpy()[i_ngh_node_indices].reshape([-1])
                i_ngh_edge_ids = e_feats.features['edge_id'].to_dense().cpu().numpy()[i_ngh_edge_indices].reshape([-1])

                ngh_node_indices[i][:i_ngh_num] = i_ngh_node_indices
                ngh_edge_times[i][:i_ngh_num] = i_ngh_edge_times
                ngh_edge_indices[i][:i_ngh_num] = i_ngh_edge_indices
                ngh_node_ids[i][:i_ngh_num] = i_ngh_node_ids
                ngh_edge_ids[i][:i_ngh_num] = i_ngh_edge_ids
        
        return ngh_node_indices, ngh_node_ids, ngh_edge_ids, ngh_edge_times

    def tem_conv(self, node_indices: np.ndarray, curr_times, 
                 n_feats: TorchFeatures, e_feats: TorchFeatures, 
                 node_raw_embed, edge_raw_embed, adj: TorchEdgeIndex, curr_layers):
        assert (curr_layers >= 0)

        device = adj.row_ptr.device

        batch_size = len(node_indices)

        node_ids = n_feats.features['node_id'].to_dense().cpu().numpy()[node_indices]
        node_ids_th = torch.from_numpy(node_ids).long().to(device)
        curr_times_th = torch.from_numpy(curr_times).float().to(device)

        curr_times_th = torch.unsqueeze(curr_times_th, dim=1)
        node_feat = self.input_node_layer(node_raw_embed(node_ids_th.view([-1])))
        node_time_embed = self.time_encoder(torch.zeros_like(curr_times_th), node_feat)

        if curr_layers == 0:
            return node_feat
        else:
            node_conv_feat = self.tem_conv(
                    node_indices, curr_times, n_feats, e_feats,
                    node_raw_embed, edge_raw_embed, adj, curr_layers-1)
            ngh_node_indices, ngh_node_ids, ngh_edge_ids, ngh_edge_times = \
                self.get_temporal_neighbor(node_indices, n_feats, e_feats, adj, self.num_ngh)

            ngh_node_indices_th = torch.from_numpy(ngh_node_indices).long().to(device)
            ngh_node_ids_th = torch.from_numpy(ngh_node_ids).long().to(device)
            ngh_edge_ids_th = torch.from_numpy(ngh_edge_ids).long().to(device)

            ngh_time_delta = curr_times - ngh_edge_times
            ngh_time_th = torch.from_numpy(ngh_time_delta).float().to(device)

            # get neighbors' features
            ngh_node_indices_flat = ngh_node_indices.flatten()  # (batch_size, -1)
            ngh_edge_times_flat = np.reshape(ngh_edge_times.flatten(), [-1, 1])  # (batch_size, -1)
            ngh_node_conv_feat = self.tem_conv(
                ngh_node_indices_flat, ngh_edge_times_flat, n_feats, e_feats, 
                node_raw_embed, edge_raw_embed, adj, curr_layers-1)
            ngh_node_feat = ngh_node_conv_feat.view(batch_size, self.num_ngh, -1)

            # get edge time features and node features
            ngh_time_embed = self.time_encoder(ngh_time_th, ngh_node_feat)
            ngh_edge_feat = self.input_edge_layer(edge_raw_embed(ngh_edge_ids_th))

            # attention aggregation
            mask = ngh_node_indices_th == 0
            attn_m = self.attn_model_list[curr_layers-1]

            local, weight = attn_m(node_conv_feat,
                                   node_time_embed,
                                   ngh_node_feat,
                                   ngh_time_embed,
                                   ngh_edge_feat,
                                   mask)
            return local

    def encode(self, subgraph: TorchSubGraphBatchData, node_raw_embed, edge_raw_embed, **kwargs):
        root_idx = subgraph.root_index.cpu().numpy()
        cut_time = subgraph.other_feats['time'].cpu().numpy()
        batch_size = len(cut_time)
        cut_time = np.repeat(cut_time, 2, axis=0)
        n_feats = subgraph.n_feats
        e_feats = subgraph.e_feats
        adj = subgraph.adjs_t
        root_embed = self.tem_conv(
                root_idx, cut_time, n_feats, e_feats, node_raw_embed, edge_raw_embed,
                adj, self.num_layers)

        src_embed = root_embed.view(batch_size, 2, -1)[:,0,:]
        dst_embed = root_embed.view(batch_size, 2, -1)[:,1,:]
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
