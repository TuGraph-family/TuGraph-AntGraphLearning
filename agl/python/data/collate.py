#!/usr/bin/python
# coding: utf-8
import torch
from typing import List

from pyagl.pyagl import (
    NodeSpec,
    EdgeSpec,
)
from agl.python.data.subgraph.subgraph import PySubGraph
from agl.python.data.column import AGLColumn, AGLRowColumn
from agl.python.data.subgraph.pyg_inputs import (
    TorchEdgeIndex,
    TorchDenseFeature,
    TorchSparseFeature,
    TorchFeatures,
    TorchEgoBatchData,
    TorchSubGraphBatchData,
)


class AGLHomoCollateForPyG:
    """AGL homograph collate fn for PyG

    (1) create subgraph with specific node_spec and edge_spec
    (2) parse pb graph features and get related node/edge feature, edge_index, root index
    (3) parse other column of samples, like label, raw_id and so on
    (4) use a container to store information above

    """

    def __init__(
        self,
        node_spec: NodeSpec,
        edge_spec: EdgeSpec,
        columns: List[AGLColumn],
        graph_feature_name: str = "graph_feature",
        label_name: str = "label",
        need_node_and_edge_num: bool = False,
        ego_edge_index: bool = False,
        hops: int = 2,
        uncompress: bool = True,
    ):
        """

        Args:
            node_spec(NodeSpec): node spec
            edge_spec(EdgeSpec): edge spec
            columns(List[AGLColumn]): column information to parse related columns
            graph_feature_name(str): graph feature column name
            label_name(str): label column name
            need_node_and_edge_num (bool): Whether should return node/edge num per sample
            ego_edge_index(bool): Whether should return ego edge_index or plain edge index.
            note need_node_and_edge_num and ego_edge_index are mutually exclusive now.
            hops(int): if return ego_edge_index, how many hops should return
            uncompress(bool): Whether the pbs should be un-compressed
        """
        if ego_edge_index and need_node_and_edge_num:
            raise NotImplementedError(
                "now the options ego_edge_index and need_node_and_edge_num are mutually exclusive!"
            )
        self._node_spec = node_spec
        self._edge_spec = edge_spec
        self._graph_feature_name = graph_feature_name
        self._label_name = label_name
        # todo now column only support dense, raw
        self._columns = columns
        self._need_node_and_edge_num = need_node_and_edge_num
        self._ego_edge_index = ego_edge_index
        self._hops = hops
        self._uncompress = uncompress

    def __call__(self, batch_input):
        return self.call(batch_input)

    def call(self, batch_input):
        """

        Args:
            batch_input: a batch of sample records

        Returns: TorchEgoBatchData if ego_edge_index is set to True, otherwise return TorchSubGraphBatchData

        """
        # step 1: transform data to dict of list
        input_dict = self.format_batch_input(batch_input)
        # step 2: prepare subgraph and parse
        sg = PySubGraph([self._node_spec], [self._edge_spec])
        gfs = input_dict[self._graph_feature_name]
        assert len(gfs) > 0
        if isinstance(gfs[0], bytes):
            gfs_bytearray = [bytearray(gf_t) for gf_t in gfs]
            sg.from_pb_bytes(gfs_bytearray, uncompress=self._uncompress)
        elif isinstance(gfs[0], bytearray):
            sg.from_pb_bytes(gfs, uncompress=self._uncompress)
        elif isinstance(gfs[0], str):
            sg.from_pb(gfs, uncompress=self._uncompress)
        else:
            raise NotImplementedError("only support string, bytes, bytearray")
        # step 3: node feature
        n_fs = AGLHomoCollateForPyG.get_node_features(sg, self._node_spec)
        # step 4: edge_feature
        e_fs = AGLHomoCollateForPyG.get_edge_features(sg, self._edge_spec)
        # step 5: adj related information
        if self._ego_edge_index and not self._need_node_and_edge_num:
            edge_index = self.get_ego_edge_index(sg, self._hops, self._edge_spec)
        else:
            edge_index = AGLHomoCollateForPyG.get_edge_index(sg, self._edge_spec)
        # step 6: root index related information
        root = self.get_root_index(sg, self._node_spec)
        # optional step 7: get node_num/edge_num per sample
        n_num, e_num = None, None
        if self._need_node_and_edge_num and not self._ego_edge_index:
            n_num, e_num = self.get_node_edge_num(sg, self._node_spec, self._edge_spec)
        # step 7: parse other column
        label = None
        other_feats = {}
        other_raw = {}
        for column in self._columns:
            c_name = column.name
            assert c_name in input_dict
            if c_name == self._label_name:
                label = column(input_dict[c_name])
            elif isinstance(column, AGLRowColumn):
                # may not be able to transformer to torch tensor, such as string nd array
                other_raw.update({c_name: column(input_dict[c_name])})
            else:
                other_feats.update({c_name: column(input_dict[c_name])})

        if self._ego_edge_index:
            return TorchEgoBatchData.create_from_tensor(
                n_feats=n_fs,
                e_feats=e_fs,
                y=label,
                adjs_t=edge_index,
                root_index=root,
                other_feats=other_feats,
                other_raw=other_raw,
            )
        else:
            return TorchSubGraphBatchData.create_from_tensor(
                n_feats=n_fs,
                e_feats=e_fs,
                y=label,
                adjs_t=edge_index,
                root_index=root,
                n_num_per_sample=n_num,
                e_num_per_sample=e_num,
                other_feats=other_feats,
                other_raw=other_raw,
            )

    @staticmethod
    def format_batch_input(batch_input):
        elem = batch_input[0]
        assert isinstance(elem, dict)
        tmp_keys = list(elem.keys())
        # map-able output [dict1, dict2]  list of dict，
        # iterable output [dict(key:list)]] batch_input[0] is dict of list
        if len(batch_input) == 1 and isinstance(elem[tmp_keys[0]], list):
            # directly return
            return elem
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
        for f_name, spec in dense_specs.items():
            df = sg.get_node_dense_feature(n_name, f_name)
            df_t = torch.from_numpy(df)
            features.update({f_name: TorchDenseFeature.create(df_t)})

        # sparse kv features
        sp_kv_specs = node_spec.GetSparseKVSpec()
        for f_name, spec in sp_kv_specs.items():
            ind_offset, keys, values = sg.get_node_sparse_kv_feature(n_name, f_name)
            max_dim = spec.GetMaxDim()
            # todo n_num now use len(ind_offset) - 1
            n_num = len(ind_offset) - 1
            sp_feature = TorchSparseFeature.create_from_csr(
                row_ptr=torch.from_numpy(ind_offset),
                col=torch.from_numpy(keys),
                value=torch.from_numpy(values),
                size=[n_num, max_dim],
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
            features.update({f_name: TorchDenseFeature.create(df_t)})

        # sparse kv features
        sp_kv_specs = edge_spec.GetSparseKVSpec()
        for f_name, spec in sp_kv_specs.items():
            ind_offset, keys, values = sg.get_edge_sparse_kv_feature(e_name, f_name)
            max_dim = spec.GetMaxDim()
            # todo n_num now use len(ind_offset) - 1
            n_num = len(ind_offset) - 1
            sp_feature = TorchSparseFeature.create_from_csr(
                row_ptr=torch.from_numpy(ind_offset),
                col=torch.from_numpy(keys),
                value=torch.from_numpy(values),
                size=[n_num, max_dim],
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

        # todo For cases where a sample has multiple labels, we currently flatten them.
        flattened_lst = list(itertools.chain.from_iterable(root_one))
        final_root = np.asarray(flattened_lst)
        return torch.as_tensor(final_root)

    @staticmethod
    def get_ego_edge_index(sg: PySubGraph, hops, edge_spec: EdgeSpec):
        res = sg.get_ego_edge_index(hops)
        assert len(res) == hops
        e_name = edge_spec.GetEdgeName()
        torch_index_list = []
        for r_ in res:
            n1, n2, e = r_[e_name]
            torch_edge_index_i = TorchEdgeIndex.create_from_coo_tensor(
                src=torch.as_tensor(n2),
                dst=torch.as_tensor(n1),
                size=None,
                edge_indices=torch.as_tensor(e),
            )
            torch_index_list.append(torch_edge_index_i)

        return torch_index_list

    @staticmethod
    def get_edge_index(sg: PySubGraph, edge_spec: EdgeSpec):
        e_index = sg.get_edge_index()
        e_name = edge_spec.GetEdgeName()
        ind_offset, indices, edge_f_index, n1_num, n2_num = e_index[e_name]
        return TorchEdgeIndex.create_from_csr_tensor(
            row_ptr=torch.from_numpy(ind_offset),
            col=torch.from_numpy(indices),
            size=(n1_num, n2_num),
            edge_indices=torch.from_numpy(edge_f_index),
        )
