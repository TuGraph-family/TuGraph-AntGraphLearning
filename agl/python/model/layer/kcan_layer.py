import torch
import torch.nn.functional as F
from torch_geometric.nn import MessagePassing
from torch_geometric.utils import softmax


def transH_project(x, w):
    w = F.normalize(w, p=2, dim=-1)
    return x - torch.sum(w.mul(x), axis=1).view(-1, 1).mul(w)


def cos_similarity(a, b):
    a = F.normalize(a, p=2, dim=-1)
    b = F.normalize(b, p=2, dim=-1)
    return torch.sum(a.mul(b), axis=1)


class KCANKnowldgeConv(MessagePassing):
    def __init__(
        self,
        in_channels,
        out_channels,
        edge_score_type: str = "transH",
        residual: bool = True,
        activation: str = None,
    ):
        super().__init__(aggr="add")
        self.in_channels = in_channels
        self.out_channels = out_channels
        self.residual = residual
        self.edge_score_type = edge_score_type
        self.lin = torch.nn.Linear(in_channels, out_channels)
        self.dropout = torch.nn.Dropout(p=0.2)
        self.activation = activation
        if edge_score_type == "dnn":
            self.score_att_lin = torch.nn.Linear(3 * in_channels, 1)

    def forward(self, x, w, b, edge_index):
        """paramters:
        x: node embedding
        w: edge space mapping for knowledge transfer
        b: edge space bias for knowledge transfer
        edge_index: edge table
        """
        return self.propagate(edge_index, x=x, w=w, b=b)

    def kg_score(self, x_i, x_j, w, b):
        if self.edge_score_type == "transH":
            att = cos_similarity(transH_project(x_i, w), transH_project(x_j, w) + b)
            return att.reshape([-1, 1])
        elif self.edge_score_type == "dnn":
            att = self.score_att_lin(torch.cat([x_i, x_j, w], -1))
            att = F.leaky_relu(att, 0.2)
            return att

    def message(self, edge_index_i, x_i, x_j, w, b):
        alpha = self.kg_score(x_i, x_j, w, b)
        self.alpha = softmax(alpha, edge_index_i)
        return x_j.mul(self.alpha.reshape(-1, 1))

    def update(self, aggr_out, x):
        aggr_out = self.lin(aggr_out)
        if self.residual:
            aggr_out += x
        if self.activation == "relu":
            aggr_out = F.leaky_relu(aggr_out)
        elif self.activation == "leaky_relu":
            aggr_out = F.leaky_relu(aggr_out, 0.2)
        return aggr_out


class KCANConditionConv(MessagePassing):
    def __init__(
        self, in_channels, out_channels, residual: bool = True, activation: str = None
    ):
        super().__init__(aggr="add")
        self.in_channels = in_channels
        self.out_channels = out_channels
        self.residual = residual
        self.activation = activation
        self.att_lin = torch.nn.Linear(4 * in_channels, 1)
        self.lin = torch.nn.Linear(in_channels, out_channels)
        self.dropout = torch.nn.Dropout(p=0.2)

    def forward(self, x, edge_index, targets, pre_alpha=None):
        """paramters:
        x: node embedding
        edge_index: edge table
        targets: target nodes
        """
        return self.propagate(edge_index, x=x, targets=targets, pre_alpha=pre_alpha)

    def message(self, edge_index_i, x_i, x_j, targets, pre_alpha=None):
        x = torch.cat([targets.reshape([targets.shape[0], -1]), x_i, x_j], -1)
        alpha = self.att_lin(x)
        alpha = F.leaky_relu(alpha, 0.2)
        self.alpha = softmax(alpha, edge_index_i)
        if pre_alpha is not None:
            self.alpha = (self.alpha + pre_alpha) / 2
        return x_j.mul(self.alpha.reshape(-1, 1))

    def update(self, aggr_out, x):
        aggr_out = self.lin(aggr_out)
        if self.residual:
            aggr_out += x
        if self.activation == "relu":
            aggr_out = F.leaky_relu(aggr_out)
        elif self.activation == "leaky_relu":
            aggr_out = F.leaky_relu(aggr_out, 0.2)
        return aggr_out
