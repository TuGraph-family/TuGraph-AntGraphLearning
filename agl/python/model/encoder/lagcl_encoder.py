import torch
import math
import dgl
import torch.nn as nn
import torch.nn.functional as F
from torch.nn.parameter import Parameter

from agl.python.model.encoder.base import AGLAlgorithm
from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData
from agl.python.model.utils.sparse_mat_interface import GraphSparseMatInterface
from agl.python.model.utils.spmm_utils import SpecialSpmm


def eliminate_zeros(x):
    indices = x.coalesce().indices()
    values = x.coalesce().values()

    mask = values.nonzero()
    nv = values.index_select(0, mask.view(-1))
    ni = indices.index_select(1, mask.view(-1))
    return torch.sparse_coo_tensor(ni, nv, x.shape, device=x.device)


class Discriminator(nn.Module):
    def __init__(self, in_features):
        super(Discriminator, self).__init__()

        self.d = nn.Linear(in_features, in_features, bias=True)
        self.wd = nn.Linear(in_features, 1, bias=False)
        self.sigmoid = nn.Sigmoid()

    def forward(self, ft):
        ft = F.elu(ft)
        ft = F.dropout(ft, 0.5, training=self.training)

        fc = F.elu(self.d(ft))
        prob = self.wd(fc)

        return prob


class Relation(nn.Module):
    def __init__(self, in_features, ablation):
        super(Relation, self).__init__()

        self.gamma_1 = nn.Linear(in_features, in_features, bias=False)
        self.gamma_2 = nn.Linear(in_features, in_features, bias=False)

        self.beta_1 = nn.Linear(in_features, in_features, bias=False)
        self.beta_2 = nn.Linear(in_features, in_features, bias=False)

        self.r = Parameter(torch.FloatTensor(1, in_features))

        # self.m_r = nn.Linear(in_features, in_features, bias=False)

        self.elu = nn.ELU()
        self.lrelu = nn.LeakyReLU(0.2)

        self.sigmoid = nn.Sigmoid()
        self.reset_parameter()
        self.ablation = ablation

    def reset_parameter(self):
        stdv = 1.0 / math.sqrt(self.r.size(1))
        self.r.data.uniform_(-stdv, stdv)

    def forward(self, ft, neighbor):
        # This function is used for the learning process of the knowledge transfer module, which should not directly
        # affect the training process of node embeddings. Therefore, perform ft.detach().
        ft = ft.detach()
        neighbor = neighbor.detach()
        if self.ablation == 3:
            m = ft + self.r - neighbor
        else:
            gamma = self.gamma_1(ft) + self.gamma_2(neighbor)
            gamma = self.lrelu(gamma) + 1.0

            beta = self.beta_1(ft) + self.beta_2(neighbor)
            beta = self.lrelu(beta)

            r_v = gamma * self.r + beta

            # transE
            m = ft + r_v - neighbor

        return m


class RelationF(nn.Module):
    def __init__(self, nfeat):
        super(RelationF, self).__init__()
        self.fc1 = nn.Linear(nfeat * 4, nfeat)
        self.fc2 = nn.Linear(nfeat, nfeat)

    def forward(self, x, neighbor, masked_neighbor):
        # This function is used for the learning process of the knowledge transfer module, which should not directly
        # affect the training process of node embeddings. Therefore, perform x.detach().
        x = x.detach()
        neighbor = neighbor.detach()
        masked_neighbor = masked_neighbor.detach()

        ngb_seq = torch.stack([x, neighbor, neighbor * x, (neighbor + x) / 2.0], dim=1)
        missing_info = self.fc1(ngb_seq.reshape(len(ngb_seq), -1))
        missing_info = F.relu(missing_info)
        missing_info = self.fc2(missing_info)
        support_out = missing_info - masked_neighbor
        return missing_info, support_out


