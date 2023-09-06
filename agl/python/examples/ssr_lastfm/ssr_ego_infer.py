import os

import numpy as np
import torch
from torch.utils.data import DataLoader, TensorDataset
import torch.nn.functional as F

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.multi_graph_feature_collate import MultiGraphFeatureCollate
from agl.python.data.column import AGLMultiDenseColumn
from pyagl.pyagl import AGLDType, DenseFeatureSpec, NodeSpec, EdgeSpec

from ssr_ego import SSRLastfmModel

# step 1: 构建dataset
node_file_name = "./data_process/data/agl_gzoo_bmdata_ssr_lastfm_open_source_node_table.csv"
edge_file_name = "./data_process/data/agl_gzoo_bmdata_ssr_lastfm_open_source_edge_table.csv"

# node data set
node_data_set = AGLTorchMapBasedDataset(
    node_file_name, has_schema=True, column_sep=",", schema=["node_id", "node_feature"]
)

# step 2: 构建collate function

# step 3: 构建 dataloader
# train loader
node_loader = DataLoader(
    dataset=node_data_set,
    batch_size=512,
    shuffle=False,
    num_workers=3,
    persistent_workers=True,
)

# step 4: 模型相关以及训练与测试
model = torch.load("result/model.pt")
print(model)
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print("in device: ", device)

user_num = 2101

print("infer features")
f = open("result/features.txt", "w")
print("node_id,features", file=f)
model.to(device)

model.eval()
for data in node_loader:
    with torch.no_grad():
        node_feature = torch.LongTensor(
            np.array(data["node_feature"], dtype=np.int32)
        ).to(device)
        x = model.node_embed_layer(node_feature.reshape([-1]))
        x = F.normalize(x, p=2, dim=-1)
        feats = x.cpu().numpy()
        ids = data["node_id"]
        for id, feat in zip(ids, feats):
            print(
                int(id),
                " ".join(map(lambda x: str(round(x, 6)), feat)),
                file=f,
                sep=",",
            )
f.close()

print("infer embeds")
f = open("result/out_embed.txt", "w")
print("node_id,embeds", file=f)
model.to(device)

f = open(edge_file_name, "r")
f.readline()
edges = []
for line in f:
    l = line.strip().split(",")
    edges.append([int(l[0]), int(l[1])])
f.close()
dataset = TensorDataset(torch.LongTensor(edges))
edge_loader = DataLoader(dataset=dataset, batch_size=512, shuffle=False)
model.eval()
fout = open("./result/edge_weight.txt", "w")
print("node1_id,node2_id,edge_type,edge_id,weight", file=fout)

print("infer edge weghts")

for data in edge_loader:
    data = data[0].to(device)
    x = model.node_embed_layer(data.reshape([-1]))
    x = F.normalize(x, p=2, dim=-1)
    edge_index = torch.arange(x.shape[0]).reshape([-1, 2]).T.to(device)
    _, (edge_index, alpha) = model._encoder.convs.forward(
        x, edge_index, return_attention_weights=True
    )
    att = alpha[: data.shape[0]].detach().cpu().numpy()
    data = data.detach().cpu().numpy()
    for d, a in zip(data, att):
        node1_type = "U" if d[0] < user_num else "I"
        node2_type = "U" if d[1] < user_num else "I"
        print(
            d[0],
            d[1],
            node1_type + "_" + node2_type,
            str(d[0]) + "_" + str(d[1]),
            round(a[0], 6),
            file=fout,
            sep=",",
        )
fout.close()
print("done")
