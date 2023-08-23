import numpy as np
import pandas as pd


class RandEdgeSampler(object):
    def __init__(self, src_list, dst_list):
        self.src_list = np.unique(src_list)
        self.dst_list = np.unique(dst_list)

    def sample(self):
        src_index = np.random.randint(0, len(self.src_list))
        dst_index = np.random.randint(0, len(self.dst_list))
        return self.src_list[src_index], self.dst_list[dst_index]


g_df = pd.read_csv("ml_wikipedia.csv")
val_time, test_time = list(np.quantile(g_df.ts, [0.70, 0.85]))

src_l = g_df.u.values
dst_l = g_df.i.values
ts_l = g_df.ts.values

train_flag = ts_l <= val_time
train_src_l = src_l[train_flag]
train_dst_l = dst_l[train_flag]

val_flag = (ts_l <= test_time) * (ts_l > val_time)
val_src_l = src_l[val_flag]
val_dst_l = dst_l[val_flag]

test_flag = ts_l > test_time
test_src_l = src_l[test_flag]
test_dst_l = dst_l[test_flag]

train_rand_sampler = RandEdgeSampler(train_src_l, train_dst_l)
full_rand_sampler = RandEdgeSampler(src_l, dst_l)

# edge table
# format: node1_id,node2_id,edge_id,time
# label table
# format: node_id,seed_id,time

node_set = set([])
edge_time = {}
full_edge_table_output = "node1_id,node2_id,edge_id,time\n"
full_seed_table_output = "node_id,seed,time\n"
train_edge_table_output = "node1_id,node2_id,edge_id,time\n"
train_seed_table_output = "node_id,seed,time\n"
is_header = True
with open("ml_wikipedia.csv") as f:
    for line in f:
        if is_header:
            is_header = False
            continue
        parts = line.rstrip().split(",")
        node1_id = int(parts[1])
        node2_id = int(parts[2])
        ts = float(parts[3])
        label = parts[4]
        edge_id = parts[5]
        edge_time[int(edge_id)] = ts

        node_set.add(node1_id)
        node_set.add(node2_id)
        full_edge_table_output += "{},{},{},{}\n".format(
            node1_id, node2_id, edge_id, ts
        )
        full_edge_table_output += "{},{},{},{}\n".format(
            node2_id, node1_id, edge_id + "_r", ts
        )
        full_seed_table_output += "{},{},{}\n".format(node1_id, edge_id, ts)
        full_seed_table_output += "{},{},{}\n".format(node2_id, edge_id, ts)

        _, node_fake = full_rand_sampler.sample()
        full_seed_table_output += "{},{},{}\n".format(node1_id, edge_id + "_f", ts)
        full_seed_table_output += "{},{},{}\n".format(node_fake, edge_id + "_f", ts)

        if ts <= val_time:
            train_edge_table_output += "{},{},{},{}\n".format(
                node1_id, node2_id, edge_id, ts
            )
            train_edge_table_output += "{},{},{},{}\n".format(
                node2_id, node1_id, edge_id + "_r", ts
            )
            train_seed_table_output += "{},{},{}\n".format(node1_id, edge_id, ts)
            train_seed_table_output += "{},{},{}\n".format(node2_id, edge_id, ts)

            _, node_fake = train_rand_sampler.sample()
            train_seed_table_output += "{},{},{}\n".format(node1_id, edge_id + "_f", ts)
            train_seed_table_output += "{},{},{}\n".format(
                node_fake, edge_id + "_f", ts
            )

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
edge_feature = np.load("ml_wikipedia.npy")
edge_num = np.shape(edge_feature)[0]
edge_feature_output = "edge_id,edge_feature\n"

for i in range(edge_num):
    time = edge_time.get(i, 0)
    edge_feature_output += "{},{}\t{}\n".format(i, i, time)
    edge_feature_output += "{}_r,{}\t{}\n".format(i, i, time)

with open("edge_feature.csv", "w") as f:
    f.write(edge_feature_output)

print("node_num=", node_num, "edge_num=", edge_num)
