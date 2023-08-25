import numpy as np
import json
import os

import networkx as nx
from networkx.readwrite import json_graph
import pandas as pd
import sklearn
from sklearn.preprocessing import StandardScaler

np.random.seed(2023)

def load_data(prefix):
    if os.path.exists(prefix + "-feats.npy"):
        feats = np.load(prefix + "-feats.npy")
    with open('ppi_node_feature.csv', 'w') as outfile:
        outfile.write('node_id,node_feature\n')
        for i, f in enumerate(feats): 
            outfile.write(str(i) + "," + " ".join(map(str, f)) + "\n")
    G_data = json.load(open(prefix + "-G.json"))
    selfloop_nodes = set()
    with open('ppi_edge_feature.csv', 'w') as outfile:
        outfile.write('node1_id,node2_id,edge_id\n')
        for link in G_data["links"]:
            outfile.write(str(link["source"]) + "," + str(link["target"]) + "," + str(link["source"]) + "_" + str(link["target"]) + "\n")
            if link["source"] != link["target"]:
                outfile.write(str(link["target"]) + "," + str(link["source"]) + "," + str(link["target"]) + "_" + str(link["source"]) + "\n")
            else:
                selfloop_nodes.add(link["target"])
        for i in range(56944):
            if i not in selfloop_nodes:
                outfile.write(str(i) + "," + str(i) + "," + str(i) + "_" + str(i) + "\n")


    id_map = json.load(open(prefix + "-id_map.json"))
    for i in range(56944):
        if i != id_map[str(i)]:
            print("id mapping error:" + i + "->" + id_map[str(i)])
            exit(0)
    node_train_flat = {}
    for node in G_data["nodes"]:
        nid = node["id"]
        if node["test"]:
            node_train_flat[nid] = "test"
        elif node["val"]:
            node_train_flat[nid] = "val"
        else:
            node_train_flat[nid] = "train"
        if node["test"] and node["val"]:
            print("both test and val error:" + nid)
            exit(0)
    train_graph_id = np.load(prefix + "-train_graph_id.npy")
    class_map = json.load(open(prefix + "-class_map.json"))
    with open('ppi_label.csv', 'w') as outfile:
        outfile.write('node_id,seed,label,train_flag\n')
        for k in class_map.keys():
            outfile.write(k + "," + str(train_graph_id[int(k)]) + "," + " ".join(map(str, class_map[k])) + "," + node_train_flat[int(k)] + "\n")



def normalize_node_feature():
    data_df = pd.read_csv('ppi_node_feature.csv', sep=',')
    ids = data_df['node_id'].to_numpy()
    feats = data_df['node_feature'].to_numpy()
    feats = np.asarray([[float(y) for y in x.split(' ')] for x in feats])
    
    train_ids = []
    for line in open("ppi_label.csv"):
        arrs = line.strip().split(",")
        if arrs[3] == 'train':
            train_ids.append(int(arrs[0]))
    train_ids = np.array(train_ids)
    train_feats = feats[train_ids]
    scaler = StandardScaler()
    scaler.fit(train_feats)
    feats = scaler.transform(feats)
    with open('ppi_node_feature_normalized.csv', 'w') as outfile:
        outfile.write('node_id,node_feature\n')
        for i, f in enumerate(feats): 
            outfile.write(str(i) + "," + " ".join(map(str, f)) + "\n")
    
load_data("ppi/ppi")
normalize_node_feature()
