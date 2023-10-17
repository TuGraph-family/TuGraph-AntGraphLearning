import time

import numpy as np
import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn
from agl.python.model.encoder.drgst import DRGSTEncoder
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


class DRGSTModel(torch.nn.Module):
    def __init__(self, feats_dim: int, hidden_dim: int, out_dim: int, k_hops: int):
        super().__init__()

        # encoder layer
        self._encoder = DRGSTEncoder(
            feats_dim=feats_dim, hidden_dim=hidden_dim, out_dim=out_dim, k_hops=k_hops
        )

    def forward(self, subgraph):
        features = subgraph.n_feats.features["sparse_kv"].get().to_dense()
        output = self._encoder(subgraph, features)
        return output

    def reset_dropout(self, droprate):
        self._encoder.reset_dropout(droprate)


def accuracy(pred, targ):
    pred = torch.softmax(pred, dim=1)
    pred_max_index = torch.max(pred, 1)[1]
    ac = ((pred_max_index == targ).float()).sum().item() / targ.size()[0]
    return ac


def weighted_cross_entropy(ig, preds, labels, beta, num_class):
    ig += 1e-6
    output = torch.softmax(preds, dim=1)
    ig = ig / (torch.mean(ig) * beta)
    labels = F.one_hot(labels, num_class)
    loss = -torch.log(torch.sum(output * labels, dim=1))
    loss = torch.sum(loss * ig)
    loss /= labels.size()[0]
    return loss


def information_gain(model, data_loader, ig, config, device, seed_name="seed"):
    out_list = torch.zeros([config[1], config[0], config[2]]).to(device)
    model.reset_dropout(config[5])
    out_list = torch.tensor(out_list, dtype=torch.float32)
    with torch.no_grad():
        for index_forward in range(config[1]):
            for j, data in enumerate(data_loader):
                data = data.to(device)
                seed = data.other_feats[seed_name].squeeze()
                preds = model(data)
                preds = torch.softmax(preds[data.root_index], dim=1)
                out_list[index_forward][seed] = preds
        index = torch.where(out_list[0].sum(dim=1) > 0.5)[0]
        out_list = out_list[:, index, :]
        out_mean = torch.mean(out_list, dim=0)
        entropy = torch.sum(torch.mean(out_list * torch.log(out_list), dim=0), dim=1)
        Eentropy = torch.sum(out_mean * torch.log(out_mean), dim=1)
        ig[index] = entropy - Eentropy
    model.reset_dropout(0.5)
    return ig


def generate_pseudo_label(
    model, data_loader, pseudo_mask, pseudo_labels, config, device, seed_name="seed"
):
    threshold = config[3]
    with torch.no_grad():
        for j, data in enumerate(data_loader):
            data = data.to(device)
            seed = data.other_feats[seed_name].squeeze()
            preds = model(data)
            preds = torch.softmax(preds, dim=1)
            confidence, pseudo_label = torch.max(preds[data.root_index], dim=1)
            change_index = seed[confidence > threshold]
            pseudo_mask[change_index] = True
            pseudo_labels[change_index] = pseudo_label[confidence > threshold]
    return pseudo_mask, pseudo_labels


