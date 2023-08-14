import torch
import numpy as np
import torch.nn as nn
import torch.nn.functional as F


class MergeLayer(torch.nn.Module):

    def __init__(self, dim1, dim2, dim3, dim4):
        super().__init__()
        self.fc1 = torch.nn.Linear(dim1 + dim2, dim3)
        self.fc2 = torch.nn.Linear(dim3, dim4)
        self.act = torch.nn.ReLU()

        torch.nn.init.xavier_normal_(self.fc1.weight)
        torch.nn.init.xavier_normal_(self.fc2.weight)

    def forward(self, x1, x2):
        x = torch.cat([x1, x2], dim=1)
        h = self.act(self.fc1(x))
        return self.fc2(h)


class ScaledDotProductAttention(torch.nn.Module):
    """ Scaled Dot-Product Attention """

    def __init__(self, temperature, attn_dropout=0.1):
        super().__init__()
        self.temperature = temperature
        self.dropout = torch.nn.Dropout(attn_dropout)
        self.softmax = torch.nn.Softmax(dim=2)

    def forward(self, q, k, v, mask=None):
        attn = torch.bmm(q, k.transpose(1, 2))
        attn = attn / self.temperature

        if mask is not None:
            attn = attn.masked_fill(mask, -1e10)

        attn = self.softmax(attn)  # [n * b, l_q, l_k]
        attn = self.dropout(attn)  # [n * b, l_v, d]

        output = torch.bmm(attn, v)
        return output, attn


class MultiHeadAttention(nn.Module):
    """ Multi-Head Attention module """

    def __init__(self, n_head, d_model, d_k, d_v, dropout=0.1):
        super().__init__()

        self.n_head = n_head
        self.d_k = d_k
        self.d_v = d_v

        self.w_qs = nn.Linear(d_model, n_head * d_k, bias=False)
        self.w_ks = nn.Linear(d_model, n_head * d_k, bias=False)
        self.w_vs = nn.Linear(d_model, n_head * d_v, bias=False)
        nn.init.normal_(self.w_qs.weight, mean=0, std=np.sqrt(2.0 / (d_model + d_k)))
        nn.init.normal_(self.w_ks.weight, mean=0, std=np.sqrt(2.0 / (d_model + d_k)))
        nn.init.normal_(self.w_vs.weight, mean=0, std=np.sqrt(2.0 / (d_model + d_v)))

        self.attention = ScaledDotProductAttention(temperature=np.power(d_k, 0.5), attn_dropout=dropout)
        self.layer_norm = nn.LayerNorm(d_model)

        self.fc = nn.Linear(n_head * d_v, d_model)

        nn.init.xavier_normal_(self.fc.weight)

        self.dropout = nn.Dropout(dropout)

    def forward(self, q, k, v, mask=None):
        d_k, d_v, n_head = self.d_k, self.d_v, self.n_head

        sz_b, len_q, _ = q.size()
        sz_b, len_k, _ = k.size()
        sz_b, len_v, _ = v.size()

        residual = q

        q = self.w_qs(q).view(sz_b, len_q, n_head, d_k)
        k = self.w_ks(k).view(sz_b, len_k, n_head, d_k)
        v = self.w_vs(v).view(sz_b, len_v, n_head, d_v)

        q = q.permute(2, 0, 1, 3).contiguous().view(-1, len_q, d_k)  # (n*b) x lq x dk
        k = k.permute(2, 0, 1, 3).contiguous().view(-1, len_k, d_k)  # (n*b) x lk x dk
        v = v.permute(2, 0, 1, 3).contiguous().view(-1, len_v, d_v)  # (n*b) x lv x dv
        
        mask = mask.repeat(n_head, 1, 1)  # (n*b) x .. x ..
        output, attn = self.attention(q, k, v, mask=mask)

        output = output.view(n_head, sz_b, len_q, d_v)

        output = output.permute(1, 2, 0, 3).contiguous().view(sz_b, len_q, -1)  # b x lq x (n*dv)

        output = self.dropout(self.fc(output))
        output = self.layer_norm(output + residual)

        return output, attn


class TimeEncode(torch.nn.Module):
    def __init__(self, expand_dim, factor=5):
        super(TimeEncode, self).__init__()

        time_dim = expand_dim
        self.factor = factor
        self.basis_freq = torch.nn.Parameter((torch.from_numpy(1 / 10 ** np.linspace(0, 9, time_dim))).float())
        self.phase = torch.nn.Parameter(torch.zeros(time_dim).float())

    def forward(self, ts):
        # ts: [N, L]
        batch_size = ts.size(0)
        seq_len = ts.size(1)

        ts = ts.view(batch_size, seq_len, 1)  # [N, L, 1]
        map_ts = ts * self.basis_freq.view(1, 1, -1)  # [N, L, time_dim]
        map_ts += self.phase.view(1, 1, -1)
        harmonic = torch.cos(map_ts)

        return harmonic


