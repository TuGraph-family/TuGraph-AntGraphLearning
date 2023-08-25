import json
import subprocess
import os
import numpy as np
import pandas as pd
import random


def preprocess(data_name):
    u_list, i_list, ts_list, label_list = [], [], [], []
    feat_l = []
    idx_list = []
    
    with open(data_name) as f:
        s = next(f)
        print(s)
        for idx, line in enumerate(f):
            e = line.strip().split(',')
            u = int(e[0])
            i = int(e[1])
            
            
            
            ts = float(e[2])
            label = int(e[3])
            
            feat = np.array([float(x) for x in e[4:]])
            
            u_list.append(u)
            i_list.append(i)
            ts_list.append(ts)
            label_list.append(label)
            idx_list.append(idx)
            
            feat_l.append(feat)
    return pd.DataFrame({'u': u_list, 
                         'i':i_list, 
                         'ts':ts_list, 
                         'label':label_list, 
                         'idx':idx_list}), np.array(feat_l)



def reindex(df):
    assert(df.u.max() - df.u.min() + 1 == len(df.u.unique()))
    assert(df.i.max() - df.i.min() + 1 == len(df.i.unique()))
    
    upper_u = df.u.max() + 1
    new_i = df.i + upper_u
    
    new_df = df.copy()
    print(new_df.u.max())
    print(new_df.i.max())
    
    new_df.i = new_i
    new_df.u += 1
    new_df.i += 1
    new_df.idx += 1
    
    print(new_df.u.max())
    print(new_df.i.max())
    
    return new_df



class RandEdgeSampler(object):
    def __init__(self, src_list, dst_list, seed):
        self.src_list = np.unique(src_list)
        self.dst_list = np.unique(dst_list)
        np.random.seed(seed)

    def sample(self):
        src_index = np.random.randint(0, len(self.src_list))
        dst_index = np.random.randint(0, len(self.dst_list))
        return self.src_list[src_index], self.dst_list[dst_index]



