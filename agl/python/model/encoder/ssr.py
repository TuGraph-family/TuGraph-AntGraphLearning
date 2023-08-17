from typing import List, Dict

import torch
from torch import Tensor
import torch.nn.functional as F

from agl.python.data.subgraph.pyg_inputs import TorchEgoBatchData
from agl.python.model.layer.ssr_layer import GumbelConv


class SSREncoder(torch.nn.Module):
    """
        First stage of SSR.
    """
    def __init__(self, hidden_dim: int, out_dim: int,
                 sampled_num: int=20, gumbel_temperature: float=0.1,
                 n_hops: int=2, residual: bool=True):
        super().__init__()

        self.hidden_dim = hidden_dim
        self.residual = residual
        self.n_hops = n_hops
        self.sampled_num = sampled_num
        self.gumbel_temperature = gumbel_temperature

        self.convs = GumbelConv(hidden_dim, hidden_dim, sampled_num=self.sampled_num,
                                gumbel_temperature=self.gumbel_temperature)

    def forward(self, subgraph: TorchEgoBatchData, x: torch.Tensor):
        for adj in subgraph.adjs_t:
            x = self.convs(x, adj.edge_index)
        x = x[subgraph.root_index]
        return x


class SSR2Encoder(torch.nn.Module):
    """
        Second stage of SSR.
    """
    def __init__(self, hidden_dim: int, neg_sample_size: int=1, tau: float=0.2,
                 reg_infonce: float=0.01, view_nums: int=4):
        super().__init__()
        self.hidden_dim = hidden_dim
        self.neg_sample_size = neg_sample_size
        self.view_nums = view_nums
        self.tau = tau
        self.reg_infonce = reg_infonce

        self.user_lins = torch.nn.ModuleList([torch.nn.Linear(hidden_dim, hidden_dim)
                                              for _ in range(view_nums)])
        self.item_lins = torch.nn.ModuleList([torch.nn.Linear(hidden_dim, hidden_dim)
                                              for _ in range(view_nums)])
        self.user_att = torch.nn.Linear(hidden_dim, 1)
        self.item_att = torch.nn.Linear(hidden_dim, 1)
        self.user_final_lin = torch.nn.Linear(hidden_dim, hidden_dim)
        self.item_final_lin = torch.nn.Linear(hidden_dim, hidden_dim)

    def mutual_info(self, x1, x2, tau=0.2):
        f = torch.sum(F.normalize(x1, p=2, dim=-1)*F.normalize(x2, p=2, dim=-1), -1)
        return torch.exp(f/tau)

    def shuffle(self, batch):
        return batch[torch.randperm(batch.shape[0])]

    def forward(self, user_feats: List[Tensor], item_feats: List[Tensor], mode='train'):
        user_embed, item_embed = None, None
        if user_feats is not None:
            user_embeds = [F.leaky_relu(self.user_lins[i](user_feats[i])) for i in range(self.view_nums)]
            user_atts = torch.softmax(torch.cat([self.user_att(user_embeds[i]) for i in range(self.view_nums)], -1), -1)
            user_agg_embed = torch.sum(torch.stack(user_embeds, 1)*torch.unsqueeze(user_atts, -1), 1)
            user_embed = self.user_final_lin(user_agg_embed)

        if item_feats is not None:
            item_embeds = [F.leaky_relu(self.item_lins[i](item_feats[i])) for i in range(self.view_nums)]
            item_atts = torch.softmax(torch.cat([self.item_att(item_embeds[i]) for i in range(self.view_nums)], -1), -1)
            item_agg_embed = torch.sum(torch.stack(item_embeds, 1)*torch.unsqueeze(item_atts, -1), 1)
            item_embed = self.item_final_lin(item_agg_embed)

        infonce_loss = 0.0
        if mode == 'train':
            infonce_count = 1e-8
            for i in range(self.view_nums):
                for j in range(i+1, self.view_nums):
                    true_distance = self.mutual_info(user_agg_embed[i], user_agg_embed[j])
                    fake_distance = 0.0
                    for _ in range(self.neg_sample_size):
                        fake_user_embed = self.shuffle(user_agg_embed[j])
                        fake_distance += self.mutual_info(user_agg_embed[i], fake_user_embed)
                    infonce_loss += torch.log(true_distance/(true_distance+fake_distance))
                    infonce_count += 1
            infonce_loss = -self.reg_infonce*torch.mean(infonce_loss) / float(infonce_count)

        return user_embed, item_embed, infonce_loss
