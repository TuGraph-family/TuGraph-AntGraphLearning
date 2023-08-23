import sys
import numpy as np
import pandas as pd

### read node graph feature
train_node_gf = {}
is_first_line = True
for line in open("train_wiki_data.csv"):
    if is_first_line:
        is_first_line = False
        continue
    parts = line.rstrip("\n").split(",")
    seed = parts[0]
    graph_feature = parts[1]
    train_node_gf[seed] = graph_feature

full_node_gf = {}
is_first_line = True
for line in open("full_wiki_data.csv"):
    if is_first_line:
        is_first_line = False
        continue
    parts = line.rstrip("\n").split(",")
    seed = parts[0]
    graph_feature = parts[1]
    full_node_gf[seed] = graph_feature

### Load data and train val test split
g_df = pd.read_csv("ml_wikipedia.csv")
val_time, test_time = list(np.quantile(g_df.ts, [0.70, 0.85]))

train_output = ""
valid_output = ""
test_output = ""

i = 0
last_node_id = None
is_header = True
for line in open("train_seed_table.csv"):
    if is_header:
        is_header = False
        continue
    parts = line.rstrip("\n").split(",")
    node_id = parts[0]
    seed = parts[1]
    time = parts[2]
    label = 0 if seed.endswith("_f") else 1

    if i % 2 == 1:
        train_output += "{},{},{},{},{}\n".format(
            last_node_id, node_id, train_node_gf[seed], time, label
        )
    last_node_id = node_id

    i += 1

i = 0
last_node_id = None
is_header = True
for line in open("full_seed_table.csv"):
    if is_header:
        is_header = False
        continue
    parts = line.rstrip("\n").split(",")
    node_id = parts[0]
    seed = parts[1]
    time = float(parts[2])
    label = 0 if seed.endswith("_f") else 1

    if i % 2 == 1:
        if time > val_time and time <= test_time:
            valid_output += "{},{},{},{},{}\n".format(
                last_node_id, node_id, full_node_gf[seed], time, label
            )
        elif time > test_time:
            test_output += "{},{},{},{},{}\n".format(
                last_node_id, node_id, full_node_gf[seed], time, label
            )
    last_node_id = node_id

    i += 1

with open("wiki_train.csv", "w") as f:
    f.write(train_output)

with open("wiki_valid.csv", "w") as f:
    f.write(valid_output)

with open("wiki_test.csv", "w") as f:
    f.write(test_output)
