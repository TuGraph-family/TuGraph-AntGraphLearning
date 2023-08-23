#!/usr/bin/python
# coding: utf-8
from typing import List, Union, Callable, Optional, Any, Dict

from pyagl.pyagl import (
    NodeSpec,
    EdgeSpec,
)
from agl.python.data.subgraph.subgraph import PySubGraph
from agl.python.data.column import AGLColumn, AGLRowColumn
from agl.python.data.subgraph.pyg_inputs import (
    TorchEgoBatchData,
    TorchSubGraphBatchData,
)
from agl.python.data.collate import AGLHomoCollateForPyG


class MultiGraphFeatureCollate:
    def __init__(
        self,
        node_spec: NodeSpec,
        edge_spec: EdgeSpec,
        columns: List[AGLColumn],
        graph_feature_name: Union[str, List[str]] = "graph_feature",
        label_name: str = "label",
        need_node_and_edge_num: bool = False,
        ego_edge_index: bool = False,
        hops: int = 2,
        uncompress: bool = True,
        pre_transform: Optional[Callable] = None,
        after_transform: Optional[Callable] = None,
    ):
        """
        parse graph feature and other columns (like label and so on). if you have several graph feature column,
        label and other column will only appear in the first XXXBatchData instance.

        Args:
            node_spec(NodeSpec): node spec
            edge_spec(EdgeSpec): edge spec
            columns(List[AGLColumn]): column information to parse related columns
            graph_feature_name(str): graph feature column name
            label_name(str): label column name
            need_node_and_edge_num (bool): whether the number of nodes/edges per sample needs to be returned.
            ego_edge_index(bool): Whether the result return ego edge_index or plain edge index.
            note need_node_and_edge_num and ego_edge_index are mutually exclusive now.
            hops(int): if return ego_edge_index, how many hops should return
            uncompress(bool): Whether the pbs should be un-compressed
            pre_transform(Optional[Callable]): pre transformer applied to batch input
            after_transform(Optional[Callable]): after_transform applied to collate, usually for graph data
        """
        if ego_edge_index and need_node_and_edge_num:
            raise NotImplementedError(
                "now the options ego_edge_index and need_node_and_edge_num are mutually exclusive!"
            )
        self._node_spec = node_spec
        self._edge_spec = edge_spec
        self._graph_feature_name = graph_feature_name
        self._label_name = label_name
        self._columns = columns
        self._need_node_and_edge_num = need_node_and_edge_num
        self._ego_edge_index = ego_edge_index
        self._hops = hops
        self._uncompress = uncompress
        self._pre_transform = pre_transform
        self._after_transform = after_transform

    def __call__(self, batch_inputs: List[Dict]):
        return self._call(batch_inputs)

    def _call_single_feature_parse(
        self, input_dict: Dict[str, Any], graph_features, chief: bool = True
    ):
        # step 1: prepare subgraph and parse
        sg = PySubGraph([self._node_spec], [self._edge_spec])
        gfs = graph_features
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

        # step 2: node feature
        n_fs = AGLHomoCollateForPyG.get_node_features(sg, self._node_spec)
        # step 3: edge_feature
        e_fs = AGLHomoCollateForPyG.get_edge_features(sg, self._edge_spec)
        # step 4: adj related information
        if self._ego_edge_index and not self._need_node_and_edge_num:
            edge_index = AGLHomoCollateForPyG.get_ego_edge_index(
                sg, self._hops, self._edge_spec
            )
        else:
            edge_index = AGLHomoCollateForPyG.get_edge_index(sg, self._edge_spec)
        # step 5: root index information
        root = AGLHomoCollateForPyG.get_root_index(sg, self._node_spec)
        # optional step 6: get node_num/edge_num per sample
        n_num, e_num = None, None
        if self._need_node_and_edge_num and not self._ego_edge_index:
            n_num, e_num = AGLHomoCollateForPyG.get_node_edge_num(
                sg, self._node_spec, self._edge_spec
            )
        # step 7: parse other columns
        label = None
        other_feats = {}
        other_raw = {}
        if chief:
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
            res = TorchEgoBatchData.create_from_tensor(
                n_feats=n_fs,
                e_feats=e_fs,
                y=label,
                adjs_t=edge_index,
                root_index=root,
                other_feats=other_feats,
                other_raw=other_raw,
            )
        else:
            res = TorchSubGraphBatchData.create_from_tensor(
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
        return res if self._after_transform is None else self._after_transform(res)

    def _call_single(self, input_dict, name: str, chief=True):
        graph_feature_column = input_dict[name]
        assert isinstance(graph_feature_column, List)
        if isinstance(graph_feature_column[0], List):
            ret = []
            for i in range(len(graph_feature_column)):
                if i != 0:
                    chief = chief and False
                ret.append(
                    self._call_single_feature_parse(
                        input_dict, graph_feature_column[i], chief
                    )
                )
            return ret
        else:
            return self._call_single_feature_parse(
                input_dict, graph_feature_column, chief
            )

    def _call(self, batch_inputs):
        # step 1: transform data to dict of list
        input_dict = AGLHomoCollateForPyG.format_batch_input(batch_inputs)
        # step 2: apply pre_transform to input dict
        input_dict = (
            input_dict
            if self._pre_transform is None
            else self._pre_transform(input_dict)
        )
        # step 3: call single graph feature process
        if isinstance(self._graph_feature_name, str):
            return self._call_single(input_dict, self._graph_feature_name)
        elif isinstance(self._graph_feature_name, List):
            ret = []
            i = 0
            for name in self._graph_feature_name:
                if i == 0:
                    ret.append(self._call_single(input_dict, name))
                else:
                    ret.append(self._call_single(input_dict, name, chief=False))
                i = i + 1
            return ret
