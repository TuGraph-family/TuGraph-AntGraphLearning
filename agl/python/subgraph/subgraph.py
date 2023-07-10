#!/usr/bin/python
# coding: utf-8

from typing import List
import numpy as np

from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray


class PySubGraph:
    def __init__(self, node_specs: List[NodeSpec], edge_specs: List[EdgeSpec]):
        self.sg = SubGraph()
        self.n_specs = node_specs
        self.e_specs = edge_specs
        for n_spec in node_specs:
            self.sg.AddNodeSpec(n_spec.GetNodeName(), n_spec)
        for e_spec in edge_specs:
            self.sg.AddEdgeSpec(e_spec.GetEdgeName(), e_spec)

    def from_pb(self, graph_features: List[str], is_merge: bool = False, uncompress: bool = False):
        # parse pb graph features
        self.sg.CreateFromPB(graph_features, is_merge, uncompress)

    def from_pb_bytes(self, graph_features: List[bytearray], is_merge: bool = False, uncompress: bool = False):
        self.sg.CreateFromPBBytesArray(graph_features, is_merge, uncompress)

    def get_edge_index(self):
        result = {}
        for e_spec in self.e_specs:
            name = e_spec.GetEdgeName()
            csr = self.sg.GetEdgeIndexCSR(name)
            ind_offset = np.squeeze(np.array(csr.GetIndPtr()), axis=-1)
            n2_indices = np.squeeze(np.array(csr.GetIndices()), axis=-1)
            edge_index = np.arange(len(n2_indices), dtype=ind_offset.dtype)
            result.update({name: (ind_offset, n2_indices, edge_index, csr.row_num, csr.col_num)})
        return result

    def get_ego_edge_index(self, hops: int):
        # 需要用户判断自己的模型是否适合 ego 模式
        res = self.sg.GetEgoEdgeIndex(hops)
        res_size = len(res)
        res_final = []
        for i in range(res_size):
            one_hop_dict = {}
            for e_name, e_coo in res[i].items():
                n1_indices = np.squeeze(np.array(e_coo.GetN1Indices()), axis=-1)
                n2_indices = np.squeeze(np.array(e_coo.GetN2Indices()), axis=-1)
                e_indices = np.squeeze(np.array(e_coo.GetEdgeIndex()), axis=-1)
                one_hop_dict.update(
                    {
                        e_name: (n1_indices, n2_indices, e_indices)
                    }
                )
            res_final.insert(0, one_hop_dict)
        return res_final

    def get_node_dense_feature(self, node_name, f_name):
        res = self.sg.GetNodeDenseFeatureArray(node_name, f_name)
        n_d_array = res.GetFeatureArray()
        py_n_d_array = np.array(n_d_array)
        return py_n_d_array

    def get_node_sparse_kv_feature(self, node_name, f_name):
        res = self.sg.GetNodeSparseKVArray(node_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        values = np.squeeze(np.array(f_array.GetVals()), axis=-1)
        return ind_offset, keys, values

    def get_node_sparse_k_feature(self, node_name, f_name):
        # todo zdl add check and raise error if not exists
        res = self.sg.GetNodeSparseKArray(node_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        return ind_offset, keys

    def get_edge_dense_feature(self, edge_name, f_name):
        res = self.sg.GetEdgeDenseFeatureArray(edge_name, f_name)
        n_d_array = res.GetFeatureArray()
        py_n_d_array = np.array(n_d_array)
        return py_n_d_array

    def get_edge_sparse_kv_feature(self, edge_name, f_name):
        res = self.sg.GetEdgeSparseKVArray(edge_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        values = np.squeeze(np.array(f_array.GetVals()), axis=-1)
        return ind_offset, keys, values

    def get_edge_sparse_k_feature(self, edge_name, f_name):
        res = self.sg.GetEdgeSparseKArray(edge_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        return ind_offset, keys

    def get_node_num_per_sample(self):
        return self.sg.GetNodeNumPerSample()

    def get_edge_num_per_sample(self):
        return self.sg.GetEdgeNumPerSample()

    def get_root_index(self):
        return self.sg.GetRootIds()