def main():
    # step 1: 构建dataset
    train_file_name = "data_process/graph_feature_1.csv"
    test_file_name = "data_process/graph_feature_0.csv"
    val_file_name = "data_process/graph_feature_2.csv"
    unlabel_file_name = "data_process/graph_feature_-1.csv"

    # 数据集参数及模型超参数
    num_node = 3327
    num_forward = 50
    num_class = 6
    threshold = 0.7
    beta = 1 / 3
    droprate = 0.5
    config = [num_node, num_forward, num_class, threshold, beta, droprate]

    # train data set, val data set and test data set
    train_data_set = AGLTorchMapBasedDataset(
        train_file_name, has_schema=True, column_sep=","
    )
    val_data_set = AGLTorchMapBasedDataset(
        val_file_name, has_schema=True, column_sep=","
    )
    test_data_set = AGLTorchMapBasedDataset(
        test_file_name, has_schema=True, column_sep=","
    )
    unlabel_data_set = AGLTorchMapBasedDataset(
        unlabel_file_name, has_schema=True, column_sep=","
    )

    # step 2: 构建collate function
    # node related spec
    node_spec = NodeSpec("default", AGLDType.STR)
    node_spec.AddSparseKVSpec(
        "sparse_kv", SparseKVSpec("sparse_kv", 3703, AGLDType.INT64, AGLDType.FLOAT)
    )
    # edge related spec
    edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)

    graph_id_column = AGLDenseColumn(name="seed", dim=1, dtype=np.int64)
    root_id_column = AGLRowColumn(name="node_id")
    label_column = AGLDenseColumn(name="label", dim=6, dtype=np.int64, sep=" ")
    train_flag_column = AGLRowColumn(name="train_flag")

    my_collate = AGLHomoCollateForPyG(
        node_spec,
        edge_spec,
        columns=[graph_id_column, root_id_column, label_column, train_flag_column],
        graph_feature_name="graph_feature",
        label_name="label",
        hops=2,
        uncompress=True,
    )

    # step 3: 构建 dataloader
    # train loader
    train_loader = DataLoader(
        dataset=train_data_set,
        batch_size=128,
        shuffle=True,
        collate_fn=my_collate,
        num_workers=3,
        persistent_workers=True,
    )

    val_loader = DataLoader(
        dataset=val_data_set,
        batch_size=500,
        shuffle=True,
        collate_fn=my_collate,
        num_workers=3,
        persistent_workers=True,
    )

    test_loader = DataLoader(
        dataset=test_data_set,
        batch_size=1000,
        shuffle=False,
        collate_fn=my_collate,
        num_workers=3,
        persistent_workers=True,
    )

    unlabel_loader = DataLoader(
        dataset=unlabel_data_set,
        batch_size=500,
        shuffle=True,
        collate_fn=my_collate,
        num_workers=3,
        persistent_workers=True,
    )

    # step 4: 模型相关以及训练与测试
    model = DRGSTModel(feats_dim=3703, hidden_dim=128, out_dim=6, k_hops=2)
    print(model)
    model_path = "model.pth"
    device = torch.device("cuda") if torch.cuda.is_available() else torch.device("cpu")
    print("in device: ", device)
    loss_op = torch.nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)

    # pseudo label 相关变量
    pseudo_mask = torch.zeros(num_node).to(device)
    pseudo_labels = torch.tensor([-1 for _ in range(num_node)], dtype=torch.int64).to(
        device
    )
    ig = torch.zeros(num_node).to(device)

    def run(data_loader, model, train_sign=False):
        total_loss, total_acc = 0, 0
        batch_num = 0
        for j, data in enumerate(data_loader):
            data = data.to(device)
            label = torch.argmax(data.y, -1)
            optimizer.zero_grad()
            preds = model(data)
            loss = loss_op(preds[data.root_index], label)
            acc = accuracy(preds[data.root_index], label)
            total_loss += loss.item()
            total_acc += acc
            batch_num = batch_num + 1
            if train_sign:
                loss.backward()
                optimizer.step()
        return total_loss / batch_num, total_acc / batch_num, model

    def train(model, ig, pseudo_labels, pseudo_mask):
        model.to(device)
        best_loss, bad_counter = 100, 0
        for epoch in range(500):
            t1 = time.time()
            model.train()
            for j, data in enumerate(unlabel_loader):
                data = data.to(device)
                seed = data.other_feats["seed"].squeeze()
                if torch.sum(pseudo_mask[seed]) == 0:
                    break
                optimizer.zero_grad()
                preds = model(data)
                preds = preds[data.root_index][torch.where(pseudo_mask[seed])[0]]
                label = pseudo_labels[seed][torch.where(pseudo_mask[seed])[0]]
                loss = weighted_cross_entropy(
                    ig[seed][torch.where(pseudo_mask[seed])[0]],
                    preds,
                    label,
                    config[4],
                    num_class,
                )
                loss.backward()
                optimizer.step()
            train_loss, train_acc, model = run(train_loader, model, True)
            with torch.no_grad():
                model.eval()
                val_loss, val_acc, _ = run(val_loader, model)
            t2 = time.time()
            # print(f"epoch {epoch}, training loss:{train_loss:.4f}, training acc:{train_acc:.4f},"
            #       f"val loss:{val_loss:.4f}, val acc:{val_acc:.4f}, time_cost:{t2 - t1:.4f}")
            if val_loss < best_loss:
                torch.save(
                    model.state_dict(), model_path, _use_new_zipfile_serialization=False
                )
                best_loss = val_loss
                bad_counter = 0
            else:
                bad_counter += 1
            if bad_counter == 20:
                break
        return

    def test():
        state_dict = torch.load(model_path)
        model.load_state_dict(state_dict)
        model.to(device)
        with torch.no_grad():
            model.eval()
            test_loss, test_acc, _ = run(test_loader, model)
            print(f"test loss:{test_loss:.4f}, test acc:{test_acc:.4f}")

    for stage in range(10):
        print(f"In stage {stage}")
        train(model, ig, pseudo_labels, pseudo_mask)
        test()
        state_dict = torch.load(model_path)
        model.load_state_dict(state_dict)
        model.to(device)
        model.eval()
        pseudo_mask, pseudo_labels = generate_pseudo_label(
            model, unlabel_loader, pseudo_mask, pseudo_labels, config, device
        )
        model.train()
        ig = information_gain(model, unlabel_loader, ig, config, device)


if __name__ == "__main__":
    main()
