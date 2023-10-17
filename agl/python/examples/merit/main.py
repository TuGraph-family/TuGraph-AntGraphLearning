import os
import sys
import time
import random

import torch
import numpy as np
import pandas as pd
from torch.utils.data import DataLoader
from sklearn.metrics import average_precision_score
from sklearn.metrics import roc_auc_score

from agl.python.data.subgraph.pyg_inputs import (
    TorchSubGraphBatchData,
    TorchFeatures,
    TorchEdgeIndex,
)
from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.multi_graph_feature_collate import MultiGraphFeatureCollate
from agl.python.data.column import AGLDenseColumn, AGLRowColumn
from pyagl import (
    AGLDType,
    DenseFeatureSpec,
    SparseKVSpec,
    SparseKSpec,
    NodeSpec,
    EdgeSpec,
    SubGraph,
    NDArray,
)

from merit import MERITModel
from tgat import TGATModel
from utils import EarlyStopMonitor
from utils import parse_args

args = parse_args()
random.seed(2020)

# step 1: read dataset
train_file_name = "./data_process/wiki_train.csv"
valid_file_name = "./data_process/wiki_valid.csv"
test_file_name = "./data_process/wiki_test.csv"

g_df = pd.read_csv("./data_process/processed/ml_wikipedia.csv")
max_time_interval = 0
for i in range(1, g_df.u.max() + 1):
    temp = g_df[g_df.u == i].ts.diff()
    temp = temp[temp > 0].tolist()
    if len(temp) == 0:
        continue
    else:
        max_val = max(temp)
    if max_val > max_time_interval:
        max_time_interval = max_val
log_max = np.log(max_time_interval)

script_dir = os.path.dirname(os.path.abspath(__file__))
train_file_name = os.path.join(script_dir, train_file_name)
test_file_name = os.path.join(script_dir, test_file_name)

# train data set and test data set
train_data_set = AGLTorchMapBasedDataset(
    train_file_name,
    format="csv",
    has_schema=False,
    column_sep=",",
    schema=["node1_id", "node2_id", "graph_feature", "time", "label"],
)
valid_data_set = AGLTorchMapBasedDataset(
    test_file_name,
    format="csv",
    has_schema=False,
    column_sep=",",
    schema=["node1_id", "node2_id", "graph_feature", "time", "label"],
)
test_data_set = AGLTorchMapBasedDataset(
    test_file_name,
    format="csv",
    has_schema=False,
    column_sep=",",
    schema=["node1_id", "node2_id", "graph_feature", "time", "label"],
)

MODEL_SAVE_PATH = "./saved_models/{}.pth".format(args.model)
get_checkpoint_path = lambda epoch: f"./saved_checkpoints/{args.model}-{epoch}.pth"

if not os.path.exists("./saved_models"):
    os.makedirs("./saved_models")
if not os.path.exists("./saved_checkpoints"):
    os.makedirs("./saved_checkpoints")

# step 2: build collate function
# node related spec
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddDenseSpec("node_id", DenseFeatureSpec("node_id", 1, AGLDType.INT64))
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddDenseSpec("edge_id", DenseFeatureSpec("edge_id", 1, AGLDType.INT64))
edge_spec.AddDenseSpec("time", DenseFeatureSpec("time", 1, AGLDType.FLOAT))

node1_id_column = AGLRowColumn(name="node1_id")
node2_id_column = AGLRowColumn(name="node2_id")
time_column = AGLDenseColumn(name="time", dim=1, dtype=np.float32)
label_column = AGLDenseColumn(name="label", dim=1, dtype=np.float32)


