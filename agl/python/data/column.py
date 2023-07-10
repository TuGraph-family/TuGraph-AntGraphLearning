#!/usr/bin/python
# coding: utf-8
from abc import ABC, abstractmethod
from typing import List

import numpy as np
import torch

import torch
from torch_geometric.data import Data

from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray
from agl.python.subgraph.subgraph import PySubGraph
from agl.python.data.agl_dtype import np_to_agl_dtype, agl_dtype_to_np

built_in_name = ["x", "edge_index", "agl", "edge_attr"]


class AGLColumn(ABC):
    def __call__(self, data, **kwargs):
        return self.decode(data, **kwargs)

    @abstractmethod
    def decode(self, data, **kwargs):
        raise NotImplementedError

    @property
    def name(self):
        raise NotImplementedError


class AGLMultiDenseColumn(AGLColumn):
    def __init__(self, name: str, dim: int, dtype: np.dtype, in_sep: str = ",", out_sep: str = " ", concat: bool = True,
                 enable_c_decode: bool = True):
        assert name not in built_in_name
        self._name = name
        self._dim = dim
        self._in_sep = in_sep
        self._out_sep = out_sep
        self._dtype = dtype
        self._concat = concat
        self._enable_c_decode = enable_c_decode

    @property
    def name(self):
        return self._name

    def decode(self, data, **kwargs):
        if self._enable_c_decode:
            res = self._c_decode(data, **kwargs)
            return res
        else:
            res = self._py_decode(data, **kwargs)
            return res

    def _c_decode(self, data, **kwargs):
        if isinstance(data, List):
            if len(data) > 0:
                if isinstance(data[0], bytes):
                    # 如果是 bytes (encoded by utf-8 ), 调用 multi_dense_decode_bytes 方法，zero copy 的传入到 c++
                    from pyagl.pyagl import multi_dense_decode_bytes
                    data_bytesarray = [bytearray(data_t) for data_t in data]
                    res = multi_dense_decode_bytes(data_bytesarray, self._out_sep, self._in_sep, self._dim,
                                                   np_to_agl_dtype[self._dtype])
                    # list -> batch_size * np.array (element_size * dim)
                    res_np_array_list = [np.array(res_i) for res_i in res]
                elif isinstance(data[0], str):
                    # 如果是 str, 使用 pybind11 copy 的方式传入到c++. (for 循环 encode 相当于在 python 层copy 效率较低)
                    from pyagl.pyagl import multi_dense_decode_string
                    res = multi_dense_decode_string(data, self._out_sep, self._in_sep, self._dim,
                                                    np_to_agl_dtype[self._dtype])
                    res_np_array_list = [np.array(res_i) for res_i in res]
                else:
                    raise NotImplementedError("only support str or bytes")

                if self._concat:
                    final_result = np.concatenate(res_np_array_list, axis=0)
                    final_result = final_result.reshape(-1, self._dim)
                    return torch.as_tensor(final_result)  # as_tensor 如果是numpy 会 call from_numpy. won't copy
                else:
                    raise NotImplementedError("only support flat concat now!")

        else:
            raise NotImplementedError("now only support list")

    def _py_decode(self, data, **kwargs):
        # for benckmark, 效率较低，后面考虑删除
        if isinstance(data, List):
            if len(data) > 0:
                result = []
                # 处理一个batch的逻辑
                for data_t in data:
                    # 处理每条样本的逻辑
                    if isinstance(data_t, bytes):
                        # 每条样本里面可能有多个dense, 而且数量不固定
                        data_splited = data_t.split(self._out_sep.encode('utf-8'))
                        one_result = []
                        for data_s_t in data_splited:
                            data_list = list(map(self._dtype, data_s_t.split(self._in_sep.encode('utf-8'))))
                            assert len(data_list) == self._dim
                            one_result.append(data_list)
                        result.append(one_result)
                    elif isinstance(data_t, str):
                        data_splited = data_t.split(self._out_sep)
                        one_result = []
                        for data_s_t in data_splited:
                            data_list = list(map(self._dtype, data_s_t.split(self._in_sep)))
                            assert len(data_list) == self._dim
                            one_result.append(data_list)
                        result.append(one_result)
                    else:
                        raise NotImplementedError("only support str or bytes")
                if self._concat:
                    import itertools
                    flattened_lst = list(itertools.chain.from_iterable(result))
                    final_result = np.asarray(flattened_lst)  # won't copy
                    final_result = final_result.reshape(-1, self._dim)
                    return torch.as_tensor(final_result)  # as_tensor 如果是numpy 会 call from_numpy. won't copy
        else:
            raise NotImplementedError("now only support list")


class AGLDenseColumn(AGLColumn):
    def __init__(self, name: str, dim: int, dtype: np.dtype, sep: str = " "):
        assert name not in built_in_name
        self._name = name
        self._dim = dim
        self._sep = sep
        self._dtype = dtype

    @property
    def name(self):
        return self._name

    def decode(self, data, **kwargs):
        if isinstance(data, List):
            if len(data) > 0:
                result = []
                for data_t in data:
                    if isinstance(data_t, bytes):
                        data_list = list(map(self._dtype, data_t.split(self._sep.encode('utf-8'))))
                    elif isinstance(data_t, str):
                        data_list = list(map(self._dtype, data_t.split(self._sep)))
                    else:
                        raise NotImplementedError("only support str or bytes")
                    assert len(data_list) == self._dim
                    result.append(data_list)
                result = np.asarray(result)  # won't copy
                return torch.as_tensor(result)  # as_tensor 如果是numpy 会 call from_numpy. won't copy
        else:
            raise NotImplementedError("now only support list")


class AGLRowColumn(AGLColumn):
    def __init__(self, name):
        self._name = name

    @property
    def name(self):
        return self._name

    def decode(self, data, **kwargs):
        return data


# todo AGLHomoGraphFeatureColumn is not ready for use
class AGLHomoGraphFeatureColumn(AGLColumn):
    def __init__(self, name: str, node_spec: NodeSpec, edge_spec: EdgeSpec,
                 x_name: str = None,
                 edge_attr_name: str = None,
                 need_node_and_edge_num: bool = False,
                 ego_edge_index: bool = False
                 ):
        assert name not in built_in_name
        if ego_edge_index and need_node_and_edge_num:
            raise NotImplementedError(
                "now the options ego_edge_index and need_node_and_edge_num are mutually exclusive!")
        self._name = name
        self._node_spec = node_spec
        self._edge_spec = edge_spec
        self._x_name = x_name
        self._edge_attr_name = edge_attr_name
        self._need_node_and_edge_num = need_node_and_edge_num
        self._ego_edge_index = ego_edge_index

    def decode(self, data, **kwargs):
        pass

    @property
    def name(self):
        return self._name
