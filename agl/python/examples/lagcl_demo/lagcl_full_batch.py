import time
import os
import random

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import DataLoader
import numpy as np
import dgl
import pandas as pd
import pickle
import argparse
import wandb
from tqdm import tqdm

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.column import AGLRowColumn
from pyagl import (
    AGLDType,
    DenseFeatureSpec,
    NodeSpec,
    EdgeSpec,
)
from agl.python.model.encoder.lagcl_encoder import LAGCLEncoder, Discriminator
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.model.utils.loss_utils import bpr_loss, l2_reg_loss, InfoNCE


def setup_manual_seed(seed):
    """
    设定全局随机种子，包括 numpy、torch、dgl 等
    """
    np.random.seed(seed)
    random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    dgl.random.seed(seed)


class LAGCLLinkPredictionModel(torch.nn.Module):
    def __init__(
        self,
        graph,
        user_num: int,
        item_num: int,
        hidden_dim: int,
        n_hops: int,
        noise_eps: float,
        cl_tau: float,
        cl_rate: float,
        tail_k_threshold: int,
        m_h_eps: float,
        kl_eps: float,
        l2_reg: float,
        agg_function: int,
        neighbor_norm_type: str,
        use_relation_rf: bool,
        annealing_type: int,
        lambda_gp: float,
        agg_w_exp_scale: float,
        device="cpu",
    ):
        super().__init__()
        self.noise_eps = noise_eps
        self.cl_tau = cl_tau
        self.cl_rate = cl_rate
        self.tail_k_threshold = tail_k_threshold
        self.m_h_eps = m_h_eps
        self.kl_eps = kl_eps
        self.l2_reg = l2_reg
        self.agg_function = agg_function
        self.neighbor_norm_type = neighbor_norm_type
        self.use_relation_rf = use_relation_rf
        self.annealing_type = annealing_type
        self.lambda_gp = lambda_gp
        self.agg_w_exp_scale = agg_w_exp_scale

        self.user_num = user_num
        self.item_num = item_num
        self.hidden_dim = hidden_dim
        self.device = device

        self.graph = graph.to(device)
        self.node_ids = self.graph.n_feats.features["node_feature"].x.reshape(-1)
        self.node_types = self.graph.n_feats.features["node_type"].x.reshape(-1)
        self.is_user_node = self.node_types == 0
        self.global_head_mask = (
            self.graph.n_feats.features["node_degree"].x.reshape(-1) > tail_k_threshold
        )
        self.user_is_head_mask = self.global_head_mask[self.is_user_node]
        self.item_is_head_mask = self.global_head_mask[~self.is_user_node]

        # initial layer
        self._node_initial = nn.Parameter(
            nn.init.xavier_uniform_(torch.empty(user_num + item_num, hidden_dim))
        )

        # encoder layer
        self._encoder = LAGCLEncoder(
            in_dim=hidden_dim,
            hidden_dim=hidden_dim,
            n_hops=n_hops,
            noise_eps=noise_eps,
            cl_tau=cl_tau,
            cl_rate=cl_rate,
            tail_k_threshold=tail_k_threshold,
            m_h_eps=m_h_eps,
            kl_eps=kl_eps,
            agg_function=agg_function,
            neighbor_norm_type=neighbor_norm_type,
            use_relation_rf=use_relation_rf,
            agg_w_exp_scale=agg_w_exp_scale,
        )

    def split_user_item_embs(self, node_embs):
        # 记录最新训练产出的每个点的表征
        user_node_ids = self.node_ids[self.is_user_node]
        item_node_ids = self.node_ids[~self.is_user_node]

        user_embs = torch.zeros(self.user_num, self.hidden_dim, device=self.device)
        item_embs = torch.zeros(self.item_num, self.hidden_dim, device=self.device)

        user_embs[user_node_ids] = node_embs[self.is_user_node]
        item_embs[item_node_ids - self.user_num] = node_embs[~self.is_user_node]

        return user_embs, item_embs

    def process_one_subgraph(self):
        x = self._node_initial[self.node_ids]

        node_embeddings, other_embs_dict = self._encoder(self.graph, x)

        rec_user_emb, rec_item_emb = self.split_user_item_embs(node_embeddings)

        emb_h, emb_nt, emb_t = (
            other_embs_dict["head_true_drop_false"],
            other_embs_dict["head_true_drop_true"],
            other_embs_dict["head_false_drop_true"],
        )
        rec_user_emb_h, rec_item_emb_h = self.split_user_item_embs(emb_h)
        rec_user_emb_t, rec_item_emb_t = self.split_user_item_embs(emb_t)
        rec_u_emb_nt, rec_i_emb_nt = self.split_user_item_embs(emb_nt)

        support_h, support_t = (
            other_embs_dict["support_h"],
            other_embs_dict["support_t"],
        )

        return (
            (rec_user_emb, rec_item_emb),
            (support_h, support_t),
            {
                # 不补邻居、不裁剪
                "user_head_true_drop_false": rec_user_emb_h,
                "item_head_true_drop_false": rec_item_emb_h,
                # 补邻居、不裁剪
                # 'user_head_false_drop_false': rec_user_emb_h,
                # 'item_head_false_drop_false': rec_item_emb_h,
                # 不补邻居、裁剪
                "user_head_true_drop_true": rec_u_emb_nt,
                "item_head_true_drop_true": rec_i_emb_nt,
                # 补邻居、裁剪
                "user_head_false_drop_true": rec_user_emb_t,
                "item_head_false_drop_true": rec_item_emb_t,
            },
        )

    def forward(self, data):
        user_idx, pos_idx, neg_idx = data
        pos_idx = torch.tensor(pos_idx) - self.user_num
        neg_idx = torch.tensor(neg_idx) - self.user_num

        (
            (rec_user_emb, rec_item_emb),
            (support_h, support_t),
            other_embs_dict,
        ) = self.process_one_subgraph()
        u_emb_h, u_emb_t, u_emb_nt = (
            other_embs_dict["user_head_true_drop_false"],
            other_embs_dict["user_head_false_drop_true"],
            other_embs_dict["user_head_true_drop_true"],
        )
        i_emb_h, i_emb_t, i_emb_nt = (
            other_embs_dict["item_head_true_drop_false"],
            other_embs_dict["item_head_false_drop_true"],
            other_embs_dict["item_head_true_drop_true"],
        )

        user_emb, pos_item_emb, neg_item_emb = (
            rec_user_emb[user_idx],
            rec_item_emb[pos_idx],
            rec_item_emb[neg_idx],
        )

        # 计算主推荐任务的 BPR Loss，以及 L2 Norm Loss
        task_loss = bpr_loss(
            user_emb=user_emb, pos_item_emb=pos_item_emb, neg_item_emb=neg_item_emb
        )
        l2_loss = l2_reg_loss(self.l2_reg, user_emb, pos_item_emb)

        # 计算 real-head node 通过 auto drop 以及补邻居操作还原所得的 pseudo-head node 之间的信息损失
        L_kl_corr_user = L_kl_corr_item = torch.tensor(0.0)
        L_kl_corr_user = (
            torch.nn.functional.kl_div(
                u_emb_t[user_idx].log_softmax(dim=-1),
                u_emb_h[user_idx].softmax(dim=-1),
                reduction="batchmean",
                log_target=False,
            )
            * self._encoder.kl_eps
        )
        L_kl_corr = L_kl_corr_user + L_kl_corr_item

        # 计算对最终节点表征的噪声扰动产生的对比学习损失
        cl_loss = self._encoder.cl_rate * self.cal_cl_loss(
            [user_idx, pos_idx], rec_user_emb, rec_item_emb
        )

        # 计算 Knowledge Trans Module 中预测邻域缺失信息的 Loss
        m_regularizer = (
            self.cal_m_regularizer_loss(
                [
                    i[self.is_user_node]
                    for i in (support_t if self.use_relation_rf else support_h)
                ],
                self.global_head_mask[self.is_user_node],
            )
            * self._encoder.m_h_eps
        )

        loss = task_loss + l2_loss + L_kl_corr + cl_loss + m_regularizer
        return (rec_user_emb, rec_item_emb), loss

    def cal_cl_loss(self, idx, user_emb, item_emb):
        u_idx = torch.unique(torch.Tensor(idx[0]).type(torch.long)).to(self.device)
        i_idx = torch.unique(torch.Tensor(idx[1]).type(torch.long)).to(self.device)

        def exec_perturbed(x):
            random_noise = torch.rand_like(x).to(self.device)
            x = (
                x
                + torch.sign(x)
                * F.normalize(random_noise, dim=-1)
                * self._encoder.noise_eps
            )
            return x

        user_view_1 = exec_perturbed(user_emb)
        user_view_2 = exec_perturbed(user_emb)
        item_view_1 = exec_perturbed(item_emb)
        item_view_2 = exec_perturbed(item_emb)
        user_cl_loss = InfoNCE(
            user_view_1[u_idx], user_view_2[u_idx], self._encoder.cl_tau
        )
        item_cl_loss = InfoNCE(
            item_view_1[i_idx], item_view_2[i_idx], self._encoder.cl_tau
        )
        return user_cl_loss + item_cl_loss

    def cal_m_regularizer_loss(self, support_out, head_mask):
        m_reg_loss = 0
        for m in support_out:
            m_reg_loss += torch.mean(torch.norm(m[head_mask], dim=1))
        return m_reg_loss

    def train_disc(
        self,
        embed_model,
        disc_model,
        optimizer_D,
        disc_pseudo_real,
        optimizer_D_pseudo_real,
        cur_iter,
        iter_num,
    ):
        disc_model.train()
        disc_pseudo_real.train()
        # embed_model.eval()
        embed_model.train()

        for p in disc_model.parameters():
            p.requires_grad = True  # to avoid computation
        for p in disc_pseudo_real.parameters():
            p.requires_grad = True
        with torch.no_grad():
            _, _, other_embs_dict = embed_model.process_one_subgraph()
            u_emb_h, u_emb_t, u_emb_nt = (
                other_embs_dict["user_head_true_drop_false"],
                other_embs_dict["user_head_false_drop_true"],
                other_embs_dict["user_head_true_drop_true"],
            )
            i_emb_h, i_emb_t, i_emb_nt = (
                other_embs_dict["item_head_true_drop_false"],
                other_embs_dict["item_head_false_drop_true"],
                other_embs_dict["item_head_true_drop_true"],
            )

        # -- only user
        all_head_emb_h = u_emb_h[self.user_is_head_mask]
        all_emb_t = u_emb_t
        all_emb_nt = u_emb_nt
        cell_mask = self.user_is_head_mask
        # -- only user

        if True:
            if self.annealing_type == 0:
                # 不做退火
                noise_eps = self.noise_eps
            elif self.annealing_type == 1:
                # 均匀直线下降
                annealing_stop_rate = 0.7  # todo: 超参数，可调节
                rate = 1 - min(cur_iter / (annealing_stop_rate * iter_num), 1.0)
                noise_eps = self.noise_eps * rate
            elif self.annealing_type == 2:
                # 先快后慢，凹函数下降
                annealing_stop_rate = 0.6  # todo: 超参数，可调节
                annealing_rate = 0.01 ** (1 / iter_num / annealing_stop_rate)
                noise_eps = self.noise_eps * (annealing_rate**cur_iter)
            else:
                # 不加噪声
                noise_eps = 0.0

            def exec_perturbed(x, noise_eps):
                random_noise = torch.rand_like(x).to(self.device)
                x = x + torch.sign(x) * F.normalize(random_noise, dim=-1) * noise_eps
                return x

            # 对 real、fake 数据加噪声
            all_head_emb_h = exec_perturbed(all_head_emb_h, noise_eps=noise_eps)
            all_emb_t = exec_perturbed(all_emb_t, noise_eps=noise_eps)
            all_emb_nt = exec_perturbed(all_emb_nt, noise_eps=noise_eps)

        prob_h = disc_model(all_head_emb_h)
        prob_t = disc_model(all_emb_t)

        errorD = -prob_h.mean()
        errorG = prob_t.mean()

        def get_select_idx(max_value, select_num, strategy="uniform"):
            """
            max_value: 在 [0, max_value) 之间采样
            select_num: 采样数目
            strategy: 采样策略，uniform 均极度均匀，random 为随机采样
            """
            select_idx = None
            if strategy == "uniform":
                select_idx = torch.randperm(max_value).repeat(
                    int(np.ceil(select_num / max_value))
                )[:select_num]
            elif strategy == "random":
                select_idx = np.random.randint(0, max_value, select_num)
            return select_idx

        def calc_gradient_penalty(netD, real_data, fake_data, lambda_gp):
            alpha = torch.rand(real_data.shape[0], 1).to(self.device)
            alpha = alpha.expand(real_data.size())

            interpolates = alpha * real_data + ((1 - alpha) * fake_data)
            interpolates.requires_grad_(True)

            disc_interpolates = netD(interpolates)

            import torch.autograd as autograd

            gradients = autograd.grad(
                outputs=disc_interpolates,
                inputs=interpolates,
                grad_outputs=torch.ones(disc_interpolates.size(), device=self.device),
                create_graph=True,
                retain_graph=True,
                only_inputs=True,
            )[0]

            gradient_penalty = ((gradients.norm(2, dim=1) - 1) ** 2).mean() * lambda_gp
            return gradient_penalty

        # disc 1
        gp_fake_data = all_emb_t
        gp_real_data = all_head_emb_h[
            get_select_idx(len(all_head_emb_h), len(gp_fake_data), strategy="random")
        ]
        gradient_penalty = calc_gradient_penalty(
            netD=disc_model,
            real_data=gp_real_data,
            fake_data=gp_fake_data,
            lambda_gp=self.lambda_gp,
        )
        L_d = errorD + errorG + gradient_penalty

        optimizer_D.zero_grad()
        L_d.backward()
        optimizer_D.step()

        # disc 2
        pseudo_embs = all_emb_nt[cell_mask]
        real_tail_embs = all_emb_nt[~cell_mask]
        if len(pseudo_embs) > len(real_tail_embs):
            gp_fake_data = pseudo_embs
            gp_real_data = real_tail_embs[
                get_select_idx(
                    len(real_tail_embs), len(gp_fake_data), strategy="random"
                )
            ]
        else:
            gp_real_data = real_tail_embs
            gp_fake_data = pseudo_embs[
                get_select_idx(len(pseudo_embs), len(gp_real_data), strategy="random")
            ]
        L_gp2 = calc_gradient_penalty(
            netD=disc_pseudo_real,
            real_data=gp_real_data,
            fake_data=gp_fake_data,
            lambda_gp=self.lambda_gp,
        )

        prob_t_with_miss = disc_pseudo_real(all_emb_nt)
        errorR_pseudo = prob_t_with_miss[cell_mask].mean()
        errorR_real_tail = -prob_t_with_miss[~cell_mask].mean()
        L_d2 = errorR_pseudo + errorR_real_tail + L_gp2

        optimizer_D_pseudo_real.zero_grad()
        L_d2.backward()
        optimizer_D_pseudo_real.step()

        log = {
            "loss/disc1_errorD": errorD.item(),
            "loss/disc1_errorG": errorG.item(),
            "loss/disc1_errorG_real": prob_t[~cell_mask].mean().item(),
            "loss/disc1_errorG_pseudo": prob_t[cell_mask].mean().item(),
            "loss/disc1_gp": gradient_penalty.item(),
            "loss/disc1_full": L_d.item(),
            "loss/disc2_full": L_d2.item(),
            "loss/disc2_gp": L_gp2.item(),
            "loss/disc2_errorR_pseudo": errorR_pseudo.item(),
            "loss/disc2_errorR_real_tail": errorR_real_tail.item(),
            "noise_eps": noise_eps,
        }
        if os.environ.get("use_wandb"):
            wandb.log(log)
        return L_d

    def train_gen(self, embed_model, optimizer, disc_model, disc_pseudo_real):
        embed_model.train()
        disc_model.train()
        disc_pseudo_real.train()
        for p in disc_model.parameters():
            p.requires_grad = False  # to avoid computation
        for p in disc_pseudo_real.parameters():
            p.requires_grad = False  # to avoid computation

        _, _, other_embs_dict = embed_model.process_one_subgraph()
        u_emb_h, u_emb_t, u_emb_nt = (
            other_embs_dict["user_head_true_drop_false"],
            other_embs_dict["user_head_false_drop_true"],
            other_embs_dict["user_head_true_drop_true"],
        )
        i_emb_h, i_emb_t, i_emb_nt = (
            other_embs_dict["item_head_true_drop_false"],
            other_embs_dict["item_head_false_drop_true"],
            other_embs_dict["item_head_true_drop_true"],
        )

        # disc 1
        all_emb_t = u_emb_t

        prob_t = disc_model(all_emb_t)
        L_disc1 = -prob_t.mean() * 0.1  # todo: 超参数，可调节

        # disc 2
        all_emb_nt = u_emb_nt[self.user_is_head_mask]

        prob_t_with_miss = disc_pseudo_real(all_emb_nt)

        L_disc2 = -prob_t_with_miss.mean() * 0.1

        L_d = L_disc1 + L_disc2

        optimizer.zero_grad()
        L_d.backward()
        optimizer.step()

        log = {
            "loss/discG_full": L_d.item(),
            "loss/discG_1": L_disc1.item(),
            "loss/discG_2": L_disc2.item(),
        }
        if os.environ.get("use_wandb"):
            wandb.log(log)
        return L_d