def get_tables(data_name):
    g_df = pd.read_csv('./processed/ml_{}.csv'.format(data_name))
    val_time, test_time = list(np.quantile(g_df.ts, [0.70, 0.85]))
    
    src_l = g_df.u.values
    dst_l = g_df.i.values
    ts_l = g_df.ts.values
    e_idx_l = g_df.idx.values
    
    max_src_index = src_l.max()
    max_idx = max(src_l.max(), dst_l.max())
    
    random.seed(2020)
    
    total_node_set = set(np.unique(np.hstack([g_df.u.values, g_df.i.values])))
    num_total_unique_nodes = len(total_node_set)
    
    mask_node_set = set(
        random.sample(set(src_l[ts_l > val_time]).union(set(dst_l[ts_l > val_time])), int(0.1 * num_total_unique_nodes)))
    mask_src_flag = g_df.u.map(lambda x: x in mask_node_set).values
    mask_dst_flag = g_df.i.map(lambda x: x in mask_node_set).values
    none_node_flag = (1 - mask_src_flag) * (1 - mask_dst_flag)
    
    train_flag = (ts_l <= val_time) * (none_node_flag > 0)
    train_src_l = src_l[train_flag]
    train_dst_l = dst_l[train_flag]
    train_e_idx_l = e_idx_l[train_flag]
    
    # select validation and test dataset
    val_flag = (ts_l <= test_time) * (ts_l > val_time)
    test_flag = ts_l > test_time
    
    val_src_l = src_l[val_flag]
    val_dst_l = dst_l[val_flag]
    
    test_src_l = src_l[test_flag]
    test_dst_l = dst_l[test_flag]
    
    train_rand_sampler = RandEdgeSampler(train_src_l, train_dst_l,2021)
    full_rand_sampler = RandEdgeSampler(src_l, dst_l,2022 )
    
    # edge table
    # format: node1_id,node2_id,edge_id,time
    # label table
    # format: node_id,seed_id,time
    
    node_set = set([])
    edge_time = {}
    full_edge_table_output = "node1_id,node2_id,edge_id,time,edge_feature\n"
    full_seed_table_output = "node_id,seed,time\n"
    train_edge_table_output = "node1_id,node2_id,edge_id,time,edge_feature\n"
    train_seed_table_output = "node_id,seed,time\n"
    is_header = True
    with open("./processed/ml_{}.csv".format(data_name)) as f:
        for line in f:
            if is_header:
                is_header = False
                continue
            parts = line.rstrip().split(",")
            node1_id = int(parts[1])
            node2_id = int(parts[2])
            ts = float(parts[3])
            label = parts[4]
            edge_id = int(parts[5])
            edge_time[edge_id] = ts
    
            node_set.add(node1_id)
            node_set.add(node2_id)
            full_edge_table_output += "{},{},{},{},{}\t{}\n".format(node1_id, node2_id, edge_id, ts, edge_id, ts)
            full_edge_table_output += "{},{},{},{},{}\t{}\n".format(node2_id, node1_id, str(edge_id)+"_r", ts, edge_id, ts)
            full_seed_table_output += "{},{},{}\n".format(node1_id, edge_id, ts)
            full_seed_table_output += "{},{},{}\n".format(node2_id, edge_id, ts)
    
            _, node_fake = full_rand_sampler.sample()
            full_seed_table_output += "{},{},{}\n".format(node1_id, str(edge_id)+"_f", ts)
            full_seed_table_output += "{},{},{}\n".format(node_fake, str(edge_id)+"_f", ts)
    
            if edge_id in train_e_idx_l:
                train_edge_table_output += "{},{},{},{},{}\t{}\n".format(node1_id, node2_id, edge_id, ts, edge_id, ts)
                train_edge_table_output += "{},{},{},{},{}\t{}\n".format(node2_id, node1_id, str(edge_id)+"_r", ts, edge_id, ts)
                train_seed_table_output += "{},{},{}\n".format(node1_id, edge_id, ts)
                train_seed_table_output += "{},{},{}\n".format(node2_id, edge_id, ts) 

                _, node_fake = train_rand_sampler.sample()
                train_seed_table_output += "{},{},{}\n".format(node1_id, str(edge_id)+"_f", ts)
                train_seed_table_output += "{},{},{}\n".format(node_fake, str(edge_id)+"_f", ts)
    
    # train edge table 
    with open("train_edge_table.csv", "w") as f:
        f.write(train_edge_table_output)
    
    # train seed table
    with open("train_seed_table.csv", "w") as f:
        f.write(train_seed_table_output)
    
    # full edge table 
    with open("full_edge_table.csv", "w") as f:
        f.write(full_edge_table_output)
    
    # full seed table
    with open("full_seed_table.csv", "w") as f:
        f.write(full_seed_table_output)
    
    # node feature
    node_num = max(node_set) + 1
    node_feature_output = "node_id,node_feature\n"
    for i in range(node_num):
        node_feature_output += "{},{}\n".format(i, i)
    
    with open("node_feature.csv", "w") as f:
        f.write(node_feature_output)
    
    # edge feature
    # # shape: (157475, 172)
    edge_feature = np.load("./processed/ml_{}.npy".format(data_name))
    edge_num = np.shape(edge_feature)[0]
    edge_feature_output = "edge_id,edge_feature\n"
    
    for i in range(edge_num):
        time = edge_time.get(i, 0)
        edge_feature_output += "{},{}\t{}\n".format(i, i, time)
        edge_feature_output += "{}_r,{}\t{}\n".format(i, i, time)
    
    with open("edge_feature.csv", "w") as f:
        f.write(edge_feature_output)
    
    print("node_num=", node_num, "edge_num=", edge_num)

def download_and_preprocess(data_name):
    PATH = './processed/{}.csv'.format(data_name)
    OUT_DF = './processed/ml_{}.csv'.format(data_name)
    OUT_FEAT = './processed/ml_{}.npy'.format(data_name)
    OUT_NODE_FEAT = './processed/ml_{}_node.npy'.format(data_name)
    
    df, feat = preprocess(PATH)
    new_df = reindex(df)
    
    print(feat.shape)
    empty = np.zeros(feat.shape[1])[np.newaxis, :]
    feat = np.vstack([empty, feat])
    
    max_idx = max(new_df.u.max(), new_df.i.max())
    rand_feat = np.zeros((max_idx + 1, feat.shape[1]))
    
    print(feat.shape)
    new_df.to_csv(OUT_DF)
    np.save(OUT_FEAT, feat)
    np.save(OUT_NODE_FEAT, rand_feat)
    get_tables(data_name)

if not os.path.exists('./processed/'):
    os.mkdir('./processed/')
if not os.path.exists('./processed/wikipedia.csv'):
    wiki_data_url = "http://snap.stanford.edu/jodie/wikipedia.csv"
    wiki_data = './processed/' + wiki_data_url.split("/")[-1]
    print(f"download from {wiki_data_url} to {wiki_data}")
    subprocess.check_call(f'wget --quiet {wiki_data_url} -O {wiki_data}', shell=True)
if not os.path.exists('train_edge_table.csv'):
    download_and_preprocess('wikipedia')