class LightTailGCN(nn.Module):
    def __init__(self, nfeat, global_r=None, use_relation_rf=False):
        super(LightTailGCN, self).__init__()
        self.use_relation_rf = use_relation_rf
        if self.use_relation_rf:
            self.trans_relation = RelationF(nfeat)
        else:
            self.trans_relation = Relation(nfeat, 0)
        if global_r is not None:
            self.trans_relation = global_r
        self.special_spmm = SpecialSpmm()

    def forward(
        self,
        x,
        adj,
        adj_norm,
        adj_node_degree,
        adj_with_loop,
        adj_with_loop_norm,
        adj_with_loop_norm_plus_1,
        head,
        res_adj,
        res_adj_norm,
    ):
        # The variable "neighbor" is pooling embeddings of observable neighbors, where both "adj" and "res_adj" do not
        # include self-loops, thus the variable "neighbor" does not have self-information.
        neighbor = self.special_spmm(adj_norm, x)

        # Knowledge Transfer Module of LAGCL. This module will predict missing neighborhood information.
        if self.use_relation_rf:
            masked_neighbor = torch.sparse.mm(res_adj_norm, x)
            missing_info, output = self.trans_relation(x, neighbor, masked_neighbor)
        else:
            missing_info = self.trans_relation(x, neighbor)
            output = missing_info

        if head:
            h_k = self.special_spmm(adj_with_loop_norm, x)
        else:
            h_s = missing_info
            h_k = self.special_spmm(adj_with_loop_norm_plus_1, x) + h_s / (
                adj_node_degree + 2
            ).reshape(-1, 1)
        return h_k, output


