import random

import torch
from torch_geometric.nn.conv import MessagePassing
import torch_sparse


def metric_accuracy(logits, labels):
    _, indices = torch.max(logits, dim=1)
    correct = torch.sum(indices == labels)
    return correct.item() * 1.0 / len(labels)


def neighbor_replace_aug(subgraph):
    adjs_t = subgraph.adjs_t
    row_ptr, col = adjs_t.row_ptr, adjs_t.col
    dst_indices, src_indices = (
        adjs_t.private_edge_index[0, :],
        adjs_t.private_edge_index[1, :],
    )

    root_list = subgraph.root_index.tolist()
    csr_row_ptr = row_ptr.tolist()
    csr_col = col.tolist()

    del_dst, del_src, add_dst, add_src = [], [], [], []
    for root_id in root_list:
        for neighbor_idx in range(csr_row_ptr[root_id], csr_row_ptr[root_id + 1]):
            if random.uniform(0, 1) < 0.5:
                neighbor = csr_col[neighbor_idx]
                neighbors_neib_idx = random.randint(
                    csr_row_ptr[neighbor], csr_row_ptr[neighbor + 1] - 1
                )
                neighbors_neib = csr_col[neighbors_neib_idx]

                if neighbor == root_id:
                    continue
                if (
                    neighbors_neib
                    in csr_col[csr_row_ptr[root_id] : csr_row_ptr[root_id + 1]]
                ):
                    continue

                del_dst += [root_id, neighbor]
                del_src += [neighbor, root_id]
                add_dst += [root_id, neighbors_neib]
                add_src += [neighbors_neib, root_id]

    del_dst = torch.tensor(del_dst, device=dst_indices.device)
    del_src = torch.tensor(del_src, device=dst_indices.device)
    add_dst = torch.tensor(add_dst, device=dst_indices.device)
    add_src = torch.tensor(add_src, device=dst_indices.device)

    aug_dst = torch.cat((dst_indices, add_dst, del_dst), 0)
    aug_src = torch.cat((src_indices, add_src, del_src), 0)
    aug_value = torch.cat(
        (
            torch.ones_like(dst_indices),
            torch.ones_like(add_dst),
            torch.zeros_like(del_dst),
        ),
        0,
    )

    aug_edge_index = torch.stack([aug_src, aug_dst], dim=0)
    aug_edge_index, aug_value = torch_sparse.coalesce(
        aug_edge_index,
        aug_value,
        m=subgraph.adjs_t.size[0],
        n=subgraph.adjs_t.size[1],
        op="min",
    )
    nnz_mask = torch.nonzero(aug_value).squeeze()
    aug_edge_index = aug_edge_index[:, nnz_mask]

    return aug_edge_index


class MeanAGG(MessagePassing):
    def __init__(self):
        super().__init__(aggr="mean")

    def forward(self, x, edge_index):
        return self.propagate(edge_index, x=x)

    def message(self, x_j):
        return x_j


class NeighborConstrainedRegLoss(torch.nn.Module):
    def __init__(self, temp):
        super(NeighborConstrainedRegLoss, self).__init__()
        self.mean_agg = MeanAGG()
        self.temp = temp

    def forward(self, edge_index, aug_pred):
        avg_pred = self.mean_agg(aug_pred, edge_index)
        sharp_avg_pred = torch.pow(avg_pred, 1.0 / self.temp) / torch.sum(
            torch.pow(avg_pred, 1.0 / self.temp), dim=1, keepdim=True
        )

        src = avg_pred + 1e-10
        dst = (sharp_avg_pred + 1e-10).detach()

        src = src[edge_index[0, :].squeeze(), :]
        dst = dst[edge_index[1, :].squeeze(), :]

        cr_loss = dst * dst.log() - dst * src.log()
        cr_loss = cr_loss.sum(1, keepdim=True).mean()
        return cr_loss
