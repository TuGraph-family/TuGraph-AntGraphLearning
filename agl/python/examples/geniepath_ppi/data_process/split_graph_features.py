import os
import numpy as np
import pandas as pd

graph_feature_file = ""
for file in os.listdir("output_graph_feature"):
    if file.startswith("part-"):
        graph_feature_file = "output_graph_feature/" + file
        break
data_all = pd.read_csv(graph_feature_file)

train_file = open('ppi_train.csv', 'w')
val_file = open('ppi_val.csv', 'w')
test_file = open('ppi_test.csv', 'w')

train_flag_column = "train_flag_list"
train_flag_column_index = -1
count = 0
for line in open(graph_feature_file):
    arrs = line.strip().split(",")
    if count == 0:
        train_file.write(line)
        val_file.write(line)
        test_file.write(line)
        for i, col in enumerate(arrs):
            if col == train_flag_column:
                train_flag_column_index = i
    else:
        train_flags = arrs[train_flag_column_index]
        train_flag_set = set(train_flags.split("\t"))
        assert len(train_flag_set) == 1
        if "train" in train_flag_set:
            train_file.write(line)
        elif "val" in train_flag_set:
            val_file.write(line)
        elif "test" in train_flag_set:
            test_file.write(line)
    count += 1
train_file.close()
val_file.close()
test_file.close()
