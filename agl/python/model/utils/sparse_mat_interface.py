import torch


class GraphSparseMatInterface(object):

    @staticmethod
    def convert_sparse_mat_to_tensor(X):
        """
        Convert scipy sparse matrix to torch sparse tensor.
        Args:
            X: scipy sparse matrix of graph adjacency matrix.
        """
        coo = X.tocoo()
        i = torch.LongTensor([coo.row, coo.col])
        v = torch.from_numpy(coo.data).float()
        return torch.sparse.FloatTensor(i, v, coo.shape).coalesce()

    @staticmethod
    def normalize_graph_mat(adj_mat: torch.sparse.FloatTensor,
                            add_virtual_node_num: int = 0,
                            return_node_degree: bool = False,
                            norm: str = 'both'):
        """
        Construct norm adjacency matrix for raw graph adjacency matrix.

        Args:
            adj_mat: sparse tensor of graph adjacency matrix.
            add_virtual_node_num: number of virtual nodes to be added.
            return_node_degree: If set to True, returns node degrees.
            norm: using both / left norm to construct norm_adj_mat (output) for adj_mat.
        """
        shape = adj_mat.shape
        if adj_mat._nnz() == 0:
            rowsum = torch.zeros(adj_mat.shape[0],
                                 device=adj_mat.device) + add_virtual_node_num
        else:
            rowsum = torch.sparse.sum(adj_mat,
                                      dim=0).to_dense() + add_virtual_node_num

        if norm == 'both' and shape[0] == shape[1]:
            # 1 / sqrt(d1 * d2), where d1 and d2 is the in/out-degree of current node.
            d_inv = rowsum**-0.5
            d_inv[torch.isinf(d_inv)] = 0.

            d_mat_inv = torch.sparse.FloatTensor(
                torch.arange(len(d_inv), device=adj_mat.device).repeat(2, 1),
                d_inv)
            norm_adj_tmp = torch.sparse.mm(d_mat_inv, adj_mat)
            norm_adj_mat = torch.sparse.mm(norm_adj_tmp, d_mat_inv)
        elif norm == 'left':
            # 1 / d1, where d1 is the in-degree of current node.
            d_inv = rowsum**-1.0
            d_inv[torch.isinf(d_inv)] = 0.

            d_mat_inv = torch.sparse.FloatTensor(
                torch.arange(len(d_inv), device=adj_mat.device).repeat(2, 1),
                d_inv)
            norm_adj_mat = torch.sparse.mm(d_mat_inv, adj_mat)
        else:
            raise 'norm need in (both, left)'

        output = norm_adj_mat.coalesce()
        if return_node_degree:
            output = output, rowsum
        return output
