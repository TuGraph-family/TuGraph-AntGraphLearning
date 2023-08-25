# 公开数据下载地址
# Facebook: https://docs.google.com/uc?export=download&id=12aJWAGCM4IvdGI2fiydDNyWzViEOLZH8&confirm=t

import torch
import os
import subprocess
import scipy.sparse as sp
from torch_sparse import SparseTensor
from torch_geometric.data import InMemoryDataset, Data, extract_zip
from torch_geometric.datasets import Amazon, Planetoid, Coauthor, AttributedGraphDataset
import numpy as np
import pandas as pd


dataset_name = 'facebook'
base_path = './'
dataset = AttributedGraphDataset("./", name=dataset_name)
data = dataset[0]
nodefeat_dim = int(data.x.shape[1])
node_embd = np.array(data.x).tolist()

node_embd_trans = []
for feat in node_embd:
    feat_str = [str(i) for i in feat]
    node_embd_trans.append(' '.join(feat_str))
node_feats = {'node_id':[str(i) for i in range(len(node_embd))], 'node_feature':node_embd_trans}
node_feats = pd.DataFrame(data=node_feats)


feat_all = []
for i in node_feats['node_feature']:
    feat = [float(x) for x in i.strip().split(' ')]
    if len(feat)!=nodefeat_dim:
        print('error')
    feat_all.append(feat)
node_feat_np = np.array(feat_all)
np.save(f'{base_path}/{dataset_name}_nodefeat',node_feat_np)

new_nodefeat = node_feats.drop(columns='node_feature')
new_nodefeat['node_feature'] = new_nodefeat['node_id']


new_nodefeat.to_csv(f'{base_path}/{dataset_name}_node_t.csv',index=None)


node1_id, node2_id = data.edge_index.tolist()
edge_feats = {'node1_id':[str(i)for i in node1_id], 'node2_id':[str(i)for i in node2_id], 'edge_id':[str(node1_id[i])+'_'+str(node2_id[i]) for i in range(len(node1_id))]}
edge_feats = pd.DataFrame(data=edge_feats)

edge_feats.to_csv(f'{base_path}/{dataset_name}_edge_t.csv',index=None)



num_nodes = data.x.shape[0]

pos_sample = edge_feats   #.sample(frac=0.3,random_state=2013)

pos_neg_rate = 1 #(正样本数量的N倍)
neg_sample = pd.DataFrame([[str(np.random.randint(0, num_nodes)), str(np.random.randint(0, num_nodes))] 
                           for i in range(len(pos_sample) * pos_neg_rate)], columns=['node1_id', 'node2_id'])



edge_index = pos_sample.set_index(["node1_id", "node2_id"]).index
neg_index = neg_sample.set_index(["node1_id", "node2_id"]).index
neg_sample = neg_sample[~neg_index.isin(edge_index)]

neg_sample['seed'] = neg_sample['node1_id']+'_'+neg_sample['node2_id']

pos_sample.rename(columns={'edge_id':'seed'}, inplace=True)


pos_sample['label'] = '0 1'
neg_sample['label'] = '1 0'
train_sample_df = pd.concat([pos_sample, neg_sample]).sample(frac=1).reset_index(drop=True)
train_sample_drop = train_sample_df.drop_duplicates(subset=['seed'],keep=False)

print(len(pos_sample), len(neg_sample), len(train_sample_drop))

train_sample_drop.to_csv(f'{base_path}/{dataset_name}_sample_t.csv',index=None)
