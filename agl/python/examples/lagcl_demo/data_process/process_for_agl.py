import os
import random
from collections import defaultdict
from random import shuffle, randint, choice

import pandas as pd
from tqdm import tqdm
import pickle


class PickleUtils:
    @staticmethod
    def load_pickle(filepath):
        res = None
        with open(filepath, "rb") as f:
            res = pickle.load(f)
        return res

    @staticmethod
    def save_pickle(res, filepath):
        with open(filepath, "wb") as f:
            pickle.dump(res, f)


def build_node_table(training_set_u, training_set_i, save_folder):
    res = []
    concated_feature_res = []
    embedding_idx = 0
    for node_id in sorted(list(training_set_u.keys())):
        res.append(
            {
                "node_id": node_id,
                "node_feature": embedding_idx,
                "node_degree": len(training_set_u.get(node_id, [])),
                "node_type": 0,
            }
        )
        concated_feature_res.append(
            {
                "node_id": node_id,
                "node_feature": str(embedding_idx)
                + "\t"
                + str(len(training_set_u.get(node_id, [])))
                + "\t0",
            }
        )
        embedding_idx = embedding_idx + 1
    for node_id in sorted(list(training_set_i.keys())):
        res.append(
            {
                "node_id": node_id,
                "node_feature": embedding_idx,
                "node_degree": len(training_set_i.get(node_id, [])),
                "node_type": 1,
            }
        )
        concated_feature_res.append(
            {
                "node_id": node_id,
                "node_feature": str(embedding_idx)
                + "\t"
                + str(len(training_set_i.get(node_id, [])))
                + "\t1",
            }
        )
        embedding_idx = embedding_idx + 1
    node_table = pd.DataFrame(res)
    node_table.to_csv(os.path.join(save_folder, "node_table.csv"), index=False)
    node_concated_feature_table = pd.DataFrame(concated_feature_res)
    node_concated_feature_table.to_csv(
        os.path.join(save_folder, "node_concated_feature_table.csv"), index=False
    )


def build_edge_table(edges, save_folder):
    edge_table = edges.copy()
    edge_table["edge_id"] = (
        edge_table["node1_id"].astype(str) + "_" + edge_table["node2_id"].astype(str)
    )

    reverse_edge_table = edges.copy()
    reverse_edge_table["node1_id"] = edges["node2_id"]
    reverse_edge_table["node2_id"] = edges["node1_id"]
    reverse_edge_table["edge_id"] = (
        reverse_edge_table["node1_id"].astype(str)
        + "_"
        + reverse_edge_table["node2_id"].astype(str)
    )

    multi_edge_table = pd.concat([edge_table, reverse_edge_table])
    pd.DataFrame(multi_edge_table[["node1_id", "node2_id", "edge_id"]]).to_csv(
        os.path.join(save_folder, "edge_table.csv"), index=False
    )


def build_full_batch_seed_table(training_set_u, training_set_i, save_folder):
    node_ids = list(training_set_u.keys())
    node_ids.extend(list(training_set_i.keys()))
    res = []
    for node_id in node_ids:
        res.append({"node_id": node_id, "seed": "graph"})
    seed_table = pd.DataFrame(res)
    seed_table.to_csv(
        os.path.join(save_folder, "full_batch_seed_table.csv"), index=False
    )


def build_training_dataset(train_edges, training_set_u, training_set_i, n_negs=1):
    training_data = [(i[0], i[1]) for i in train_edges[["node1_id", "node2_id"]].values]
    shuffle(training_data)

    # u_idx, i_idx, j_idx = [], [], []
    u_idx, i_idx, labels, pair_ids = [], [], [], []

    item_list = list(training_set_i.keys())
    for i, (user, item) in enumerate(training_data):
        u_idx.append(user)
        i_idx.append(item)
        labels.append(1)
        pair_ids.append(i)
        for m in range(n_negs):
            neg_item = choice(item_list)
            while neg_item in training_set_u[user]:
                neg_item = choice(item_list)
            u_idx.append(user)
            i_idx.append(neg_item)
            labels.append(0)
            pair_ids.append(i)
            # j_idx.append(neg_item)
    # return u_idx, i_idx, j_idx
    return pd.DataFrame(
        {
            "node1_id": u_idx,
            "node2_id": i_idx,
            "seed": [f"train_{i}" for i in range(len(u_idx))],
            "label": labels,
            "pair_ids": pair_ids,
            "train_flag": "train",
        }
    )


def build_test_dataset(test_set, training_set_u, training_set_i):
    u_idx, i_idx, labels = [], [], []
    for user_id, related_item_ids in tqdm(test_set.items()):
        # 遍历所有测试集正样本的用户
        for item_id in training_set_i.keys():
            # 遍历全量 item
            if item_id in related_item_ids:
                # 属于测试集正样本
                label = 1
            elif item_id in training_set_u[user_id]:
                # 属于训练集正样本，该条数据不计算
                label = -1
            else:
                label = 0
            u_idx.append(user_id)
            i_idx.append(item_id)
            labels.append(label)
    return pd.DataFrame(
        {
            "node1_id": u_idx,
            "node2_id": i_idx,
            "label": labels,
        }
    )


def build_sample_table(
    save_folder, train_edges, training_set_u, training_set_i, test_set
):
    training_dataset = build_training_dataset(
        train_edges, training_set_u, training_set_i
    )
    training_dataset.to_csv(os.path.join(save_folder, "sample_table.csv"), index=False)


def __generate_set(training_data, test_data):
    training_set_u = defaultdict(dict)
    training_set_i = defaultdict(dict)
    test_set = defaultdict(dict)
    for user, item in training_data[["node1_id", "node2_id"]].values:
        training_set_u[user][item] = 1
        training_set_i[item][user] = 1
    for user, item in test_data[["node1_id", "node2_id"]].values:
        if user not in training_set_u.keys():
            continue
        if item not in training_set_i.keys():
            continue
        test_set[user][item] = 1
    return training_set_u, training_set_i, test_set


if __name__ == "__main__":
    random.seed(1024)

    dataset_name = "datasets/lastfm"

    save_folder_path = os.path.join(dataset_name, "full_batch_datasets")
    os.makedirs(save_folder_path, exist_ok=True)

    train_edges = pd.read_csv(
        os.path.join(dataset_name, "train.txt"),
        names=["node1_id", "node2_id", "weight"],
        sep=" ",
    )
    train_edges["node1_id"] = "userid_" + train_edges["node1_id"].astype(str)
    train_edges["node2_id"] = "itemid_" + train_edges["node2_id"].astype(str)
    test_edges = pd.read_csv(
        os.path.join(dataset_name, "test.txt"),
        names=["node1_id", "node2_id", "weight"],
        sep=" ",
    )
    test_edges["node1_id"] = "userid_" + test_edges["node1_id"].astype(str)
    test_edges["node2_id"] = "itemid_" + test_edges["node2_id"].astype(str)

    training_set_u, training_set_i, test_set = __generate_set(train_edges, test_edges)

    build_node_table(training_set_u, training_set_i, save_folder_path)
    build_full_batch_seed_table(training_set_u, training_set_i, save_folder_path)
    build_edge_table(train_edges, save_folder_path)

    build_sample_table(
        save_folder_path, train_edges, training_set_u, training_set_i, test_set
    )

    PickleUtils.save_pickle(
        (training_set_u, training_set_i, test_set),
        os.path.join(save_folder_path, "ui_link_info.pickle"),
    )
