import torch
import torch.nn.functional as F
import torch.nn as nn


def bpr_loss(user_emb, pos_item_emb, neg_item_emb, reduction='mean'):
    pos_score = torch.mul(user_emb, pos_item_emb).sum(dim=1)
    neg_score = torch.mul(user_emb, neg_item_emb).sum(dim=1)
    loss = -torch.log(10e-8 + torch.sigmoid(pos_score - neg_score))

    if reduction == 'sum':
        reduce_fun = torch.sum
    elif reduction == 'mean':
        reduce_fun = torch.mean
    else:
        reduce_fun = lambda x: x
    return reduce_fun(loss)


def l2_reg_loss(reg, *args):
    emb_loss = 0
    for emb in args:
        emb_loss += torch.norm(emb, p=2)
    return emb_loss * reg


def InfoNCE(view1, view2, temperature, view_all=None):
    """
        view1: view 1 in contrastive learning.
        view2: view 2 in contrastive learning.
        temperature: hyperparameters tau in InfoNCE.
    """
    view1 = F.normalize(view1, dim=1)
    view2 = F.normalize(view2, dim=1)
    if view_all is None:
        view_all = view2
    else:
        view_all = F.normalize(view_all, dim=1)
    pos_score = (view1 * view2).sum(dim=-1)
    pos_score = torch.exp(pos_score / temperature)
    ttl_score = torch.matmul(view1, view_all.transpose(0, 1))
    ttl_score = torch.exp(ttl_score / temperature).sum(dim=1)
    cl_loss = -torch.log(pos_score / ttl_score)
    return torch.mean(cl_loss)
