import numpy as np
import pandas as pd
import sklearn
from sklearn.preprocessing import StandardScaler

np.random.seed(2023)

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
