import torch
from agl.python.model.layer.geniepath_layer import GeniePathLayer
from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData
from agl.python.model.encoder.geniepath_encoder import GeniePathLazyEncoder


class PaGNNEncoder(torch.nn.Module):
    """
    Paper: Inductive Link Prediction with Interactive Structure Learning on Attributed Graph
            https://2021.ecmlpkdd.org/wp-content/uploads/2021/07/sub_635.pdf
    """

    def __init__(self, node_dim: int, edge_dim: int, hidden_dim: int, n_hops: int):
        """ 
        Args:
            node_dim(int): node feature dimension
            edge_dim(int): edge feature dimension
            hidden_dim(int): dimension of hidden embedding
            n_hops(int): number of gnn layers
        """
        super().__init__()
        self.node_feature_dim = node_dim
        self.edge_feature_dim = edge_dim
        self.embedding_size = hidden_dim
        self.n_hops = n_hops

        self.broadcast_from_source = PaGNNBroadcast(
            node_dim, edge_dim, hidden_dim, n_hops
        )
        self.aggregation_from_source = PaGNNAggregation(
            node_dim, edge_dim, hidden_dim, n_hops
        )

        self.broadcast_from_target = PaGNNBroadcast(
            node_dim, edge_dim, hidden_dim, n_hops
        )
        self.aggregation_from_target = PaGNNAggregation(
            node_dim, edge_dim, hidden_dim, n_hops
        )

    def forward(self, subgraph: TorchSubGraphBatchData, node_feat):
        send_from_source = self.broadcast_from_source(subgraph, node_feat, True)
        target_embd = self.aggregation_from_source(
            subgraph, node_feat, send_from_source, False
        )

        send_from_target = self.broadcast_from_target(subgraph, node_feat, False)
        source_embd = self.aggregation_from_target(
            subgraph, node_feat, send_from_target, True
        )

        embedding = torch.concat([source_embd, target_embd], 1)
        return embedding


