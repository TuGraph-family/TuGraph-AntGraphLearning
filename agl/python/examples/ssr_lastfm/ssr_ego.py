import os
import time
from typing import Dict, Union

import numpy as np
from sklearn import metrics
import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader

from agl.python.dataset.map_based_dataset import AGLTorchMapBasedDataset
from agl.python.data.multi_graph_feature_collate import MultiGraphFeatureCollate
from agl.python.data.column import AGLMultiDenseColumn
from agl.python.model.encoder.ssr import SSREncoder
from pyagl.pyagl import AGLDType, DenseFeatureSpec, NodeSpec, EdgeSpec


class SSRLastfmModel(torch.nn.Module):
    def __init__(
        self,
        feats_dims: Dict[str, int], hidden_dim: int, out_dim: int,
        sampled_num: int=20, gumbel_temperature: float=0.1, n_hops: int=2, residual: bool=True
    ):
        super().__init__()

        # initial layer
        self.node_embed_layer = torch.nn.Embedding(feats_dims['node_feature'],
                                                   hidden_dim, max_norm=5,
                                                   scale_grad_by_freq=True)

        # encoder layer
        self._encoder = SSREncoder(
            hidden_dim=hidden_dim, out_dim=out_dim,
            sampled_num=sampled_num, gumbel_temperature=gumbel_temperature,
            n_hops=n_hops, residual=residual
        )

        # decoder layer
        self._decoder = torch.nn.Linear(hidden_dim, out_dim)

    def forward(self, subgraph):
        x = self.node_embed_layer(subgraph.n_feats.features['node_feature'].to_dense().reshape([-1]))
        x = F.normalize(x, p=2, dim=-1)

        embedding = self._encoder(subgraph, x)
        
        out = self._decoder(embedding)
        return out

def main():
    # step 1: 构建dataset
    train_file_name = "./data/subgraph_ssr_lastfm_train.csv"
    test_file_name = "./data/subgraph_ssr_lastfm_test.csv"

    # train data set and test data set
    train_data_set = AGLTorchMapBasedDataset(train_file_name,
                                            has_schema=True,
                                            schema=["node1_id", "node2_id", "link", "label", "train_flag", "graph_feature", "graph_feature_2"],
                                            column_sep=',')
    test_data_set = AGLTorchMapBasedDataset(test_file_name,
                                            has_schema=True,
                                            schema=["node1_id", "node2_id", "link", "label", "train_flag", "graph_feature", "graph_feature_2"],
                                            column_sep=',')


    # step 2: 构建collate function
    # node related spec
    node_spec = NodeSpec("default", AGLDType.STR)
    node_spec.AddDenseSpec("node_feature", DenseFeatureSpec("node_feature", 1, AGLDType.INT64))
    # edge related spec
    edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
    # edge_spec.AddDenseSpec("edge_feature", DenseFeatureSpec("edge_feature", 1, AGLDType.INT64))

    label_column = AGLMultiDenseColumn(name="label", dim=1, dtype=np.int64)
    my_collate = MultiGraphFeatureCollate(node_spec, edge_spec, columns=[label_column],
                                        need_node_and_edge_num=False,
                                        graph_feature_name=['graph_feature', 'graph_feature_2'],
                                        label_name="label",
                                        hops=2, uncompress=True,
                                        ego_edge_index=True)

    # step 3: 构建 dataloader
    # train loader
    train_loader = DataLoader(dataset=train_data_set,
                            batch_size=256,
                            shuffle=True,
                            collate_fn=my_collate,
                            num_workers=3,
                            persistent_workers=True)

    test_loader = DataLoader(dataset=test_data_set,
                            batch_size=256,
                            shuffle=False,
                            collate_fn=my_collate,
                            num_workers=3,
                            persistent_workers=True)

    # step 4: 模型相关以及训练与测试
    model = SSRLastfmModel(feats_dims={"node_feature": 20847}, hidden_dim=64, out_dim=64,
                    sampled_num=20, gumbel_temperature=0.5, residual=True)
    print(model)
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print("in device: ", device)
    loss_op = torch.nn.BCEWithLogitsLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=0.0001)


    def train():
        model.to(device)
        model.train()

        total_loss = 0
        i = 0
        t1 = time.time()
        for j, data in enumerate(train_loader):
            data = [d.to(device) for d in data]
            optimizer.zero_grad()
            user_embed = model(data[0])
            item_embed = model(data[1])
            preds = torch.sum(user_embed*item_embed, -1).reshape([-1, 1])
            loss = loss_op(preds, data[0].y.to(torch.float32))
            total_loss += loss.item()
            i = i+1
            loss.backward()
            optimizer.step()
            t2 = time.time()
            if j % 100 == 0:
                print(f"batch {j}, loss:{loss.item()}, time_cost:{t2-t1}")
        return total_loss / i


    def test(loader):
        model.eval()
        ys, preds = [], []
        for data in loader:
            with torch.no_grad():
                data = [d.to(device) for d in data]
                user_embed = model(data[0])
                item_embed = model(data[1])
                out = torch.sum(user_embed*item_embed, -1).reshape([-1, 1])
            pred = out.float().cpu().numpy()
            preds.extend(pred)
            ys.extend(data[0].y.cpu().numpy())

        auc = metrics.roc_auc_score(ys, preds)

        return auc

    best_auc = 0.0
    for epoch in range(1, 101):
        t0 = time.time()
        loss = train()
        t1 = time.time()
        auc = test(test_loader)
        t2 = time.time()
        if auc > best_auc:
            best_auc = auc
            torch.save(model, 'result/model.pt')
        print('Epoch: {:02d}, Loss: {:.4f}, auc: {:.4f}, best_auc: {:.4f}, train_time: {:4f}, val_time: {:4f}'.format(epoch, loss,
                                                                                                    auc, best_auc, t1-t0, t2-t1))
if __name__ == '__main__':
    main()