def next_batch_pairwise(
    training_data, node_table, training_set_u, training_set_i, batch_size
):
    from random import shuffle, choice

    n_negs = 1

    shuffle(training_data)
    batch_id = 0
    data_size = len(training_data)

    while batch_id < data_size:
        if batch_id + batch_size <= data_size:
            users = [
                training_data[idx][0] for idx in range(batch_id, batch_size + batch_id)
            ]
            items = [
                training_data[idx][1] for idx in range(batch_id, batch_size + batch_id)
            ]
            batch_id += batch_size
        else:
            users = [training_data[idx][0] for idx in range(batch_id, data_size)]
            items = [training_data[idx][1] for idx in range(batch_id, data_size)]
            batch_id = data_size

        u_idx, i_idx, j_idx = [], [], []
        item_list = list(training_set_i.keys())
        for i, user_id in enumerate(users):
            i_idx.append(node_table[items[i]]["node_feature"])
            u_idx.append(node_table[user_id]["node_feature"])
            for _ in range(n_negs):
                neg_item_id = choice(item_list)
                while neg_item_id in training_set_u[user_id]:
                    neg_item_id = choice(item_list)
                j_idx.append(node_table[neg_item_id]["node_feature"])
        yield u_idx, i_idx, j_idx


def build_argparse():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--dataset_name",
        type=str,
        default="lastfm",
        help="Dataset Name",
        choices=["lastfm", "yelp2018", "amazon-book", "ml-25m"],
    )
    parser.add_argument("--batch_size", type=int, default=-1, help="Batch Size")
    parser.add_argument("--n_hops", type=int, default=-1, help="Num layers of GNN")
    parser.add_argument("--user_num", type=int, default=-1, help="user num")
    parser.add_argument("--item_num", type=int, default=-1, help="item_num")
    parser.add_argument("--hidden_size", type=int, default=-1, help="hidden_size")
    parser.add_argument("--learning_rate", type=float, default=-1, help="learning_rate")
    parser.add_argument("--metric_topk", type=int, default=-1, help="metric_topk")
    parser.add_argument("--epoch", type=int, default=-1, help="epoch")

    parser.add_argument("--noise_eps", type=float, default=-1, help="noise_eps")
    parser.add_argument("--cl_tau", type=float, default=-1, help="InfoNCE tau")
    parser.add_argument("--cl_rate", type=float, default=-1, help="cl rate for InfoNCE")
    parser.add_argument(
        "--tail_k_threshold", type=int, default=-1, help="tail_k_threshold"
    )
    parser.add_argument("--m_h_eps", type=float, default=-1, help="")
    parser.add_argument("--kl_eps", type=float, default=-1, help="")

    parser.add_argument(
        "--neighbor_norm_type",
        type=str,
        default="-1",
        help="neighbor_norm_type for LAGCL",
    )
    parser.add_argument("--annealing_type", type=int, default=-1, help="")
    parser.add_argument("--agg_function", type=int, default=-1, help="")
    parser.add_argument("--lambda_gp", type=float, default=-1, help="")
    parser.add_argument("--use_relation_rf", action="store_true")
    parser.add_argument("--agg_w_exp_scale", type=float, default=-1, help="")

    parser.add_argument("--l2_reg", type=float, default=-1, help="")

    parser.add_argument("--seed", type=int, default=1024, help="random seed")
    args = parser.parse_args()

    # 固定随机种子，保证每次跑的分数不变
    setup_manual_seed(args.seed)

    if args.dataset_name == "lastfm":
        default_params = {
            "dataset_name": "lastfm",
            "batch_size": 2048,
            "n_hops": 2,
            "user_num": 1890,
            "item_num": 15387,
            "hidden_size": 64,
            "learning_rate": 0.001,
            "metric_topk": 20,
            "epoch": 50,
            "noise_eps": 0.1,
            "cl_tau": 0.2,
            "cl_rate": 0.1,
            "tail_k_threshold": 40,
            "m_h_eps": 0.01,
            "kl_eps": 10,
            "neighbor_norm_type": "both",
            "annealing_type": 3,
            "agg_function": 1,
            "lambda_gp": 1.0,
            "use_relation_rf": False,
            "agg_w_exp_scale": 20.0,
            "l2_reg": 0.0001,
        }
    elif args.dataset_name == "yelp2018":
        default_params = {
            "dataset_name": "yelp2018",
            "batch_size": 2048,
            "n_hops": 2,
            "user_num": 31668,
            "item_num": 38048,
            "hidden_size": 64,
            "learning_rate": 0.001,
            "metric_topk": 20,
            "epoch": 20,
            "noise_eps": 0.1,
            "cl_tau": 0.2,
            "cl_rate": 0.5,
            "tail_k_threshold": 20,
            "m_h_eps": 0.001,
            "kl_eps": 10.0,
            "neighbor_norm_type": "left",
            "annealing_type": 0,
            "agg_function": 0,
            "lambda_gp": 1.0,
            "use_relation_rf": True,
            "agg_w_exp_scale": 20.0,
            "l2_reg": 0.0001,
        }
    elif args.dataset_name == "amazon-book":
        default_params = {
            "dataset_name": "amazon-book",
            "batch_size": 2048,
            "n_hops": 2,
            "user_num": 52643,
            "item_num": 91599,
            "hidden_size": 64,
            "learning_rate": 0.001,
            "metric_topk": 20,
            "epoch": 15,
            "noise_eps": 0.1,
            "cl_tau": 0.2,
            "cl_rate": 2.0,
            "tail_k_threshold": 30,
            "m_h_eps": 0.001,
            "kl_eps": 10.0,
            "neighbor_norm_type": "left",
            "annealing_type": 1,
            "agg_function": 0,
            "lambda_gp": 1.0,
            "use_relation_rf": True,
            "agg_w_exp_scale": 20.0,
            "l2_reg": 0.0001,
        }
    elif args.dataset_name == "ml-25m":
        default_params = {
            "dataset_name": "ml-25m",
            "batch_size": 2048,
            "n_hops": 2,
            "user_num": 153315,
            "item_num": 25121,
            "hidden_size": 64,
            "learning_rate": 0.001,
            "metric_topk": 20,
            "epoch": 6,
            "noise_eps": 0.2,
            "cl_tau": 0.2,
            "cl_rate": 0.2,
            "tail_k_threshold": 18,
            "m_h_eps": 0.1,
            "kl_eps": 0.1,
            "neighbor_norm_type": "both",
            "annealing_type": 2,
            "agg_function": 0,
            "lambda_gp": 1.0,
            "use_relation_rf": True,
            "agg_w_exp_scale": 20.0,
            "l2_reg": 0.0001,
        }
    else:
        raise NotImplementedError(
            f"args.dataset_name not supported: {args.dataset_name}"
        )
    print("default params: ", default_params)
    params_changed_num = 0
    for key, value in args.__dict__.items():
        if (
            key in default_params
            and key not in ["dataset_name"]
            and str(value) != "-1"
            and default_params[key] != value
        ):
            print(f"default params ({key}): {default_params[key]} ---> {value}")
            default_params[key] = value
            params_changed_num += 1
    if params_changed_num > 0:
        print("params changed num: ", params_changed_num)
        print("final params: ", default_params)
    else:
        print("params no changed.")
    return default_params


