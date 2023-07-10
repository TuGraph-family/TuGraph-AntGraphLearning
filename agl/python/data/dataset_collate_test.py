#!/usr/bin/python
# coding: utf-8

import unittest
import os
import subprocess
import numpy as np
from torch.utils.data import DataLoader

from agl.python.data.dataset import PPITorchDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn
from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray


class DatasetAndCollateFnTest(unittest.TestCase):
    # http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/zdl_sync%2Ftest_file.txt
    file = "test_file.txt"
    schema = ["id", "graph_feature", "label"]
    record_num = 9
    r_id = ['10002', '10013', '1002', '10024', '10086', '10088', '10177', '10184', '10203']
    schema_sep = '\t'

    @staticmethod
    def download():
        # todo file 路径问题
        script_dir = os.path.dirname(os.path.abspath(__file__))
        file_name = os.path.join(script_dir, DatasetAndCollateFnTest.file)
        subprocess.check_call(['sh', '-c',
                               f'test -f test_file.txt || wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/zdl_sync%2Ftest_file.txt -O {file_name}'])

    def test_dataset(self):
        DatasetAndCollateFnTest.download()
        script_dir = os.path.dirname(os.path.abspath(__file__))
        file_name = os.path.join(script_dir, DatasetAndCollateFnTest.file)
        script_dir = os.path.dirname(os.path.abspath(__file__))
        my_test_dataset = PPITorchDataset(file_name, False, script_dir)
        self.assertEqual(my_test_dataset.len(), self.record_num)

        # 1. node related spec
        n_name = "default"
        n_id_dtype = AGLDType.STR

        # 1.1 node sp kv spec
        n_spkv_name = "sparse_kv"
        n_max_dim = 50
        n_key_dtype = AGLDType.INT64
        n_val_dtype = AGLDType.FLOAT
        node_spec = NodeSpec(n_name, n_id_dtype)
        # node_spec.AddDenseSpec(n_df_name, DenseFeatureSpec(n_df_name, n_df_dim, n_df_dtype))
        node_spec.AddSparseKVSpec(n_spkv_name, SparseKVSpec(n_spkv_name, n_max_dim, n_key_dtype, n_val_dtype))

        # 2. edge related spec
        e_name = "default"
        n1_name = "default"
        n2_name = "default"
        e_id_dtype = AGLDType.STR
        # 2.1 edge spkv spec
        e_kv_name = "sparse_kv"
        e_max_dim = 1
        e_key_dtype = AGLDType.INT64
        e_val_dtype = AGLDType.FLOAT
        edge_spec = EdgeSpec(e_name, node_spec, node_spec, e_id_dtype)
        edge_spec.AddSparseKVSpec(e_kv_name, SparseKVSpec(e_kv_name, e_max_dim, e_key_dtype, e_val_dtype))

        label_column = AGLDenseColumn(name="label", dim = 121, dtype=np.int64, sep=" ")
        id_column = AGLRowColumn(name="id")
        my_collate = AGLHomoCollateForPyG(node_spec, edge_spec, columns=[label_column, id_column])
        train_loader = DataLoader(dataset=my_test_dataset,
                                  batch_size=5,
                                  shuffle=False,
                                  collate_fn=my_collate,
                                  num_workers=2)
        for i, data in enumerate(train_loader):
            print(f'i:{i}, data:{data}')