class MercerEncode(torch.nn.Module):
    def __init__(self, k, d, n_feat_dim):
        super(MercerEncode, self).__init__()
        expand_dim = k*d
        self.n_feature_dim = n_feat_dim

        w = 1 / 10 ** np.linspace(0, 9, k)
        basis_freq_np = []
        for i in range(k):
            for j in range(d):
                basis_freq_np.append(j*np.pi*w[i])
        basis_freq_np = np.array(basis_freq_np)

        self.basis_freq = (torch.from_numpy(basis_freq_np)).float()

        self.layer = torch.nn.Linear(1, expand_dim, bias=False)
        self.layer.weight.data.copy_(self.basis_freq.view(-1,1))
        self.layer.weight.requires_grad = False

        self.layer_coef = torch.nn.Linear(self.n_feature_dim, expand_dim, bias=True)

    def forward(self, ts, node_feat=None, isNeighbor=False, root_feat=None):
        out_ts = None
        batch_size = ts.size(0)
        seq_len = ts.size(1)
        ts = ts.view(batch_size, seq_len, 1)

        out_ts = self.layer(ts)
        out_ts = torch.cos(out_ts)
        node_feat = node_feat.view(batch_size, -1, self.n_feature_dim)
        coefficient = self.layer_coef(node_feat)
        out_ts = torch.mul(out_ts, coefficient)
        return out_ts


class AttnModel(torch.nn.Module):
    """Attention based temporal layers
    """

    def __init__(self, feat_dim, edge_dim, time_dim, n_head=2, drop_out=0.1):
        """
        args:
          feat_dim: dim for the node features
          edge_dim: dim for the temporal edge features
          time_dim: dim for the time encoding
          n_head: number of heads in attention
          drop_out: probability of dropping a neural.
        """
        super(AttnModel, self).__init__()

        self.feat_dim = feat_dim
        self.time_dim = time_dim

        self.edge_in_dim = (feat_dim + edge_dim + time_dim)
        self.model_dim = self.edge_in_dim

        self.merger = MergeLayer(self.model_dim, feat_dim, feat_dim, feat_dim)

        assert (self.model_dim % n_head == 0)
        self.multi_head_target = MultiHeadAttention(n_head,
                                                    d_model=self.model_dim,
                                                    d_k=self.model_dim // n_head,
                                                    d_v=self.model_dim // n_head,
                                                    dropout=drop_out)
        print('Using scaled prod attention')

    def forward(self, src, src_t, seq, seq_t, seq_e, mask):
        """"Attention based temporal attention forward pass
        args:
          src: float Tensor of shape [B, D]
          src_t: float Tensor of shape [B, Dt], Dt == D
          seq: float Tensor of shape [B, N, D]
          seq_t: float Tensor of shape [B, N, Dt]
          seq_e: float Tensor of shape [B, N, De], De == D
          mask: boolean Tensor of shape [B, N], where the true value indicate a null value in the sequence.
        returns:
          output, weight
          output: float Tensor of shape [B, D]
          weight: float Tensor of shape [B, N]
        """

        src_ext = torch.unsqueeze(src, dim=1)  # src [B, 1, D]
        src_e_ph = torch.zeros_like(src_ext)
        q = torch.cat([src_ext, src_e_ph, src_t], dim=2)  # [B, 1, D + De + Dt] -> [B, 1, D]
        k = torch.cat([seq, seq_e, seq_t], dim=2)  # [B, 1, D + De + Dt] -> [B, 1, D]

        mask = torch.unsqueeze(mask, dim=2)  # mask [B, N, 1]
        mask = mask.permute([0, 2, 1])  # mask [B, 1, N]

        # target-attention
        output, attn = self.multi_head_target(q=q, k=k, v=k, mask=mask)  # output: [B, 1, D + Dt], attn: [B, 1, N]
        output = output.squeeze()
        attn = attn.squeeze()

        output = self.merger(output, src)
        return output, attn


