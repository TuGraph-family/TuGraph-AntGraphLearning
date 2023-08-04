#!/usr/bin/python
# coding: utf-8

import unittest
import numpy as np

from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, NodeSpec, EdgeSpec
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn
from agl.python.data.subgraph.pyg_inputs import TorchSubGraphBatchData


class SubGraphTest(unittest.TestCase):
    # 1. node related spec
    n_name = "default"
    n_id_dtype = AGLDType.STR
    # 1.1 node dense spec
    n_df_name = "dense"
    n_df_dim = 3
    n_df_dtype = AGLDType.FLOAT
    # 1.2 node sp kv spec
    n_spkv_name = "nf"
    n_max_dim = 11
    n_key_dtype = AGLDType.INT64
    n_val_dtype = AGLDType.INT64
    node_spec = NodeSpec(n_name, n_id_dtype)
    node_spec.AddDenseSpec(n_df_name, DenseFeatureSpec(n_df_name, n_df_dim, n_df_dtype))
    node_spec.AddSparseKVSpec(
        n_spkv_name, SparseKVSpec(n_spkv_name, n_max_dim, n_key_dtype, n_val_dtype)
    )

    # 2. edge related spec
    e_name = "default"
    n1_name = "default"
    n2_name = "default"
    e_id_dtype = AGLDType.STR
    # 2.1 edge spkv spec
    e_kv_name = "ef"
    e_max_dim = 11
    e_key_dtype = AGLDType.INT64
    e_val_dtype = AGLDType.FLOAT
    edge_spec = EdgeSpec(e_name, node_spec, node_spec, e_id_dtype)
    edge_spec.AddSparseKVSpec(
        e_kv_name, SparseKVSpec(e_kv_name, e_max_dim, e_key_dtype, e_val_dtype)
    )

    def test_call_function(self):
        pb_string = [
            b"CnIKB2RlZmF1bHQSZwoLCgkKATEKATIKATMSWAozCgVkZW5zZRIqCAMSJgokzczMPc3MjD8AAIA/zcxMPs3MDEAAAABAmpmZPjMzU0AAAEBAEiEKAm5mEhsKBQoDAgMGEggKBgEKAgMECioICgYBAQIDAwMScAoHZGVmYXVsdBJlCgwKCgoDMS0yCgMyLTMSDQoFCgMBAgISBAoCAQIiB2RlZmF1bHQqB2RlZmF1bHQyNBIyCgJlZhIsCgQKAgMGEggKBgECCQIDChoaChjNzIw/zcwMQJqZIUHNzAxAMzNTQGZmBkAaCwoJEgdkZWZhdWx0"
        ]
        # pb_string_double = [pb_string[0], pb_string[0]]
        mock_batch_input = {"graph_feature": pb_string[0]}
        mock_batch_input_double = [mock_batch_input, mock_batch_input]
        sp_kv_specs = self.node_spec.GetSparseKVSpec()
        for f_name, spec in sp_kv_specs.items():
            print(f"======== f_name:{f_name}, f_spec_name:{spec.GetFeatureName()}")

        # ############### test when output is TorchBatchOfSubGraph ###############
        my_collate = AGLHomoCollateForPyG(
            self.node_spec, self.edge_spec, columns=[], uncompress=False
        )

        data = my_collate(mock_batch_input_double)
        self.assertTrue(isinstance(data, TorchSubGraphBatchData))
        # verify edge_index and edge indices
        ego_2_hop_gt = {0: (0, 1), 1: (1, 2), 2: (3, 4), 3: (4, 5)}
        data_edge_index = data.adjs_t.edge_index
        data_edge_indices = data.adjs_t.edge_indices  # data.agl["edge_indices"]
        for i in range(len(data_edge_indices)):
            print(
                f"edge index:{data_edge_indices[i]}, n1_index:{data_edge_index[1][i]}, n2_index:{data_edge_index[0][i]}"
            )
            self.assertEqual(
                ego_2_hop_gt[data_edge_indices.numpy()[i]][0],
                data_edge_index.numpy()[1][i],
            )
            self.assertEqual(
                ego_2_hop_gt[data_edge_indices.numpy()[i]][1],
                data_edge_index.numpy()[0][i],
            )

        # verify node dense feature
        n_df_one_gt = [[0.1, 1.1, 1], [0.2, 2.2, 2], [0.3, 3.3, 3]]
        n_df_gt = []
        n_df_gt.extend(n_df_one_gt)
        n_df_gt.extend(n_df_one_gt)
        dense_feature = data.n_feats.features["dense"].to_dense()
        self.assertAlmostEqual(np.array(n_df_gt).all(), dense_feature.numpy().all())

        # todo comment spkv now, dataloader 多进程时 sparse tensor 无法pickle
        # # verify node sp kv feature
        # n_spkv_collate = data.agl["node_feature"]["sparse_kv"][self.n_spkv_name]
        # print(f"n_spkv_collate:{n_spkv_collate} ")
        # n_spkv_ind_gt_two = [0, 2, 3, 6, 8, 9, 12]
        # n_spkv_key_gt_two = [1, 10, 2, 3, 4, 10, 1, 10, 2, 3, 4, 10]
        # n_spkv_val_gt_two = [1, 1, 2, 3, 3, 3, 1, 1, 2, 3, 3, 3]
        # self.assertListEqual(n_spkv_ind_gt_two, n_spkv_collate.crow_indices().numpy().tolist())
        # self.assertListEqual(n_spkv_key_gt_two, n_spkv_collate.col_indices().numpy().tolist())
        # self.assertListEqual(n_spkv_val_gt_two, n_spkv_collate.values().numpy().tolist())
        #
        # # verify edge sp kv feature
        # e_spkv_collate =  data.agl["edge_feature"]["sparse_kv"][self.e_kv_name]
        # print(f"e_spkv_collate:{e_spkv_collate}")
        # e_sp_kv_ind_gt_two = [0, 3, 6, 9, 12]
        # e_sp_kv_key_gt_two = [1, 2, 9, 2, 3, 10, 1, 2, 9, 2, 3, 10]
        # s_sp_kv_val_gt_two = [1.1, 2.2, 10.1, 2.2, 3.3, 2.1, 1.1, 2.2, 10.1, 2.2, 3.3, 2.1]
        # self.assertListEqual(e_sp_kv_ind_gt_two, e_spkv_collate.crow_indices().numpy().tolist())
        # self.assertListEqual(e_sp_kv_key_gt_two, e_spkv_collate.col_indices().numpy().tolist())
        # self.assertAlmostEqual(np.array(s_sp_kv_val_gt_two).all(), n_spkv_collate.values().numpy().all())

        my_collate2 = AGLHomoCollateForPyG(
            self.node_spec,
            self.edge_spec,
            columns=[],
            ego_edge_index=True,
            uncompress=False,
        )
        data2 = my_collate2(mock_batch_input_double)

        ego_2_hop_gt = {0: (0, 1), 1: (1, 2), 2: (3, 4), 3: (4, 5)}

        ego_1_hop_gt = {0: {0, 1}, 2: {3, 4}}

        e2_hop_adj = data2.adjs_t[0].edge_index.numpy()
        e1_hop_adj = data2.adjs_t[1].edge_index.numpy()

        e2_indices = data2.adjs_t[0].edge_indices.numpy()
        e1_indices = data2.adjs_t[1].edge_indices.numpy()

        for i in range(len(e2_indices)):
            e_t = e2_indices[i]
            self.assertEqual(ego_2_hop_gt[e_t][0], e2_hop_adj[1][i])
            self.assertEqual(ego_2_hop_gt[e_t][1], e2_hop_adj[0][i])

    def test_dynamic_data_parase(self):
        # node 上无特征，边上有时间维度特征，特征长度为1，类型为int64
        node_spec = NodeSpec("default", AGLDType.STR)
        # edge related spec
        edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
        edge_spec.AddDenseSpec("time", DenseFeatureSpec("time", 1, AGLDType.INT64))

        # label_column = AGLMultiDenseColumn(name="labels", dim=121, dtype=np.int64)
        root_id_column = AGLRowColumn(name="seed")
        graph_id_column = AGLRowColumn(name="node_id_list")
        root_time_column = AGLDenseColumn(name="time_list", dim=1, dtype=np.int64)

        my_collate = AGLHomoCollateForPyG(
            node_spec,
            edge_spec,
            columns=[root_id_column, graph_id_column, root_time_column],
            label_name=None,
            need_node_and_edge_num=True,
            uncompress=False,
        )

        gf = "ChsKB2RlZmF1bHQSEAoMCgoKAjM4CgQ4MjY1EgASZQoHZGVmYXVsdBJaCiIKIAoOMzhfODI2NV8yODA3LjAKDjM4XzgyNjVfMjc4NS4wEgwKBAoCAgISBAoCAQEiB2RlZmF1bHQqB2RlZmF1bHQyFAoSCgR0aW1lEgoIASIGCgT3FeEVGhQaEggBEg4KB2RlZmF1bHQSAwoBAA=="
        seed = "123"
        node_id_list = "123"
        time_list = "1000"
        data = {
            "graph_feature": [gf],
            "seed": [seed],
            "node_id_list": [node_id_list],
            "time_list": [time_list],
        }
        res = my_collate([data])
        print(res)
        self.assertTrue(isinstance(res, TorchSubGraphBatchData))
