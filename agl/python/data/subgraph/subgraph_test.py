#!/usr/bin/python
# coding: utf-8

import unittest
import os
import numpy as np

from agl.python.data.subgraph.subgraph import PySubGraph
from pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, NodeSpec, EdgeSpec


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

    @staticmethod
    def create_subgraph():
        sg = PySubGraph([SubGraphTest.node_spec], [SubGraphTest.edge_spec])
        script_dir = os.path.dirname(os.path.abspath(__file__))
        # the ground truth json result referring to: ./test_data/data1_content.txt
        file_name = os.path.join(script_dir, "./test_data/data1.txt")
        pb_string = []
        with open(file_name, "rb") as f:
            graph_feature_content = f.readline()
            pb_string.append(graph_feature_content)

        pb_string_double = [bytearray(pb_string[0]), bytearray(pb_string[0])]
        sg.from_pb_bytes(pb_string_double)
        return sg

    def verify_node_spec(self, node_spec):
        # step 1: check name and id dtype
        spec_n_name = node_spec.GetNodeName()
        self.assertEqual(self.n_name, spec_n_name)
        spec_n_id_dtyppe = node_spec.GetNodeIdDtype()
        self.assertEqual(spec_n_id_dtyppe, self.n_id_dtype)

        # step 2: dense spec
        d_specs = node_spec.GetDenseFeatureSpec()
        self.assertEqual(len(d_specs), 1)
        self.assertTrue(self.n_df_name in d_specs)
        d_spec = d_specs[self.n_df_name]
        self.assertEqual(d_spec.GetFeatureName(), self.n_df_name)
        self.assertEqual(d_spec.GetFeatureDtype(), self.n_df_dtype)
        self.assertEqual(d_spec.GetDim(), self.n_df_dim)

        # step 3: sparse kv spec
        spkv_specs = node_spec.GetSparseKVSpec()
        self.assertEqual(len(spkv_specs), 1)
        self.assertTrue(self.n_spkv_name in spkv_specs)
        spec_spkv = spkv_specs[self.n_spkv_name]
        self.assertEqual(spec_spkv.GetFeatureName(), self.n_spkv_name)
        self.assertEqual(spec_spkv.GetMaxDim(), self.n_max_dim)
        self.assertEqual(spec_spkv.GetKeyDtype(), self.n_key_dtype)
        self.assertEqual(spec_spkv.GetValDtype(), self.n_val_dtype)

    def verify_edge_spec(self, edge_spec):
        # step 1: check name and id dtype
        spec_e_name = edge_spec.GetEdgeName()
        self.assertEqual(self.e_name, spec_e_name)
        spec_e_id_dtype = edge_spec.GetEidDtype()
        self.assertEqual(spec_e_id_dtype, self.e_id_dtype)

        # step 2: sparse kv spec
        spkv_specs = edge_spec.GetSparseKVSpec()
        self.assertEqual(len(spkv_specs), 1)
        self.assertTrue(self.e_kv_name in spkv_specs)
        spec_spkv = spkv_specs[self.e_kv_name]
        self.assertEqual(spec_spkv.GetFeatureName(), self.e_kv_name)
        self.assertEqual(spec_spkv.GetMaxDim(), self.e_max_dim)
        self.assertEqual(spec_spkv.GetKeyDtype(), self.e_key_dtype)
        self.assertEqual(spec_spkv.GetValDtype(), self.e_val_dtype)

    def test_simple_case(self):
        # test specs
        self.verify_node_spec(self.node_spec)
        self.verify_edge_spec(self.edge_spec)

        sg = SubGraphTest.create_subgraph()
        # test node dense
        node_dense = sg.get_node_dense_feature(self.n_name, self.n_df_name)
        n_df_one_gt = [[0.1, 1.1, 1], [0.2, 2.2, 2], [0.3, 3.3, 3]]
        n_df_gt = []
        n_df_gt.extend(n_df_one_gt)
        n_df_gt.extend(n_df_one_gt)
        print(f"==========\n node dense:\n{node_dense}\n")
        self.assertAlmostEqual(np.array(n_df_gt).all(), node_dense.all())
        # test node sparse kv
        node_sparse_kv = sg.get_node_sparse_kv_feature(self.n_name, self.n_spkv_name)
        n_spkv_ind_gt_two = [0, 2, 3, 6, 8, 9, 12]
        n_spkv_key_gt_two = [1, 10, 2, 3, 4, 10, 1, 10, 2, 3, 4, 10]
        n_spkv_val_gt_two = [1, 1, 2, 3, 3, 3, 1, 1, 2, 3, 3, 3]
        print(f"==========\n node sparse:\n{node_sparse_kv}\n")
        self.assertListEqual(n_spkv_ind_gt_two, node_sparse_kv[0].tolist())
        self.assertListEqual(n_spkv_key_gt_two, node_sparse_kv[1].tolist())
        self.assertListEqual(n_spkv_val_gt_two, node_sparse_kv[2].tolist())
        # test edge sparse kv
        edge_sparse_kv = sg.get_edge_sparse_kv_feature(self.e_name, self.e_kv_name)
        print(f"==========\n edge sparse:\n{edge_sparse_kv}\n")
        e_sp_kv_ind_gt_two = [0, 3, 6, 9, 12]
        e_sp_kv_key_gt_two = [1, 2, 9, 2, 3, 10, 1, 2, 9, 2, 3, 10]
        s_sp_kv_val_gt_two = [
            1.1,
            2.2,
            10.1,
            2.2,
            3.3,
            2.1,
            1.1,
            2.2,
            10.1,
            2.2,
            3.3,
            2.1,
        ]
        self.assertListEqual(e_sp_kv_ind_gt_two, edge_sparse_kv[0].tolist())
        self.assertListEqual(e_sp_kv_key_gt_two, edge_sparse_kv[1].tolist())
        self.assertAlmostEqual(
            np.array(s_sp_kv_val_gt_two).all(), edge_sparse_kv[2].all()
        )
        # test node_num
        n_num = sg.get_node_num_per_sample()
        print(f"==========\n n_num per sample:\n{n_num}\n")
        n_num_gt_two = [3, 3]
        self.assertListEqual(n_num_gt_two, n_num[self.n_name])
        # test edge_num
        e_num = sg.get_edge_num_per_sample()
        print(f"==========\n e_num per sample:\n{e_num}\n")
        e_num_gt_two = [2, 2]
        self.assertListEqual(e_num_gt_two, e_num[self.e_name])
        # test ego graph
        ego_adj = sg.get_ego_edge_index(2)
        print(f"==========\n ego adj 2hops:\n{ego_adj}\n")
        ego_2_hop_gt = {0: (0, 1), 1: (1, 2), 2: (3, 4), 3: (4, 5)}

        ego_1_hop_gt = {0: {0, 1}, 2: {3, 4}}
        # test 2 -hop
        n1, n2, e_index = ego_adj[0][self.e_name]
        for i in range(len(e_index)):
            e_t = e_index[i]
            self.assertEqual(ego_2_hop_gt[e_t][0], n1[i])
            self.assertEqual(ego_2_hop_gt[e_t][1], n2[i])

        # test e_index
        e_index = sg.get_edge_index()
        print(e_index)
        edge_adj_gt_two = [0, 1, 2, 2, 3, 4, 4]
        edge_n2_indices_gt_two = [1, 2, 4, 5]
        self.assertListEqual(edge_adj_gt_two, e_index[self.e_name][0].tolist())
        self.assertListEqual(edge_n2_indices_gt_two, e_index[self.e_name][1].tolist())
