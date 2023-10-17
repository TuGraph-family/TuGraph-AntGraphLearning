import os
import time
import argparse
import numpy as np

import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader
from torch_geometric.nn import GCN

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn
from pyagl import AGLDType, SparseKVSpec, NodeSpec, EdgeSpec

from agl.python.model.utils.nasa_utils import *


def setup_seed(seed=2023):
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    np.random.seed(seed)
    random.seed(seed)
    torch.backends.cudnn.deterministic = True


setup_seed()

parser = argparse.ArgumentParser()
parser.add_argument("--lr", type=float, default=0.01, help="Initial learning rate.")
parser.add_argument("--weight_decay", type=float, default=1e-3, help="Weight decay")
parser.add_argument("--hidden", type=int, default=256, help="Number of hidden units.")
parser.add_argument("--dropout", type=float, default=0.1, help="Dropout rate")
parser.add_argument("--alpha", type=float, default=0.05, help="loss hyperparameter")
parser.add_argument("--temp", type=float, default=0.1, help="sharpen temperature")
parser.add_argument("--in_channels", type=int, default=745, help="in channels of GNN")
parser.add_argument("--out_channels", type=int, default=8, help="out channels of GNN")
parser.add_argument("--num_layers", type=int, default=2, help="layer number of GNN")
parser.add_argument("--max_epoch", type=int, default=300, help="max training epoch")
args = parser.parse_args()

# step 1: 构建dataset
train_file_name = "data_process/graph_features.csv"
script_dir = os.path.dirname(os.path.abspath("./nasa/"))
train_file_path = os.path.join(script_dir, train_file_name)

train_dataset = AGLTorchMapBasedDataset(
    train_file_path,
    format="csv",
    column_sep=",",
    has_schema=True,
)

# step 2: 构建collate function
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddSparseKVSpec(
    "sparse_kv", SparseKVSpec("sparse_kv", 745, AGLDType.INT64, AGLDType.FLOAT)
)
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddSparseKVSpec(
    "sparse_kv", SparseKVSpec("sparse_kv", 1, AGLDType.INT64, AGLDType.FLOAT)
)

label_column = AGLDenseColumn(name="label_list", dim=7650, dtype=np.int64, sep="\t")
id_column = AGLDenseColumn(name="node_id_list", dim=7650, dtype=np.int64, sep="\t")
flag_column = AGLDenseColumn(name="train_flag_list", dim=7650, dtype=np.int64, sep="\t")
my_collate = AGLHomoCollateForPyG(
    node_spec,
    edge_spec,
    columns=[label_column, id_column, flag_column],
    uncompress=True,
)

# step 3: 构建 dataloader
train_loader = DataLoader(
    dataset=train_dataset,
    batch_size=1,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=0,
)

# step 4: 模型相关以及训练与测试
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = GCN(
    in_channels=args.in_channels,
    hidden_channels=args.hidden,
    num_layers=args.num_layers,
    out_channels=args.out_channels,
    dropout=args.dropout,
)
optimizer = torch.optim.Adam(
    model.parameters(), lr=args.lr, weight_decay=args.weight_decay
)
ce_loss = torch.nn.BCEWithLogitsLoss(reduction="mean")
cr_loss = NeighborConstrainedRegLoss(args.temp)


# step 4.1: 模型训练
def train(loader):
    model.to(device)
    model.train()

    total_loss, i = 0, 0
    for j, subgraph in enumerate(loader):
        # step 4.1.0: 梯度清零
        optimizer.zero_grad()

        # step 4.1.1: 准备当前batch数据
        subgraph = subgraph.to(device)
        train_mask = subgraph.other_feats["train_flag_list"] == 0
        x = subgraph.n_feats.features["sparse_kv"].get().to_dense()
        aug_edge_index = neighbor_replace_aug(subgraph)

        # step 4.1.2: 计算节点表征
        aug_pred = model(x=x, edge_index=aug_edge_index)

        # step 4.1.3: 节点表征后处理，用于Loss计算
        softmax_aug_pred = F.softmax(aug_pred, 1)
        aug_pred = aug_pred[subgraph.root_index][train_mask.squeeze(), :]
        y = (
            F.one_hot(
                subgraph.other_feats["label_list"].squeeze(1)[train_mask],
                num_classes=args.out_channels,
            )
            .to(device)
            .to(torch.float32)
        )

        # step 4.1.3: 计算Loss
        loss1 = ce_loss(aug_pred, y)
        loss2 = cr_loss(aug_edge_index, softmax_aug_pred)
        loss = loss1 + args.alpha * loss2

        # step 4.1.4: 参数优化
        loss.backward()
        optimizer.step()

        total_loss += loss.item()
        i += 1

    return total_loss / i


# step 4.2: 模型测试
def test(loader, flag="test"):
    model.eval()

    ys, preds = [], []
    for data in loader:
        with torch.no_grad():
            # step 4.2.1: 准备当前batch数据
            subgraph = data.to(device)
            x = subgraph.n_feats.features["sparse_kv"].get().to_dense()
            edge_index = subgraph.adjs_t.edge_index

            # step 4.2.2: 计算节点表征
            aug_pred = model(x=x, edge_index=edge_index)
            softmax_aug_pred = F.softmax(aug_pred, 1)

        # step 4.1.3: 数据后处理，用于计算评估指标
        mask = data.other_feats["train_flag_list"].squeeze(0)
        if flag == "train":
            mask = mask == 0
        if flag == "val":
            mask = mask == 1
        if flag == "test":
            mask = mask == 2
        preds.append(softmax_aug_pred[data.root_index][mask, :])
        ys.append(subgraph.other_feats["label_list"][mask.unsqueeze(0)])

    # step 4.1.4: 计算评估指标
    final_pred, final_y = torch.cat(preds, dim=0), torch.cat(ys, dim=0)
    acc = metric_accuracy(final_pred, final_y)
    return acc


print("Training!")
for epoch in range(args.max_epoch):
    t0 = time.time()

    loss = train(train_loader)
    t1 = time.time()

    train_f1 = test(train_loader, flag="train")
    val_f1 = test(train_loader, flag="val")
    test_f1 = test(train_loader)

    t2 = time.time()
    print(
        "Epoch: {:02d}, Loss: {:.4f}, train_time: {:4f}, val_time: {:4f}".format(
            epoch, loss, t1 - t0, t2 - t1
        ),
        end="\t",
    )
    print(
        "train_f1: {:4f}, val_f1: {:.4f}, test_f1: {:.4f}".format(
            train_f1, val_f1, test_f1
        ),
        end="\n",
    )