class ConvAttnModel(torch.nn.Module):
    """Attention based temporal layers with causal convolution
    """

    def __init__(self, feat_dim, edge_dim, time_dim,
                 n_head=2, drop_out=0.1, kernel_size=3):
        """
        args:
          feat_dim: dim for the node features
          edge_dim: dim for the temporal edge features
          time_dim: dim for the time encoding
          attn_mode: choose from 'prod' and 'map'
          n_head: number of heads in attention
          drop_out: probability of dropping a neural.
        """
        super(ConvAttnModel, self).__init__()

        self.feat_dim = feat_dim
        self.time_dim = time_dim

        self.edge_in_dim = (feat_dim + edge_dim + time_dim)
        self.model_dim = self.edge_in_dim
        self.merger = MergeLayer(self.model_dim, feat_dim, feat_dim, feat_dim)

        self.kernel_size = kernel_size
        in_channels = feat_dim + edge_dim
        out_channels = in_channels
        self.conv1d = torch.nn.Conv1d(in_channels, out_channels, kernel_size,
                                      stride=1, padding=0, dilation=1)

        self.multi_head_target = MultiHeadAttention(n_head,
                                                    d_model=self.model_dim,
                                                    d_k=self.model_dim // n_head,
                                                    d_v=self.model_dim // n_head,
                                                    dropout=drop_out)

    def forward(self, src, src_t, seq, seq_t, seq_e, mask):
        """"Attention based temporal attention forward pass
        args:
          src: float Tensor of shape [B, D]
          src_t: float Tensor of shape [B, 1, Dt], Dt == D
          seq: float Tensor of shape [B, N, D]
          seq_t: float Tensor of shape [B, N, Dt]
          seq_e: float Tensor of shape [B, N, De], De == D
          mask: boolean Tensor of shape [B, N], where the true value indicate a null value in the sequence.
        
        returns:
          output, weight

          output: float Tensor of shape [B, D]
          weight: float Tensor of shape [B, N]
        """

        src_ext = torch.unsqueeze(src, dim=1)  # src [B, 1, D]
        src_e_ph = torch.zeros_like(src_ext)
        q = torch.cat([src_ext, src_e_ph, src_t], dim=2)  # [B, 1, D + De + Dt]
        v = torch.cat([seq, seq_e, seq_t], dim=2)  # [B, N, D + De + Dt]
        k = torch.cat([seq, seq_e], dim=2)  # [B, N, D + De]

        # Do causal convolution on keys
        k = k.permute(0,2,1)
        k = F.pad(k, [self.kernel_size-1, 0], mode="constant", value=0)
        conv1d_out = self.conv1d(k).permute(0,2,1)
        k = torch.cat([conv1d_out, seq_t], dim=2)

        mask = torch.unsqueeze(mask, dim=2)  # mask [B, N, 1]
        mask = mask.permute([0, 2, 1])  # mask [B, 1, N]

        # target-attention
        output, attn = self.multi_head_target(q=q, k=k, v=v, mask=mask)  # output: [B, 1, D + Dt], attn: [B, 1, N]
        output = output.squeeze(dim=1)
        attn = attn.squeeze(dim=1)

        output = self.merger(output, src)
        return output, attn


class LstmAttnModel(torch.nn.Module):
    """Attention based temporal layers with LSTM
    """

    def __init__(self, feat_dim, edge_dim, time_dim,
                 n_head=2, drop_out=0.1, kernel_size=3):
        """
        args:
          feat_dim: dim for the node features
          edge_dim: dim for the temporal edge features
          time_dim: dim for the time encoding
          attn_mode: choose from 'prod' and 'map'
          n_head: number of heads in attention
          drop_out: probability of dropping a neural.
        """
        super(LstmAttnModel, self).__init__()

        self.feat_dim = feat_dim
        self.time_dim = time_dim

        self.edge_in_dim = (feat_dim + edge_dim + time_dim)
        self.model_dim = self.edge_in_dim
        self.merger = MergeLayer(self.model_dim, feat_dim, feat_dim, feat_dim)

        self.multi_head_target = MultiHeadAttention(n_head,
                                                    d_model=self.model_dim,
                                                    d_k=self.model_dim // n_head,
                                                    d_v=self.model_dim // n_head,
                                                    dropout=drop_out)

        self.lstm = torch.nn.LSTM(input_size=self.edge_in_dim,
                                  hidden_size=self.edge_in_dim,
                                  num_layers=1,
                                  batch_first=True)

    def forward(self, src, src_t, seq, seq_t, seq_e, mask):
        """"Attention based temporal attention forward pass
        args:
          src: float Tensor of shape [B, D]
          src_t: float Tensor of shape [B, 1, Dt], Dt == D
          seq: float Tensor of shape [B, N, D]
          seq_t: float Tensor of shape [B, N, Dt]
          seq_e: float Tensor of shape [B, N, De], De == D
          mask: boolean Tensor of shape [B, N], where the true value indicate a null value in the sequence.

        returns:
          output, weight

          output: float Tensor of shape [B, D]
          weight: float Tensor of shape [B, N]
        """

        src_ext = torch.unsqueeze(src, dim=1)  # src [B, 1, D]
        src_e_ph = torch.zeros_like(src_ext)
        q = torch.cat([src_ext, src_e_ph, src_t], dim=2)  # [B, 1, D + De + Dt]
        v = torch.cat([seq, seq_e, seq_t], dim=2)  # [B, N, D + De + Dt]

        # Do lstm on keys
        k = torch.cat([seq, seq_e, seq_t], dim=2)
        hn, (_, _) = self.lstm(k)
        k = hn

        mask = torch.unsqueeze(mask, dim=2)  # mask [B, N, 1]
        mask = mask.permute([0, 2, 1])  # mask [B, 1, N]

        # target-attention
        output, attn = self.multi_head_target(q=q, k=k, v=v, mask=mask)  # output: [B, 1, D + Dt], attn: [B, 1, N]
        output = output.squeeze(dim=1)
        attn = attn.squeeze(dim=1)

        output = self.merger(output, src)
        return output, attn
