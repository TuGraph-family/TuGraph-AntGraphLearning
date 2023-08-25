import os
import numpy as np
import pandas as pd

graph_feature_file = ""
for file in os.listdir("output_graph_feature"):
    if file.startswith("part-"):
        graph_feature_file = "output_graph_feature/" + file
        break
data_all = pd.read_csv(graph_feature_file)

data_spilt = [0.75, 0.05, 0.2]

nums = data_all.shape[0]
train_data = data_all[:int(nums*data_spilt[0])]
val_data = data_all[int(nums*data_spilt[0]):int(nums*(data_spilt[0]+data_spilt[1]))]
test_data = data_all[int(nums*(data_spilt[0]+data_spilt[1])):]

dataset_name = 'facebook'
base_path = './'
train_data.to_csv(f'{base_path}/{dataset_name}_pagnn_train.csv',index=None)
val_data.to_csv(f'{base_path}/{dataset_name}_pagnn_val.csv',index=None)
test_data.to_csv(f'{base_path}/{dataset_name}_pagnn_test.csv',index=None)