def main():
    params = build_argparse()
    use_wandb = os.environ.get("use_wandb", False)

    # step 1: 构建dataset
    script_dir = os.path.dirname(os.path.abspath(__file__))
    dataset_name = f'data_process/datasets/{params["dataset_name"]}/full_batch_datasets'
    graph_file_name = os.path.join(script_dir, dataset_name, "full_graph_feature.csv")

    full_graph_dataset = AGLTorchMapBasedDataset(
        graph_file_name,
        column_sep=",",
        has_schema=True,
        schema=["seed", "graph_feature", "node_id_list"],
    )

    # step 2: 构建collate function
    # node related spec
    node_spec = NodeSpec("default", AGLDType.STR)
    node_spec.AddDenseSpec(
        "node_feature", DenseFeatureSpec("node_feature", 1, AGLDType.INT64)
    )
    node_spec.AddDenseSpec(
        "node_degree", DenseFeatureSpec("node_degree", 1, AGLDType.INT64)
    )
    node_spec.AddDenseSpec(
        "node_type", DenseFeatureSpec("node_type", 1, AGLDType.INT64)
    )
    # edge related spec
    edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)

    node_id_column = AGLRowColumn(name="node_id_list")
    my_collate = AGLHomoCollateForPyG(
        node_spec,
        edge_spec,
        columns=[node_id_column],
        ego_edge_index=False,
        need_node_and_edge_num=True,
    )

    # step 3: 构建 dataloader
    # train loader
    full_graph_dataloader = DataLoader(
        dataset=full_graph_dataset,
        batch_size=1,
        shuffle=False,
        collate_fn=my_collate,
        num_workers=4,
        persistent_workers=True,
    )
    graph = list(full_graph_dataloader)[0]

    # step 4: 加载额外所需信息
    training_set_u, training_set_i, test_set = pickle.load(
        open(os.path.join(script_dir, dataset_name, "ui_link_info.pickle"), "rb")
    )
    # 读取包含 label 的训练样本，并按照
    training_data = (
        pd.read_csv(os.path.join(script_dir, dataset_name, "sample_table.csv"))
        .query("label == 1")[["node1_id", "node2_id"]]
        .values.tolist()
    )
    node_table = pd.read_csv(os.path.join(script_dir, dataset_name, "node_table.csv"))
    node_table = node_table.set_index("node_id").to_dict(orient="index")
    node_raw_idx_mapping = {
        key: value["node_feature"] for key, value in node_table.items()
    }
    id2item = {
        value["node_feature"] - params["user_num"]: key
        for key, value in node_table.items()
        if "itemid" in key
    }

    # step 5: 模型相关以及训练与测试
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = LAGCLLinkPredictionModel(
        graph=graph,
        user_num=params["user_num"],
        item_num=params["item_num"],
        hidden_dim=params["hidden_size"],
        n_hops=params["n_hops"],
        noise_eps=params["noise_eps"],
        cl_tau=params["cl_tau"],
        cl_rate=params["cl_rate"],
        tail_k_threshold=params["tail_k_threshold"],
        m_h_eps=params["m_h_eps"],
        kl_eps=params["kl_eps"],
        l2_reg=params["l2_reg"],
        neighbor_norm_type=params["neighbor_norm_type"],
        agg_function=params["agg_function"],
        annealing_type=params["annealing_type"],
        lambda_gp=params["lambda_gp"],
        use_relation_rf=params["use_relation_rf"],
        agg_w_exp_scale=params["agg_w_exp_scale"],
        device=device,
    )

    optimizer = torch.optim.Adam(model.parameters(), lr=params["learning_rate"])
    disc_model = Discriminator(params["hidden_size"]).to(device)
    disc_pseudo_real = Discriminator(params["hidden_size"]).to(device)
    optimizer_D = torch.optim.RMSprop(
        disc_model.parameters(), lr=params["learning_rate"] * 0.1
    )
    optimizer_D_pseudo_real = torch.optim.RMSprop(
        disc_pseudo_real.parameters(), lr=params["learning_rate"] * 0.1
    )

    if use_wandb:
        wandb.init(project="agl_opensource_tailgnn", config=params)
        wandb.watch(model)
    print("---> params: ", params)

    def train():
        model.to(device)
        model.train()

        iter_num = (len(training_data) - 1) // params["batch_size"] + 1
        discD_every_iter = iter_num // 36
        discG_every_iter = discD_every_iter * 4
        print(
            f"iter_num: {iter_num}, discD every iter: {discD_every_iter}, discG every iter: {discG_every_iter}"
        )

        train_loader = next_batch_pairwise(
            training_data,
            node_table,
            training_set_u=training_set_u,
            training_set_i=training_set_i,
            batch_size=params["batch_size"],
        )
        pbar = tqdm(train_loader, total=iter_num)
        total_loss = 0
        for n, data in enumerate(pbar):
            optimizer.zero_grad()
            _, loss = model(data)
            total_loss += loss.item()
            loss.backward()
            optimizer.step()
            pbar.set_postfix({"loss": total_loss / (n + 1)})

            if n % discD_every_iter == 0:
                # todo: 直接 full batch 4:1 训练这两部分
                model.train_disc(
                    model,
                    disc_model,
                    optimizer_D,
                    disc_pseudo_real,
                    optimizer_D_pseudo_real,
                    n,
                    iter_num,
                )
                if n % discG_every_iter == 0 and n > 0:
                    model.train_gen(model, optimizer, disc_model, disc_pseudo_real)
        return total_loss / iter_num

    def test():
        model.eval()
        topk = params["metric_topk"]
        rec_list = {}

        with torch.no_grad():
            (user_embs, item_embs), _, _ = model.process_one_subgraph()
            for user in tqdm(test_set):
                user_embedding = user_embs[node_raw_idx_mapping[user]]
                item_embeddings = item_embs
                candidates = (
                    torch.matmul(user_embedding, item_embeddings.t())
                    .detach()
                    .cpu()
                    .numpy()
                )
                for item in training_set_u[user].keys():
                    candidates[node_raw_idx_mapping[item] - model.user_num] = -10e8

                from metrics import find_k_largest, ranking_evaluation

                ids, scores = find_k_largest(topk, candidates)
                item_names = [id2item[iid] for iid in ids]
                rec_list[user] = list(zip(item_names, scores))

        metrics_res = ranking_evaluation(test_set, rec_list, topk)
        print("test: ", metrics_res)
        recall_20 = metrics_res["Recall"]
        ndcg_20 = metrics_res["NDCG"]
        return recall_20, ndcg_20

    best_metrics = {}
    for epoch in range(1, params["epoch"] + 1):
        t0 = time.time()
        loss = train()
        t1 = time.time()
        recall, ndcg = test()
        print("test: ", recall, ndcg)
        best_metrics["Recall@20"] = max(best_metrics.get("Recall@20", 0.0), recall)
        best_metrics["NDCG@20"] = max(best_metrics.get("NDCG@20", 0.0), ndcg)
        t2 = time.time()
        print(
            "Epoch: {:02d}, Loss: {:.4f}, Recall@20: {:.4f}, NDCG@20: {:.4f}, best Recall@20: {:.4f}, best NDCG@20: {:.4f}, train_time: {:4f}, val_time: {:4f}".format(
                epoch,
                loss,
                recall,
                ndcg,
                best_metrics["Recall@20"],
                best_metrics["NDCG@20"],
                t1 - t0,
                t2 - t1,
            )
        )

        if use_wandb:
            wandb.log(
                {
                    "epoch": epoch - 1,
                    "Recall@20": recall,
                    "NDCG@20": ndcg,
                }
            )
            wandb.summary.update(best_metrics)
    print("best metrics: ", best_metrics)


if __name__ == "__main__":
    main()
