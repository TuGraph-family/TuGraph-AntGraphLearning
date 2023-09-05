import os
import numpy as np
import pandas as pd

def combine_metapath(metapath_list, train_flag):
    graph_feature_dict = {}
    label_dict = {}
    for mi, metapath in enumerate(metapath_list):
        graph_feature_file = ""
        for file in os.listdir(f"output_graph_feature_{metapath}_{train_flag}"):
            if file.startswith("part-"):
                graph_feature_file = f"output_graph_feature_{metapath}_{train_flag}/" + file
                break
        print(f"----------- {mi} {graph_feature_file} into hegnn_{train_flag}.csv")
        count = 0
        column_index = {}
        for line in open(graph_feature_file):
            arrs = line.strip().split(",")
            if count == 0:
                for i, col in enumerate(arrs):
                    column_index[col] = i
            else:
                seed = arrs[column_index["seed"]]
                if mi == 0:
                    graph_feature_dict[seed] = arrs[column_index["graph_feature"]]
                    label_dict[seed] = arrs[column_index["label"]]
                else:
                    graph_feature_dict[seed] = graph_feature_dict[seed] + ";" +arrs[column_index["graph_feature"]]
                    assert label_dict[seed] == arrs[column_index["label"]]
            count += 1
    print(f"----------- {len(graph_feature_dict)}")
    train_file = open(f'hegnn_{train_flag}.csv', 'w')
    train_file.write("seed,graph_feature,label\n")
    for seed in graph_feature_dict.keys():
        train_file.write(seed + "," + graph_feature_dict[seed] + "," + label_dict[seed] + "\n")
    train_file.close()
combine_metapath(["p_s_p", "p_a_p"], "test")
combine_metapath(["p_s_p", "p_a_p"], "train")
combine_metapath(["p_s_p", "p_a_p"], "val")
