import os

import torch
import numpy as np
import sys
import pickle as pkl
import scipy.sparse as sp
import csv
import networkx as nx
from torch_sparse import SparseTensor

def parse_index_f(path):
    """Parse the index file.
    Parameters
    ----------
    path:
        directory of index file (str)
    """
    index = []
    for line in open(path):
        index.append(int(line.strip()))
    return index

def sparse_mx_to_sparse_tensor(sparse_mx):
    """Convert a scipy sparse matrix to a sparse tensor.
    """
    sparse_mx = sparse_mx.tocoo().astype(np.float32)
    rows = torch.from_numpy(sparse_mx.row).long()
    cols = torch.from_numpy(sparse_mx.col).long()
    values = torch.from_numpy(sparse_mx.data)
    return SparseTensor(row=rows, col=cols, value=values, sparse_sizes=torch.tensor(sparse_mx.shape)), rows, cols


def normalize(mx):
    """Row-normalize sparse matrix.
    """
    r_sum = np.array(mx.sum(1))
    r_inv = np.power(r_sum, -1).flatten()
    r_inv[np.isinf(r_inv)] = 0.
    r_mat_inv = sp.diags(r_inv)
    mx = r_mat_inv.dot(mx)
    return mx

def load_data(path, dataset):
    """Load input data from directory.
        Parameters
        ----------
        path:
            directory of data (str)
        dataset:
            name of dataset (str)

        Files
        ----------
        ind.dataset.x:
            feature of trainset (sp.csr.csr_matrix)
        ind.dataset.tx:
            feature of testset (sp.csr.csr_matrix)
        ind.dataset.allx:
            feature of both labeled and unlabeled training instances (sp.csr.csr_matrix)
        ind.dataset.y:
            one-hot label of trainset (numpy.array)
        ind.dataset.ty:
            one-hot label of testset (numpy.array)
        ind.dataset.ally:
            label of instances in ind.dataset.allx (numpy.array)
        ind.dataset.graph:
            dictionary in the format {index:[index_of_neighbor_nodes]} (collections.defaultdict)
        ind.dataset.test.index:
            indices of testset for the inductive setting (list)

        All objects above must be saved using python pickle module.
        """
    print("Loading {} dataset...".format(dataset))
    print(os.getcwd())
    if dataset in ['cora', 'citeseer', 'pubmed']:
        names = ['x', 'y', 'tx', 'ty', 'allx', 'ally', 'graph']
        objects = []
        for i in range(len(names)):
            with open("{}/ind.{}.{}".format(path, dataset, names[i]), 'rb') as f:
                if sys.version_info > (3, 0):
                    objects.append(pkl.load(f, encoding='latin1'))
                else:
                    objects.append(pkl.load(f))

        x, y, tx, ty, allx, ally, graph = tuple(objects)
        test_idx_reorder = parse_index_f("{}/ind.{}.test.index".format(path, dataset))
        test_idx_range = np.sort(test_idx_reorder)

        if dataset == 'citeseer':
            # Fix citeseer dataset (there are some isolated nodes in the graph)
            # Find isolated nodes, add them as zero-vecs into the right position
            test_idx_range_full = range(min(test_idx_reorder), max(test_idx_reorder) + 1)
            tx_extended = sp.lil_matrix((len(test_idx_range_full), x.shape[1]))
            tx_extended[test_idx_range - min(test_idx_range), :] = tx
            tx = tx_extended
            ty_extended = np.zeros((len(test_idx_range_full), y.shape[1]))
            ty_extended[:, 0] = 1
            ty_extended[test_idx_range - min(test_idx_range), :] = ty
            ty = ty_extended

        feature = sp.vstack((allx, tx)).tolil()
        feature = normalize(feature)
        feature[test_idx_reorder, :] = feature[test_idx_range, :]
        feature = torch.from_numpy(feature.todense()).float()

        adj = nx.adjacency_matrix(nx.from_dict_of_lists(graph)).tolil()
        adj, rows, cols = sparse_mx_to_sparse_tensor(adj)

        label = np.vstack((ally, ty))
        label[test_idx_reorder, :] = label[test_idx_range, :]
        label = np.where(label)[1]

        num_class = len(set(label))
        num_node = len(label)
        idx_train = []
        for j in range(num_class):
            idx_train.extend([i for i, x in enumerate(label) if x == j][:20])

        label = torch.LongTensor(label)

        idx_test = test_idx_range.tolist()
        idx_val = range(len(y), len(y) + 500)
    return feature, rows, cols, label, idx_train, idx_val, idx_test, num_node


def agl_dataset_2_json(features, rows, cols, labels, idx_train, idx_val, idx_test, num_node):
    with open('citeseer_node_table.csv', 'w', newline='') as csvfile:
        spamwriter = csv.writer(csvfile)
        spamwriter.writerow(['node_id', 'node_feature'])
        for id in range(num_node):
            feature = features[id].tolist()
            flat_feature = ''
            for index, fea in enumerate(feature):
                if fea != 0:
                    flat_feature += str(index) + ':' + str(fea) + ' '
            flat_feature = flat_feature[:-1]
            spamwriter.writerow([id, flat_feature])
    with open('citeseer_label.csv', 'w', newline='') as csvfile:
        spamwriter = csv.writer(csvfile)
        spamwriter.writerow(['node_id', 'seed', 'label', 'train_flag'])
        labels = torch.nn.functional.one_hot(labels)
        for id in range(num_node):
            label = labels[id].tolist()
            label = " ".join([str(i) for i in label])
            if id in idx_train:
                train_flag = '1'
            elif id in idx_test:
                train_flag = '0'
            elif id in idx_val:
                train_flag = '2'
            else:
                train_flag = '-1'
            spamwriter.writerow([str(id), str(id), label, train_flag])
    src_edges, dst_edges = rows.tolist(), cols.tolist()
    with open('citeseer_edge_table.csv', 'w', newline='') as csvfile:
        spamwriter = csv.writer(csvfile)
        spamwriter.writerow(['node1_id', 'node2_id', 'edge_id'])
        for index, src_id in enumerate(src_edges):
            dst_id = dst_edges[index]
            edge = str(src_id) + '_' + str(dst_id)
            spamwriter.writerow([str(src_id), str(dst_id), edge])
    return

if __name__ == '__main__':
    features, rows, cols, labels, train_mask, val_mask, test_mask, num_node = load_data("data", "citeseer")
    agl_dataset_2_json(features, rows, cols, labels, train_mask, val_mask, test_mask, num_node)
