#!/usr/bin/python
# coding: utf-8

from typing import List
import numpy as np

from pyagl import (
    AGLDType,
    DenseFeatureSpec,
    SparseKVSpec,
    SparseKSpec,
    NodeSpec,
    EdgeSpec,
    SubGraph,
    NDArray,
)


class PySubGraph:
    """PySubGraph

    call c++ subgraph to parse pbs, and get edge_index, node feature, edge feature from the subgraph.

    >>> node_spec = NodeSpec("n", AGLDType.STR)
    >>> edge_spec = EdgeSpec("e", node_spec, node_spec, AGLDType.STR)
    >>> edge_spec.AddDenseSpec("time", DenseFeatureSpec("time", 1, AGLDType.INT64))
    >>> sg = PySubGraph([node_spec], [edge_spec])
    >>> sg.from_pb(["xxx"])
    >>> print(sg.get_edge_index())
    >>> {"e": (xxx, xxx, xxx, xxx, xxx)} # {edge_name: (row_ptr, col_indices, edge_indices, row_num, col_num)}

    """

    def __init__(self, node_specs: List[NodeSpec], edge_specs: List[EdgeSpec]):
        """

        Args:
            node_specs(List[NodeSpec]): list of node specs, each type of node has a node_spec
            edge_specs(List[EdgeSpec]): list of edge specs, each type of edge has a edge spec
        """
        self.sg = SubGraph()
        self.n_specs = node_specs
        self.e_specs = edge_specs
        for n_spec in node_specs:
            self.sg.AddNodeSpec(n_spec.GetNodeName(), n_spec)
        for e_spec in edge_specs:
            self.sg.AddEdgeSpec(e_spec.GetEdgeName(), e_spec)

    def from_pb(
        self,
        graph_features: List[str],
        is_merge: bool = False,
        uncompress: bool = False,
    ):
        """from_pb pass list of pb strings to c++, to parse pbs into a subgraph

        Args:
            graph_features(List[str]): a list of pb strings, each represents a subgraph wrt. to a certain
            or a set of nodes.
            is_merge(bool): Default is False. Whether merge those subgraphs into one subgraph.
            now only support disjoint merge (is_merge=False).
            uncompress(bool): whether the pb strings should be um-compressed by default compress algorithm (i.e., gzip)

        """
        # parse pb graph features
        self.sg.CreateFromPB(graph_features, is_merge, uncompress)

    def from_pb_bytes(
        self,
        graph_features: List[bytearray],
        is_merge: bool = False,
        uncompress: bool = False,
    ):
        """from_pb_bytes efficiently pass a list of pb bytearray to c++, to parse pbs into a subgraph

        Args:
            graph_features(List[bytearray]):  a list of pb strings (encoded with utf-8 to efficiently pass to c++),
            each represents a subgraph wrt. to a certain or a set of nodes.
            is_merge(bool): Default is False. Whether merge those subgraphs into one subgraph.
            now only support disjoint merge (is_merge=False).
            uncompress(bool): whether the pb strings should be um-compressed by default compress algorithm (i.e., gzip)

        """
        self.sg.CreateFromPBBytesArray(graph_features, is_merge, uncompress)

    def get_edge_index(self):
        """

        Returns: {edge_name: (row_ptr, col_indices, edge_indices, row_num, col_num)}. A dict of edge indexes wrt.
        to their types.

        """
        result = {}
        for e_spec in self.e_specs:
            name = e_spec.GetEdgeName()
            csr = self.sg.GetEdgeIndexCSR(name)
            ind_offset = np.squeeze(np.array(csr.GetIndPtr()), axis=-1)
            n2_indices = np.squeeze(np.array(csr.GetIndices()), axis=-1)
            edge_index = np.arange(len(n2_indices), dtype=ind_offset.dtype)
            result.update(
                {name: (ind_offset, n2_indices, edge_index, csr.row_num, csr.col_num)}
            )
        return result

    def get_ego_edge_index(self, hops: int):
        """get_ego_edge_index, get ego-subgraph wrt. hops num
        with the iteration of many GNNs, the receptive-field will decrease. For example, the first iteration of
        a 3-layer GNN usually should be conducted on 3-hop neighborhood wrt. target nodes. The second iteration
        only need 2-hop neighborhood of target nodes.

        get_ego_edge_index will return a set of edge index wrt. hops.

        Args:
            hops: how many hops we need.

        Returns: List of edge indexes. [{edge_name: edge_index_k_hop}, {edge_name: edge_index_{k-1}_hop} ...]

        """
        res = self.sg.GetEgoEdgeIndex(hops)
        res_size = len(res)
        res_final = []
        for i in range(res_size):
            one_hop_dict = {}
            for e_name, e_coo in res[i].items():
                n1_indices = np.squeeze(np.array(e_coo.GetN1Indices()), axis=-1)
                n2_indices = np.squeeze(np.array(e_coo.GetN2Indices()), axis=-1)
                e_indices = np.squeeze(np.array(e_coo.GetEdgeIndex()), axis=-1)
                one_hop_dict.update({e_name: (n1_indices, n2_indices, e_indices)})
            res_final.insert(0, one_hop_dict)
        return res_final

    def get_node_dense_feature(self, node_name, f_name):
        """

        Args:
            node_name(str): node (type) name
            f_name(str): dense feature name

        Returns: 2-dim np.array(), node_num * dense_feature_dim

        """
        res = self.sg.GetNodeDenseFeatureArray(node_name, f_name)
        n_d_array = res.GetFeatureArray()
        py_n_d_array = np.array(n_d_array)
        return py_n_d_array

    def get_node_sparse_kv_feature(self, node_name, f_name):
        """

        Args:
            node_name(str): node (type) name
            f_name(str): spares kv feature name

        Returns: (indices_offset, keys, values)

        """
        res = self.sg.GetNodeSparseKVArray(node_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        values = np.squeeze(np.array(f_array.GetVals()), axis=-1)
        return ind_offset, keys, values

    def get_node_sparse_k_feature(self, node_name, f_name):
        """

        Args:
            node_name(str): node (type) name
            f_name(str): spares key feature name

        Returns: (indices_offset, keys)

        """
        # todo zdl add check and raise error if not exists
        res = self.sg.GetNodeSparseKArray(node_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        return ind_offset, keys

    def get_edge_dense_feature(self, edge_name, f_name):
        """

        Args:
            edge_name(str): edge (type) name
            f_name(str): edge dense feature name

        Returns: 2-d np array. edge_num * dense_feature_dim

        """
        res = self.sg.GetEdgeDenseFeatureArray(edge_name, f_name)
        n_d_array = res.GetFeatureArray()
        py_n_d_array = np.array(n_d_array)
        return py_n_d_array

    def get_edge_sparse_kv_feature(self, edge_name, f_name):
        """

        Args:
            edge_name(str): edge (type) name
            f_name(str): edge sparse kv feature name

        Returns: (indices_offset, keys, values)

        """
        res = self.sg.GetEdgeSparseKVArray(edge_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        values = np.squeeze(np.array(f_array.GetVals()), axis=-1)
        return ind_offset, keys, values

    def get_edge_sparse_k_feature(self, edge_name, f_name):
        """

        Args:
            edge_name(str): edge (type) name
            f_name(str): edge sparse key feature name

        Returns: (indices_offset, keys)

        """
        res = self.sg.GetEdgeSparseKArray(edge_name, f_name)
        f_array = res.GetFeatureArray()
        ind_offset = np.squeeze(np.array(f_array.GetIndOffset()), axis=-1)
        keys = np.squeeze(np.array(f_array.GetKeys()), axis=-1)
        return ind_offset, keys

    def get_node_num_per_sample(self):
        """node num per sample

        Returns: {n_name1: [XXX], n_name2: [XXX]}, node_num per sample (pb);

        """
        return self.sg.GetNodeNumPerSample()

    def get_edge_num_per_sample(self):
        """edge num per sample

        Returns: {e_name1: [XXX], e_name2: [XXX]}, node_num per sample (pb);

        """
        return self.sg.GetEdgeNumPerSample()

    def get_root_index(self):
        """

        Returns: root ids of each sample. {name: [[]]}. name -> sample_index -> root_ids

        """
        return self.sg.GetRootIds()
