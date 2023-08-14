from typing import List, Tuple, Optional, Union, Dict

import os
import sys
import time
import torch
import random
import numpy as np
import pandas as pd
from torch import Tensor
from torch.utils.data import DataLoader
from torch_geometric.nn import GATConv
from torch_geometric.typing import OptPairTensor
from sklearn import metrics
from sklearn.metrics import average_precision_score
from sklearn.metrics import f1_score
from sklearn.metrics import roc_auc_score

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn, AGLMultiDenseColumn
from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray
from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData

from merit import MERITModel
from tgat import TGATModel
from utils import EarlyStopMonitor
from utils import parse_args


args = parse_args()
random.seed(2020)

# step 1: read dataset
train_file_name = "wiki_train.csv"
valid_file_name = "wiki_valid.csv"
test_file_name = "wiki_test.csv"

g_df = pd.read_csv('./ml_wikipedia.csv')
max_time_interval = 0
for i in range(1, g_df.u.max()+1):
    temp = g_df[g_df.u==i].ts.diff()
    temp = temp[temp>0].tolist()
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
        schema=["node1_id", "node2_id", "graph_feature", "time", "label"])
valid_data_set = AGLTorchMapBasedDataset(
        test_file_name,
        format="csv",
        has_schema=False,
        column_sep=",",
        schema=["node1_id", "node2_id", "graph_feature", "time", "label"])
test_data_set = AGLTorchMapBasedDataset(
        test_file_name,
        format="csv",
        has_schema=False,
        column_sep=",",
        schema=["node1_id", "node2_id", "graph_feature", "time", "label"])

MODEL_SAVE_PATH = './saved_models/{}.pth'.format(args.model)
get_checkpoint_path = lambda epoch: f'./saved_checkpoints/{args.model}-{epoch}.pth'

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
my_collate = AGLHomoCollateForPyG(
        node_spec,
        edge_spec, 
        uncompress=True,
        columns=[node1_id_column, node2_id_column, time_column, label_column], 
        label_name="label")


# step 3: build dataloader
# train loader
train_loader = DataLoader(dataset=train_data_set,
                          batch_size=200,
                          shuffle=False,
                          collate_fn=my_collate,
                          num_workers=2,
                          persistent_workers=True)

valid_loader = DataLoader(dataset=valid_data_set,
                          batch_size=200,
                          shuffle=False,
                          collate_fn=my_collate,
                          num_workers=1,
                          persistent_workers=True)

test_loader = DataLoader(dataset=test_data_set,
                         batch_size=200,
                         shuffle=False,
                         collate_fn=my_collate,
                         num_workers=1,
                         persistent_workers=True)

# step 4: model train
e_feat = np.load('./ml_wikipedia.npy')
n_feat = np.load('./ml_wikipedia_node.npy')
if args.model == "merit":
    model = MERITModel(
            n_feat, e_feat, node_dim=100, num_layers=2, seq_len=10, n_head=3, drop_out=0.4, d=100,
            fourier_basis=5, log_max=log_max, kernel_size=3, context_type=args.agg_type)
elif args.model == "tgat":
    model = TGATModel(n_feat, e_feat, num_layers=2, n_head=3, drop_out=0.4, num_ngh=10)
else:
    print("unknown model: {}".format(args.model))
    sys.exit(-1)

device = torch.device('cuda:0' if torch.cuda.is_available() else 'cpu')
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
        i = i+1
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
    print('Epoch: {:02d}, Loss: {:.4f}, acc: {:.4f}, auc: {:.4f}, ap: {:.4f}, train_time: {:4f}, val_time: {:4f}'.format(
                epoch, loss, val_acc, val_auc, val_ap, t1-t0, t2-t1))

    if early_stopper.early_stop_check(val_ap):
        print('No improvment over {} epochs, stop training'.format(early_stopper.max_round))
        print(f'Loading the best model at epoch {early_stopper.best_epoch}')
        best_model_path = get_checkpoint_path(early_stopper.best_epoch)
        model.load_state_dict(torch.load(best_model_path))
        print(f'Loaded the best model at epoch {early_stopper.best_epoch} for inference')
        model.eval()
        break
    else:
        torch.save(model.state_dict(), get_checkpoint_path(epoch))

# testing phase use all information
test_acc, test_ap, test_auc = test(test_loader)
print('Test statistics: acc: {:.4f}, auc: {:.4f}, ap: {:.4f}'.format(test_acc, test_auc, test_ap))

print('Saving {} model'.format(args.model))
torch.save(model.state_dict(), MODEL_SAVE_PATH)
print('{} model saved'.format(args.model))
