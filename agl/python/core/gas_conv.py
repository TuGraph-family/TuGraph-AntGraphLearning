import torch


class Impled:
    def __init__(self, impled=False):
        self.impled = impled

    def __call__(self, fn):
        fn.__impled = self.impled
        return fn


class NodeCentricConv(torch.nn.Module):
    def __init__(self, in_dim, hidden_dim):
        super().__init__()
        self.in_dim = in_dim
        self.hidden_dim = hidden_dim

    def gather(self, message, dst_index):
        return message, dst_index

    def scatter(self, src_node_states, src_index):
        return tuple([n[src_index] for n in src_node_states])

    def apply_node(self, dst_node_state, aggr_state):
        pass

    def apply_edge(self, message, edge_state):
        if edge_state:
            return torch.nn.Linear(self.in_dim, self.hidden_dim)(edge_state) + message
        else:
            return message

    @Impled(impled=False)
    def scatter_and_gather(self, src_node_states, src_index, dst_index, edge_states):
        pass

    def forward(self, src_node_states, dst_node_states, dst_index, src_index, edge_states):

        def default_scatter_and_gather():
            message = self.scatter(src_node_states, src_index)
            message = self.apply_edge(message, edge_states)
            aggr_state = self.gather(message, dst_index)
            return aggr_state

        if not self.training or not self.scatter_and_gather.__impled:
            aggr_state = default_scatter_and_gather()
            return self.apply_node(dst_node_states, aggr_state)
        else:
            aggr_state = self.scatter_and_gather(src_node_states, src_index, dst_index, edge_states)
            return self.apply_node(dst_node_states, aggr_state)



class EdgeCentricLayer:
    pass


class SubGraphCentricLayer:
    pass