class PaGNNBroadcast(torch.nn.Module):
    """
    Broadcast Stage:
        Broadcast nodes to their neighbors through subgraph as to represent the structures(e.g. path counts\common neighbors)
    """

    def __init__(self, node_dim: int, edge_dim: int, hidden_dim: int, n_hops: int):
        """ 
        Args:
            node_dim(int): node feature dimension
            edge_dim(int): edge feature dimension
            hidden_dim(int): dimension of hidden embedding
            n_hops(int): number of gnn layers
        """
        super().__init__()
        self.node_feature_dim = node_dim
        self.edge_feature_dim = edge_dim
        self.embedding_size = hidden_dim
        self.n_hops = n_hops

        self.convs = torch.nn.ModuleList(
            [
                GeniePathLayer(self.embedding_size, self.embedding_size)
                for _ in range(self.n_hops)
            ]
        )

    def get_next_neibs_adj(self, adj, source_nodes, num_nodes, transpose=0):
        """
        Get edges that only containing source nodes and neighbors
        Inputs:
            adj: original adjacency matrix
            source_nodes: nodes need to spread
            num_nodes(int): number of all nodes in subgraph for building adjacency matrix
        Returns:
            next_adj: adjacency matrix which only containing edges between source nodes and their 1-hop neighbors
            next_neibs: set of 1-hop neighbors of source nodes
        """
        source_nodes = torch.reshape(source_nodes, [1, -1])

        indices = torch.cat([source_nodes, source_nodes], 0)
        values = torch.ones(source_nodes.shape[1]).to(source_nodes.device)
        source_diagonal = torch.sparse_coo_tensor(
            indices, values, [num_nodes, num_nodes]
        )

        next_adj = torch.sparse.mm(source_diagonal, adj.to_dense()).to_sparse()

        next_neibs_indices = next_adj.coalesce().indices()
        # avoid not edges in adj
        next_neibs = (
            next_neibs_indices
            if next_neibs_indices.isnan
            else torch.unique(next_neibs_indices[1])
        )

        return next_adj, next_neibs

    def keep_source_embd(self, node_embd, source_nodes, num_nodes):
        """
        Keeping/updating source node's embedding and setting others to default(zero)
        Inputs:
            node_embd: node imbedding
            source_nodes: nodes need to spread
            num_nodes(int): number of all nodes in subgraph for building adjacency matrix
        Return: 
            embeding (shape the same with node_embd)
        """
        source_nodes = torch.reshape(source_nodes, [1, -1])

        indices = torch.cat([source_nodes, source_nodes], 0)
        values = torch.ones(source_nodes.shape[1]).to(node_embd.device)
        source_diagonal = torch.sparse_coo_tensor(
            indices, values, [num_nodes, num_nodes]
        )

        keep_embd = torch.sparse.mm(source_diagonal, node_embd)

        return keep_embd

    def forward(
            self, subgraph: TorchSubGraphBatchData, node_feat, send_from_source: bool
    ):
        """
        Inputs:
            subgraph: TorchSubGraphBatchData
            node_feat: node imbedding
            send_from_source(bool): whether broadcast from source node(Setting Ture) or target node(Setting False)
        Return: 
            embeding: embedding that updating by broadcast stage
        """
        edges_index = subgraph.adjs_t.edge_index
        node_embd = node_feat
        root_nodes = torch.reshape(subgraph.root_index, (-1, 2))
        num_nodes = subgraph.n_num_per_sample.sum()
        device = node_embd.device

        source_nodes = root_nodes[:, 0] if send_from_source else root_nodes[:, 1]
        bi_edges = torch.cat(
            (torch.cat((edges_index[0], edges_index[1]), 0), torch.cat((edges_index[1], edges_index[0]), 0)),
            -1).reshape(2, -1)
        indices = torch.unique(bi_edges, dim=1)
        values = torch.ones(indices.shape[1]).to(device)
        adj = torch.sparse_coo_tensor(indices, values, [num_nodes, num_nodes])

        x = self.keep_source_embd(node_embd, source_nodes, num_nodes).to(device)

        h = torch.zeros(1, x.shape[0], self.embedding_size, device=x.device)
        c = torch.zeros(1, x.shape[0], self.embedding_size, device=x.device)

        for k, i in enumerate(range(self.n_hops)):
            next_adj, next_neibs = self.get_next_neibs_adj(adj, source_nodes, num_nodes)
            next_edge_index = next_adj.coalesce().indices()

            next_embd, (h, c) = self.convs[k](x, next_edge_index, h, c)

            next_embd = next_embd + node_embd
            x.data[next_neibs] = next_embd.data[next_neibs]

            source_nodes = next_neibs

        return x


class PaGNNAggregation(torch.nn.Module):
    """
    Aggregation Stage:
        Aggregation neighbors representations(updating with broadcast stage) through gnn
    """

    def __init__(self, node_dim: int, edge_dim: int, hidden_dim: int, n_hops: int):
        """ 
        Args:
            node_dim(int): node feature dimension
            edge_dim(int): edge feature dimension
            hidden_dim(int): dimension of hidden embedding
            n_hops(int): number of gnn layers
        """
        super().__init__()
        self.node_feature_dim = node_dim
        self.edge_feature_dim = edge_dim
        self.embedding_size = hidden_dim
        self.n_hops = n_hops

        self.convs = GeniePathLazyEncoder(
            self.embedding_size * 2, self.embedding_size, self.n_hops
        )

    def forward(
            self,
            subgraph: TorchSubGraphBatchData,
            node_feat,
            send_embd: torch.Tensor,
            agg_by_source: bool,
    ):
        """
        Inputs:
            subgraph: TorchSubGraphBatchData
            node_feat: node imbedding
            send_embd: embedding from broadcast stage
            agg_by_source(bool): whether aggregate by source node(Setting Ture) or target node(Setting False)
        Return: 
            embeding: embedding that updating by aggregation stage
        """
        node_embd = node_feat
        root_nodes = torch.reshape(subgraph.root_index, (-1, 2))

        source_nodes = root_nodes[:, 0] if agg_by_source else root_nodes[:, 1]
        node_embd = torch.cat([node_embd, send_embd], 1)
        out_embd = self.convs(subgraph, node_embd)

        return out_embd[source_nodes]
