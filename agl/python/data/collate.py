#!/usr/bin/python
# coding: utf-8

from abc import ABC, abstractmethod
from typing import List

import torch
import time
from torch_geometric.data import Data

from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray
from agl.python.subgraph.subgraph import PySubGraph
from agl.python.data.column import AGLColumn, AGLDenseColumn, AGLRowColumn
from agl.python.subgraph.pyg_inputs import TorchEdgeIndex, TorchFeature, TorchDenseFeature, TorchSparseFeature, \
    TorchFeatures, TorchEgoBatchData, TorchSubGraphBatchData


class AGLCollate(ABC):

    def __call__(self, batch_input):
        return self.call(batch_input)

    @abstractmethod
    def call(self, batch_input):
        raise NotImplementedError(f"{self.__class__}.Collate")


class AGLHomoCollateForPyG(AGLCollate):
    def __init__(self,
                 node_spec: NodeSpec, edge_spec: EdgeSpec,
                 columns: List[AGLColumn],
                 graph_feature_name: str = "graph_feature",
                 label_name: str = "label",
                 need_node_and_edge_num: bool = False,
                 ego_edge_index: bool = False,
                 hops: int = 2):
        super().__init__()
        if ego_edge_index and need_node_and_edge_num:
            raise NotImplementedError(
                "now the options ego_edge_index and need_node_and_edge_num are mutually exclusive!")
        self._node_spec = node_spec
        self._edge_spec = edge_spec
        self._graph_feature_name = graph_feature_name
        self._label_name = label_name
        # todo 目前 column 只支持dense, raw, 后续支持 GraphFeatureColumn
        self._columns = columns
        self._need_node_and_edge_num = need_node_and_edge_num
        self._ego_edge_index = ego_edge_index
        self._hops = hops

    def call(self, batch_input):
        
        t1 = time.time()
        # step 1: 把输入从 list of dict 转换为 dict of list. 考虑一下是否用 chain 做处理？
        input_dict = self.format_batch_input(batch_input)
        t2 = time.time()
        # step 2: prepare subgraph and parse
        sg = PySubGraph([self._node_spec], [self._edge_spec])
        gfs = input_dict[self._graph_feature_name]
        assert len(gfs) > 0
        if isinstance(gfs[0], bytes):
            gfs_bytearray = [bytearray(gf_t) for gf_t in gfs]
            sg.from_pb_bytes(gfs_bytearray)
        elif isinstance(gfs[0], bytearray):
            sg.from_pb_bytes(gfs)
        elif isinstance(gfs[0], str):
            sg.from_pb(gfs)
        else:
            raise NotImplementedError("only support string, bytes, bytearray")
        t3 = time.time()
        # step 3: node feature
        n_fs = AGLHomoCollateForPyG.get_node_features(sg, self._node_spec)
        t4 = time.time()
        # step 4: edge_feature
        e_fs = AGLHomoCollateForPyG.get_edge_features(sg, self._edge_spec)
        t5 = time.time()
        # step 5: adj矩阵相关信息
        if self._ego_edge_index and not self._need_node_and_edge_num:
            edge_index = self.get_ego_edge_index(sg, self._hops, self._edge_spec)
        else:
            edge_index = AGLHomoCollateForPyG.get_edge_index(sg, self._edge_spec)
        t6 = time.time()
        # step 6: root index 信息
        root = self.get_root_index(sg, self._node_spec)
        # optional step 7: 是否需要每个样本的node num 和 edge_num
        n_num, e_num = None, None
        if self._need_node_and_edge_num and not self._ego_edge_index:
            n_num, e_num = self.get_node_edge_num(sg, self._node_spec, self._edge_spec)
        t7 = time.time()
        t_l_s = 0
        t_l_e = 0
        # step 7: 其他column的parse
        label = None
        other_feats = {}
        other_raw = {}
        for column in self._columns:
            # todo 除了 label 列，其他列暂时无法输出
            c_name = column.name
            assert c_name in input_dict
            if c_name == self._label_name:
                t_l_s = time.time()
                label = column(input_dict[c_name])
                t_l_e = time.time()
            elif isinstance(column, AGLRowColumn):
                # may not be able to transformer to torch tensor, such as string nd array
                other_raw.update({c_name: column(input_dict[c_name])})
            else:
                other_feats.update({c_name: column(input_dict[c_name])})
        t_end = time.time()
        # print(f">>>>>>>>> total in collate: {t_end-t1:.4f}, format_input: {t2-t1:.4f}, "
        #       f"parse_feature: {t3-t2:.4f}, nf_get: {t4-t3:.4f}, ef_get:{t5-t4:.4f}, adj_get:{t6-t5:.4f}, "
        #       f"root_and_num: {t7-t6:.4f}, other_column: {t_end-t7}, label_time: {t_l_e - t_l_s:.4f}")
        if self._ego_edge_index:
            return TorchEgoBatchData.create_from_tensor(n_feats=n_fs,
                                                        e_feats=e_fs,
                                                        y=label,
                                                        adjs_t=edge_index,
                                                        root_index=root,
                                                        other_feats=other_feats,
                                                        other_raw=other_raw)
        else:
            return TorchSubGraphBatchData.create_from_tensor(n_feats=n_fs,
                                                             e_feats=e_fs,
                                                             y=label,
                                                             adjs_t=edge_index,
                                                             root_index=root,
                                                             n_num_per_sample=n_num,
                                                             e_num_per_sample=e_num,
                                                             other_feats=other_feats,
                                                             other_raw=other_raw)

    @staticmethod
    def format_batch_input(batch_input):
        elem = batch_input[0]
        assert isinstance(elem, dict)
        tmp_keys = list(elem.keys())
        input_dict = {k: [] for k in tmp_keys}
        for data_ in batch_input:
            for key in tmp_keys:
                input_dict[key].append(data_[key])
        return input_dict

    @staticmethod
    def get_node_features(sg: PySubGraph, node_spec: NodeSpec):
        n_name = node_spec.GetNodeName()
        # dense features
        dense_specs = node_spec.GetDenseFeatureSpec()
        features = {}
        # n_df_dict = {}
        for f_name, spec in dense_specs.items():
            df = sg.get_node_dense_feature(n_name, f_name)
            df_t = torch.from_numpy(df)
            features.update({f_name: TorchDenseFeature.create_from_tensor(df_t)})

        # sparse kv features
        sp_kv_specs = node_spec.GetSparseKVSpec()
        for f_name, spec in sp_kv_specs.items():
            ind_offset, keys, values = sg.get_node_sparse_kv_feature(n_name, f_name)
            max_dim = spec.GetMaxDim()
            # todo n_num now use len(ind_offset) - 1
            n_num = len(ind_offset) - 1
            # todo 是否需要转换为 coo ?
            sp_feature = TorchSparseFeature.create_from_csr_tensor(
                row_ptr=torch.from_numpy(ind_offset), 
                col=torch.from_numpy(keys), 
                value=torch.from_numpy(values),
                size=[n_num, max_dim]
            )
            features.update({f_name: sp_feature})

        return TorchFeatures.create_from_torch_feature(features)

    @staticmethod
    def get_edge_features(sg: PySubGraph, edge_spec: EdgeSpec):
        e_name = edge_spec.GetEdgeName()
        # dense features
        dense_specs = edge_spec.GetDenseFeatureSpec()
        features = {}
        for f_name, spec in dense_specs.items():
            df = sg.get_edge_dense_feature(e_name, f_name)
            df_t = torch.from_numpy(df)
            features.update({f_name: TorchDenseFeature.create_from_tensor(df_t)})

        # sparse kv features
        sp_kv_specs = edge_spec.GetSparseKVSpec()
        for f_name, spec in sp_kv_specs.items():
            ind_offset, keys, values = sg.get_edge_sparse_kv_feature(e_name, f_name)
            max_dim = spec.GetMaxDim()
            # todo n_num now use len(ind_offset) - 1
            n_num = len(ind_offset) - 1
            # todo 是否需要转换为 coo ?
            sp_feature = TorchSparseFeature.create_from_csr_tensor(
                row_ptr=torch.from_numpy(ind_offset),
                col=torch.from_numpy(keys),
                value=torch.from_numpy(values),
                size=[n_num, max_dim]
            )
            features.update({f_name: sp_feature})

        return TorchFeatures.create_from_torch_feature(features)

    @staticmethod
    def get_node_edge_num(sg: PySubGraph, node_spec: NodeSpec, edge_spec: EdgeSpec):
        n_name = node_spec.GetNodeName()
        n_num = sg.get_node_num_per_sample()
        assert n_name in n_num
        res_n_num = torch.as_tensor(n_num[n_name])

        e_name = edge_spec.GetEdgeName()
        e_num = sg.get_edge_num_per_sample()
        assert e_name in e_num
        res_e_num = torch.as_tensor(e_num[e_name])

        return res_n_num, res_e_num

    @staticmethod
    def get_root_index(sg: PySubGraph, node_spec: NodeSpec):
        n_name = node_spec.GetNodeName()
        root_dict = sg.get_root_index()
        assert n_name in root_dict
        root_one = root_dict[n_name]
        import itertools
        import numpy as np
        # todo 对于一个样本有多个label的情况，目前先打平处理
        flattened_lst = list(itertools.chain.from_iterable(root_one))
        final_root = np.asarray(flattened_lst)
        return torch.as_tensor(final_root)

    @staticmethod
    def get_ego_edge_index(sg: PySubGraph, hops, edge_spec: EdgeSpec):
        t0 = time.time()
        res = sg.get_ego_edge_index(hops)
        t1 = time.time()
        assert len(res) == hops
        e_name = edge_spec.GetEdgeName()
        torch_index_list = []
        for r_ in res:
            n1, n2, e = r_[e_name]
            # edge_index = torch.stack([torch.as_tensor(n2), torch.as_tensor(n1)], dim=0)
            # torch_edge_index_i = TorchEdgeIndex.create_from_tensor(adj=edge_index,
            #                                                        size=None,
            #                                                        edge_indices=torch.as_tensor(e))
            torch_edge_index_i = TorchEdgeIndex.create_from_coo_tensor(src=torch.as_tensor(n2), dst=torch.as_tensor(n1),
                                                                       size=None,
                                                                       edge_indices=torch.as_tensor(e))
            torch_index_list.append(torch_edge_index_i)
        t2 = time.time()
        #print(f"ego edge total: {t2 -t0}, call c++: {t1 - t0}, tensor: {t2 - t1}")
        
        return torch_index_list

    @staticmethod
    def get_edge_index(sg: PySubGraph, edge_spec: EdgeSpec):
        t0 = time.time()
        e_index = sg.get_edge_index()
        t1 = time.time()
        e_name = edge_spec.GetEdgeName()
        ind_offset, indices, edge_f_index, n1_num, n2_num = e_index[e_name]
        # # todo use torch_sparse and do not sort
        # from torch_sparse import SparseTensor
        # # is sorted should be Ture, otherwise, edge_f_index [0~nnz-1] would be changed
        # sp = SparseTensor(rowptr=torch.from_numpy(ind_offset), col=torch.from_numpy(indices),
        #                   value=torch.from_numpy(edge_f_index), sparse_sizes=(n1_num, n2_num), is_sorted=True)
        # coo = sp.coo()
        # dst_indices, src_indices, edge_indices = coo[0], coo[1], coo[2]
        # # 转换index 方向
        # edge_index = torch.stack([src_indices, dst_indices], dim=0)
        # t2 = time.time()
        # print(f">>>>> get_edge_index_total: {t2-t0}, call_c++: {t1-t0}, tensor: {t2-t1}")
        # return TorchEdgeIndex.create_from_tensor(adj=edge_index, size=[n2_num, n1_num], edge_indices=edge_indices)
        return TorchEdgeIndex.create_from_csr_tensor(row_ptr=torch.from_numpy(ind_offset),
                                                     col=torch.from_numpy(indices),
                                                     size=(n1_num, n2_num),
                                                     edge_indices=torch.from_numpy(edge_f_index))
