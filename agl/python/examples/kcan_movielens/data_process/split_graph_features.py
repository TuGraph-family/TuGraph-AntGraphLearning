import os
import numpy as np
import pandas as pd

graph_feature_file = ""
for file in os.listdir("output_graph_feature"):
    if file.startswith("part-"):
        graph_feature_file = "output_graph_feature/" + file
        break
data_all = pd.read_csv(graph_feature_file)

train_file = open('subgraph_kcan_movielens_train.csv', 'w')
val_file = open('subgraph_kcan_movielens_test.csv', 'w')

train_flag_column_index = -1
for line in open(graph_feature_file):
    arrs = line.strip().split(",")
    train_flags = arrs[train_flag_column_index]
    if train_flags == '1':
        train_file.write(line)
    elif train_flags == '0':
        val_file.write(line)
train_file.close()
val_file.close()