def get_temporal_neighbor(subgraph: TorchSubGraphBatchData):
    root_index: torch.Tensor = subgraph.root_index
    n_feats: TorchFeatures = subgraph.n_feats
    e_feats: TorchFeatures = subgraph.e_feats
    adj: TorchEdgeIndex = subgraph.adjs_t
    cut_time = subgraph.other_feats["time"]
    # todo 从 model 中获取
    num_ngh = 10
    # todo 从 model 中获取
    layer = 2

    def get_temporal_neighbor_inner(
        node_indices: np.ndarray,
        n_feats: TorchFeatures,
        e_feats: TorchFeatures,
        adj: TorchEdgeIndex,
        num_ngh,
    ):
        ngh_node_indices = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_edge_times = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_edge_indices = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_node_ids = np.zeros((len(node_indices), num_ngh)).astype(np.int32)
        ngh_edge_ids = np.zeros((len(node_indices), num_ngh)).astype(np.int32)

        row_ptr = adj.row_ptr.cpu().numpy()
        col = adj.col.cpu().numpy()
        edge_indices = adj.edge_indices.cpu().numpy()

        for i, node_idx in enumerate(node_indices):
            i_ngh_range = range(row_ptr[node_idx], row_ptr[node_idx + 1])
            i_ngh_num = len(i_ngh_range)
            if i_ngh_num >= num_ngh:
                i_ngh_num = num_ngh
            i_ngh_node_indices = col[i_ngh_range][:i_ngh_num]
            i_ngh_edge_indices = edge_indices[i_ngh_range][:i_ngh_num]
            i_ngh_edge_times = (
                e_feats.features["time"]
                .to_dense()
                .cpu()
                .numpy()[i_ngh_edge_indices]
                .reshape([-1])
            )

            # sort neighbors by time
            if len(i_ngh_edge_times) > 0:
                i_ngh_edge_times, i_ngh_node_indices, i_ngh_edge_indices = list(
                    zip(
                        *sorted(
                            zip(
                                i_ngh_edge_times, i_ngh_node_indices, i_ngh_edge_indices
                            )
                        )
                    )
                )
                i_ngh_edge_times = list(i_ngh_edge_times)
                i_ngh_node_indices = list(i_ngh_node_indices)
                i_ngh_edge_indices = list(i_ngh_edge_indices)

                i_ngh_node_ids = (
                    n_feats.features["node_id"]
                    .to_dense()
                    .cpu()
                    .numpy()[i_ngh_node_indices]
                    .reshape([-1])
                )
                i_ngh_edge_ids = (
                    e_feats.features["edge_id"]
                    .to_dense()
                    .cpu()
                    .numpy()[i_ngh_edge_indices]
                    .reshape([-1])
                )

                ngh_node_indices[i][:i_ngh_num] = i_ngh_node_indices
                ngh_edge_times[i][:i_ngh_num] = i_ngh_edge_times
                ngh_edge_indices[i][:i_ngh_num] = i_ngh_edge_indices
                ngh_node_ids[i][:i_ngh_num] = i_ngh_node_ids
                ngh_edge_ids[i][:i_ngh_num] = i_ngh_edge_ids

        return ngh_node_indices, ngh_node_ids, ngh_edge_ids, ngh_edge_times

    root_index_npy = root_index.cpu().numpy()
    cur_time = cut_time.cpu().numpy()

    node_indices = root_index_npy
    curr_time = np.repeat(cur_time, 2, axis=0)
    for i_l in range(layer):
        (
            ngh_node_indices,
            ngh_node_ids,
            ngh_edge_ids,
            ngh_edge_times,
        ) = get_temporal_neighbor_inner(node_indices, n_feats, e_feats, adj, num_ngh)
        ngh_time_delta = curr_time - ngh_edge_times

        subgraph.other_feats.update(
            {
                f"ngh_node_indices_{layer - 1 - i_l}": torch.from_numpy(
                    ngh_node_indices
                ).long(),
                f"ngh_node_ids_{layer - 1 - i_l}": torch.from_numpy(
                    ngh_node_ids
                ).long(),
                f"ngh_edge_ids_{layer - 1 - i_l}": torch.from_numpy(
                    ngh_edge_ids
                ).long(),
                f"ngh_edge_times_{layer - 1 - i_l}": torch.from_numpy(
                    ngh_edge_times
                ).float(),
                f"ngh_time_delta_{layer - 1 - i_l}": torch.from_numpy(
                    ngh_time_delta
                ).float(),
            }
        )
        # (batch_size, -1)
        ngh_node_indices_flat = ngh_node_indices.flatten()
        # (batch_size, -1)
        ngh_edge_times_flat = np.reshape(ngh_edge_times.flatten(), [-1, 1])
        # 下一次迭代的起点
        node_indices = ngh_node_indices_flat
        # 下一次计算迭代的 curr_time, 用于计算delta_time
        curr_time = ngh_edge_times_flat
    return subgraph


