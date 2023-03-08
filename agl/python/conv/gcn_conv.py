import torch
from torch.nn import Module, Linear
from agl.python.core.gas_conv import NodeCentricConv
from torch_scatter.scatter import scatter


class GCNConv(NodeCentricConv, Module):
    def __init__(self, in_dim, hidden_dim):
        super(Module, self).__init__()
        self.node_conv = Linear(in_features=in_dim, out_features=hidden_dim)

    def gather(self, message, dst_index):
        msg, weight = message
        weighed_msg = weight.view(-1, 1) * msg
        return scatter(weighed_msg, dst_index, reduce="sum")

    def apply_node(self, dst_node_state, aggr_state):
        new_node_state = self.node_conv(dst_node_state)
        return new_node_state + aggr_state

    def apply_edge(self, message, edge_state):
        return message

    def scatter_and_gather(self, src_node_states, src_index, dst_index, edge_states):
        assert edge_states is None
        return