class LAGCLEncoder(AGLAlgorithm):
    def __init__(
        self,
        in_dim: int,
        hidden_dim: int,
        n_hops: int,
        tail_k_threshold: int = 5,
        cl_rate: float = 0.2,
        cl_tau: float = 0.2,
        m_h_eps: float = 0.01,
        kl_eps: float = 10.0,
        noise_eps: float = 0.1,
        agg_function: int = 0,
        neighbor_norm_type: str = "left",
        use_relation_rf=False,
        agg_w_exp_scale=20,
    ):
        """
        Args:
            in_dim: Input dimension of the LAGCLEncoder. If different from hidden_dim, an additional Linear layer will
            be added for transformation.
            hidden_dim: Dimension of the node embeddings in each GNN layers.
            n_hops: Number of GNN layers.
            tail_k_threshold: Threshold k that distinguishes head and tail nodes based on node degree in graph. Set to
            -1 to use original LightGCN.
            cl_rate: Weight of contrastive learning loss for generating view1 and view2 of node embeddings through
            noise perturbation.
            cl_tau: A temperature hyperparametar for InfoNCE loss.
            m_h_eps: Weight of loss for knowledge transfer module.
            kl_eps: Weight of KL divergence loss for calculating information loss between pseudo-head nodes and
            real-head nodes.
            noise_eps: Amplitude of noise perturbation.
            agg_function: 0 for learning the similarity between nodes n1 and n2 based on network parameters, 1 for
            computing cosine similarity directly.
            neighbor_norm_type: Norm type used for aggregating neighbor information. Default is left.
            use_relation_rf: Type of missing neighborhood information prediction module in the Knowledge Transfer
            Module. Set to True to use MLP-based method.
            agg_w_exp_scale: Scaling factor for adjusting the adjacency matrix to the range of 0/1 after Auto Drop
            Module. A larger value indicates stronger scaling.
        """
        super().__init__()
        self.neighbor_norm_type = neighbor_norm_type
        self.tail_k_threshold = tail_k_threshold
        self.cl_rate = cl_rate
        self.cl_tau = cl_tau
        self.m_h_eps = m_h_eps
        self.kl_eps = kl_eps
        self.noise_eps = noise_eps
        self.agg_function = agg_function
        self.use_relation_rf = use_relation_rf
        self.agg_w_exp_scale = agg_w_exp_scale

        self.lin = (
            nn.Linear(in_dim, hidden_dim) if in_dim != hidden_dim else lambda x: x
        )

        self.hidden_dim = hidden_dim

        self.n_layers = n_hops
        self.x_weights = nn.Linear(hidden_dim, hidden_dim, bias=False)

        # self.global_r = Relation(emb_size, ablation=3)
        self.rel_layers = nn.ModuleList(
            [
                LightTailGCN(
                    hidden_dim, global_r=None, use_relation_rf=self.use_relation_rf
                )
                for _ in range(self.n_layers)
            ]
        )

    def encode(self, subgraph: TorchSubGraphBatchData, x: torch.Tensor):
        """
        Args:
            subgraph: TorchSubGraphBatchData
            x: initialized node feat

        Returns:
            embeddings or other information you need
        """
        ego_embeddings = self.lin(x)
        edge_index = subgraph.adjs_t.edge_index
        node_degrees = subgraph.n_feats.features["node_degree"].x.reshape(-1)
        node_types = subgraph.n_feats.features["node_type"].x.reshape(-1)
        drop_node_mask = (
            node_types == 0
        )  # auto drop for user nodes. (item nodes are similar)
        node_is_head_mask = node_degrees > self.tail_k_threshold
        node_is_head_mask[~drop_node_mask] = True

        # Drop the self-loop of original graph (For LightGCN).
        edge_index = edge_index[:, edge_index[0] != edge_index[1]]

        n_nodes = len(ego_embeddings)
        n_edges = len(edge_index[0])

        # build some adjacency matrixs for LAGCL.
        adj = torch.sparse_coo_tensor(
            edge_index,
            torch.ones(n_edges, device=edge_index.device),
            size=(n_nodes, n_nodes),
            device=edge_index.device,
        ).coalesce()
        adj_norm, adj_node_degree = GraphSparseMatInterface.normalize_graph_mat(
            adj, norm=self.neighbor_norm_type, return_node_degree=True
        )
        adj_with_loop = adj + torch.sparse_coo_tensor(
            torch.arange(len(adj), device=adj.device).repeat(2, 1),
            torch.ones(len(adj), device=adj.device),
            device=adj.device,
        )

        adj_with_loop_norm = GraphSparseMatInterface.normalize_graph_mat(adj_with_loop)
        adj_with_loop_norm_plus_1 = GraphSparseMatInterface.normalize_graph_mat(
            adj_with_loop, add_virtual_node_num=1
        )
        res_adj = torch.sparse_coo_tensor(
            torch.tensor([[0], [0]], dtype=torch.int64),
            torch.tensor([0.0]),
            adj.shape,
            device=adj.device,
        )
        res_adj_norm = GraphSparseMatInterface.normalize_graph_mat(res_adj)

        enable_lagcl = self.tail_k_threshold >= 0

        # do not use auto drop module and knowledge transfer module, then predict all embeddings of nodes.
        emb_h, support_h, _ = self.gather_pre(
            ego_embeddings,
            adj,
            adj_norm,
            adj_node_degree,
            adj_with_loop,
            adj_with_loop_norm,
            adj_with_loop_norm_plus_1,
            res_adj,
            res_adj_norm,
            head=True,
            use_auto_drop=False,
            drop_node_mask=None,
            node_is_head_mask=None,
            add_other_status=False,
        )

        if enable_lagcl:
            # use auto drop module and knowledge transfer module to enhance the embeddings of real/pseudo-tail nodes.
            emb_t, support_t, emb_nt = self.gather_pre(
                ego_embeddings,
                adj,
                adj_norm,
                adj_node_degree,
                adj_with_loop,
                adj_with_loop_norm,
                adj_with_loop_norm_plus_1,
                res_adj,
                res_adj_norm,
                head=False,
                use_auto_drop=True if self.training else False,
                drop_node_mask=drop_node_mask,
                node_is_head_mask=node_is_head_mask,
                add_other_status=True,
            )

            # merge the raw node embeddings and augmented node embeddings.
            node_emb = emb_h * node_is_head_mask.long().reshape(-1, 1) + emb_t * (
                1 - node_is_head_mask.long().reshape(-1, 1)
            )
        else:
            node_emb = emb_h
            emb_nt = emb_t = emb_h
            support_t = support_h

        other_embs_dict = {
            # w/o knowledge transfer module, w/o auto drop module.
            "head_true_drop_false": emb_h,
            # w/o knowledge transfer module, w/ auto drop module.
            "head_true_drop_true": emb_nt,
            # w/ knowledge transfer module, w/ auto drop module.
            "head_false_drop_true": emb_t,
            "support_h": support_h,
            "support_t": support_t,
        }

        return node_emb, other_embs_dict

    def gather_pre(
        self,
        ego_embeddings,
        adj,
        adj_norm,
        adj_node_degree,
        adj_with_loop,
        adj_with_loop_norm,
        adj_with_loop_norm_plus_1,
        res_adj,
        res_adj_norm,
        head,
        use_auto_drop,
        drop_node_mask,
        node_is_head_mask,
        add_other_status=False,
    ):
        """
        If 'add_other_status' is True, add the representation of 'head' being False, and vice versa.
        """
        tail_k_threshold = self.tail_k_threshold
        assert tail_k_threshold != 0
        if use_auto_drop and tail_k_threshold > 0:
            indices = adj.indices()
            node_need_drop = drop_node_mask[indices[0]]
            indices = indices.t()[node_need_drop].t()
            if self.agg_function == 0:
                # attention mechanism
                ego_norm = torch.max(
                    ego_embeddings.norm(dim=1)[:, None],
                    torch.zeros(len(ego_embeddings), 1, device=ego_embeddings.device)
                    + 1e-8,
                )
                normd_emb = ego_embeddings / ego_norm

                agg_w = (
                    self.x_weights.weight[0]
                    * normd_emb[indices[0]]
                    * normd_emb[indices[1]]
                ).sum(dim=1)

                agg_w = torch.nn.Softsign()(agg_w)
                agg_w = torch.nn.Softsign()(torch.exp(agg_w * self.agg_w_exp_scale))
            else:
                # cosine similarity
                sims = F.cosine_similarity(
                    ego_embeddings[indices[0]], ego_embeddings[indices[1]]
                )
                sims = torch.nn.Softsign()(torch.exp(sims * self.agg_w_exp_scale))
                agg_w = sims

            head_to_tail_sample_type = "top-k"  # topk or mantail-k
            if head_to_tail_sample_type == "top-k":
                drop_node_is_head_mask = node_is_head_mask[drop_node_mask]
                # The maximum number of neighbors sampled for all nodes is k, and the specific number
                # is randomly generated.
                # idea: first randomly assign each node an integer from 1 to k, then set the outgoing edges of that
                # node to this type. During sampling, different types of edges will sample different neighbors.
                k = {i: i for i in range(1, tail_k_threshold + 1)}

                # Option 1 (current): Randomly sample node_type with equal frequency during sampling, which ensures
                # that the number of each degree in the pseudo-labels is balanced.
                # Option 2: When sampling the node_type, sample according to the distribution of the actual number of
                # tail nodes. This will make the quantity of each degree in the pseudo labels similar to the actual
                # number of tail nodes.
                node_type = torch.randint(
                    1,
                    tail_k_threshold + 1,
                    (len(ego_embeddings),),
                    device=indices.device,
                )

                # Do not apply the auto drop for real-tail nodes and keep the original adjacency relationship.
                node_type[drop_node_mask][~drop_node_is_head_mask] = tail_k_threshold
                edge_type = node_type[indices[0]]
                data_dict = {}
                edata = {}
                for edge_type_idx in k.keys():
                    select_mask = edge_type == edge_type_idx
                    nen_type = ("node", edge_type_idx, "node")
                    data_dict[nen_type] = (
                        indices[0][select_mask],
                        indices[1][select_mask],
                    )
                    edata[nen_type] = agg_w[select_mask]
                g = dgl.heterograph(data_dict)
                g.edata["weight"] = edata

                sampled_g = dgl.sampling.sample_neighbors(
                    g,
                    nodes=g.nodes(),
                    fanout=k,
                    edge_dir="out",
                    prob="weight",
                    output_device=g.device,
                )
                all_edges = []
                all_agg_w = []
                for etype in k.keys():
                    all_edges.append(torch.stack(sampled_g.edges(etype=etype)))
                    all_agg_w.append(sampled_g.edata["weight"][("node", etype, "node")])
                all_edges = torch.cat(all_edges, dim=1)
                all_agg_w = torch.cat(all_agg_w)
            elif head_to_tail_sample_type == "mantail-k":
                g = dgl.graph((indices[0], indices[1]))
                g.edata["weight"] = agg_w
                sampled_g = dgl.sampling.sample_neighbors(
                    g,
                    nodes=g.nodes(),
                    fanout=tail_k_threshold,
                    edge_dir="out",
                    prob="weight",
                )
                all_edges = sampled_g.edges()
                all_agg_w = sampled_g.edata["weight"]

            tail_indices = torch.stack(
                [
                    torch.cat([all_edges[0], all_edges[1]]),
                    torch.cat([all_edges[1], all_edges[0]]),
                ]
            )
            tail_values = torch.cat([all_agg_w, all_agg_w])

            tail_adj = torch.sparse_coo_tensor(
                tail_indices, tail_values, adj.shape, device=adj.device
            ).coalesce()
            (
                tail_adj_norm,
                tail_adj_node_degree,
            ) = GraphSparseMatInterface.normalize_graph_mat(
                tail_adj, norm=self.neighbor_norm_type, return_node_degree=True
            )
            tail_adj_with_loop = tail_adj + torch.sparse_coo_tensor(
                torch.arange(len(tail_adj)).repeat(2, 1),
                torch.ones(len(tail_adj)),
                device=adj.device,
            )

            tail_adj_with_loop_norm = GraphSparseMatInterface.normalize_graph_mat(
                tail_adj_with_loop
            )
            tail_adj_with_loop_norm_plus_1 = (
                GraphSparseMatInterface.normalize_graph_mat(
                    tail_adj_with_loop, add_virtual_node_num=1
                )
            )
            tail_res_adj = eliminate_zeros(adj - tail_adj)
            tail_res_adj_norm = GraphSparseMatInterface.normalize_graph_mat(
                tail_res_adj
            )

            adj, adj_norm, adj_node_degree = (
                tail_adj,
                tail_adj_norm,
                tail_adj_node_degree,
            )
            adj_with_loop, adj_with_loop_norm = (
                tail_adj_with_loop,
                tail_adj_with_loop_norm,
            )
            adj_with_loop_norm_plus_1 = tail_adj_with_loop_norm_plus_1
            res_adj, res_adj_norm = tail_res_adj, tail_res_adj_norm

        all_status_embeddings = {True: [], False: []}
        all_status_support_outs = {True: [], False: []}
        ego_embeddings1 = ego_embeddings2 = ego_embeddings
        for k in range(self.n_layers):
            ego_embeddings1, output1 = self.rel_layers[k](
                ego_embeddings1,
                adj,
                adj_norm,
                adj_node_degree,
                adj_with_loop,
                adj_with_loop_norm,
                adj_with_loop_norm_plus_1,
                head,
                res_adj,
                res_adj_norm,
            )
            if add_other_status:
                ego_embeddings2, output2 = self.rel_layers[k](
                    ego_embeddings2,
                    adj,
                    adj_norm,
                    adj_node_degree,
                    adj_with_loop,
                    adj_with_loop_norm,
                    adj_with_loop_norm_plus_1,
                    not head,
                    res_adj,
                    res_adj_norm,
                )
            else:
                ego_embeddings2, output2 = ego_embeddings1, output1
            all_status_embeddings[head].append(ego_embeddings1)
            all_status_embeddings[not head].append(ego_embeddings2)
            all_status_support_outs[head].append(output1)
            all_status_support_outs[not head].append(output2)

        def agg_all_layers_out(all_embeddings, backbone_name="lightgcn"):
            if backbone_name == "lightgcn":
                all_embeddings = torch.stack(all_embeddings, dim=1)
                all_embeddings = torch.mean(all_embeddings, dim=1)
            elif backbone_name == "gcn":
                all_embeddings = all_embeddings[-1]
            return all_embeddings

        all_embeddings = agg_all_layers_out(all_status_embeddings[head])
        all_embeddings_other = agg_all_layers_out(all_status_embeddings[not head])

        return all_embeddings, all_status_support_outs[head], all_embeddings_other

    def forward(
        self,
        subgraph: TorchSubGraphBatchData,
        x: torch.Tensor,
    ):
        return self.encode(subgraph, x)