my_collate = MultiGraphFeatureCollate(
    node_spec,
    edge_spec,
    uncompress=True,
    columns=[node1_id_column, node2_id_column, time_column, label_column],
    label_name="label",
    after_transform=get_temporal_neighbor,
)

# step 3: build dataloader
# train loader
train_loader = DataLoader(
    dataset=train_data_set,
    batch_size=200,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=4,
    persistent_workers=True,
)

valid_loader = DataLoader(
    dataset=valid_data_set,
    batch_size=200,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=4,
    persistent_workers=True,
)

test_loader = DataLoader(
    dataset=test_data_set,
    batch_size=200,
    shuffle=False,
    collate_fn=my_collate,
    num_workers=4,
    persistent_workers=True,
)

# step 4: model train
e_feat = np.load("./data_process/processed/ml_wikipedia.npy")
n_feat = np.load("./data_process/processed/ml_wikipedia_node.npy")
if args.model == "merit":
    model = MERITModel(
        n_feat,
        e_feat,
        node_dim=100,
        num_layers=2,
        seq_len=10,
        n_head=3,
        drop_out=0.4,
        d=100,
        fourier_basis=5,
        log_max=log_max,
        kernel_size=3,
        context_type=args.agg_type,
    )
elif args.model == "tgat":
    model = TGATModel(n_feat, e_feat, num_layers=2, n_head=3, drop_out=0.4, num_ngh=10)
else:
    print("unknown model: {}".format(args.model))
    sys.exit(-1)

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
print("device: ", device)
loss_op = torch.nn.BCELoss()
optimizer = torch.optim.Adam(model.parameters(), lr=0.0001)


def train(train_loader):
    model.to(device)
    model.train()

    total_loss = 0
    i = 0
    for j, data in enumerate(train_loader):
        t1 = time.time()
        data = data.to(device)
        optimizer.zero_grad()
        loss = loss_op(model(data), data.y.to(torch.float32).view([-1]))
        total_loss += loss.item()
        i = i + 1
        loss.backward()
        optimizer.step()
        t2 = time.time()
    return total_loss / i


def test(loader):
    model.eval()
    acc, ap, auc = [], [], []
    with torch.no_grad():
        for data in loader:
            data_gpu = data.to(device)
            pred_score = model(data_gpu).cpu().numpy()
            pred_label = (pred_score > 0.5).astype(float)
            true_label = np.reshape(data.y.cpu().numpy(), [-1])

            acc.append((pred_label == true_label).mean())
            ap.append(average_precision_score(true_label, pred_score))
            auc.append(roc_auc_score(true_label, pred_score))

    return np.mean(acc), np.mean(ap), np.mean(auc)


early_stopper = EarlyStopMonitor()
for epoch in range(1, 50):
    t0 = time.time()
    loss = train(train_loader)
    t1 = time.time()

    # validation phase use all information
    val_acc, val_ap, val_auc = test(valid_loader)

    t2 = time.time()
    print(
        "Epoch: {:02d}, Loss: {:.4f}, acc: {:.4f}, auc: {:.4f}, ap: {:.4f}, train_time: {:4f}, val_time: {:4f}".format(
            epoch, loss, val_acc, val_auc, val_ap, t1 - t0, t2 - t1
        )
    )

    if early_stopper.early_stop_check(val_ap):
        print(
            "No improvment over {} epochs, stop training".format(
                early_stopper.max_round
            )
        )
        print(f"Loading the best model at epoch {early_stopper.best_epoch}")
        best_model_path = get_checkpoint_path(early_stopper.best_epoch)
        model.load_state_dict(torch.load(best_model_path))
        print(
            f"Loaded the best model at epoch {early_stopper.best_epoch} for inference"
        )
        model.eval()
        break
    else:
        torch.save(model.state_dict(), get_checkpoint_path(epoch))

# testing phase use all information
test_acc, test_ap, test_auc = test(test_loader)
print(
    "Test statistics: acc: {:.4f}, auc: {:.4f}, ap: {:.4f}".format(
        test_acc, test_auc, test_ap
    )
)

print("Saving {} model".format(args.model))
torch.save(model.state_dict(), MODEL_SAVE_PATH)
print("{} model saved".format(args.model))
