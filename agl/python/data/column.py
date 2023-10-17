#    Copyright 2023 AntGroup CO., Ltd.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

from abc import ABC, abstractmethod
from typing import List

import torch
import numpy as np

from agl.python.data.agl_dtype import np_to_agl_dtype

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
    def __init__(
        self,
        name: str,
        dim: int,
        dtype: np.dtype,
        in_sep: str = ",",
        out_sep: str = " ",
        concat: bool = True,
        enable_c_decode: bool = True,
    ):
        """a data record may store some dense feature in one column.
        AGLMultiDenseColumn is used to decode data in such format

        Args:
            name(str):  column name
            dim(int): dimension of dense feature
            dtype(np.dtype): data type of the dense feature
            in_sep(str): separator within a dense feature
            out_sep(str): separator between different dense feature
            concat(bool): whether plain concat different dense features
            enable_c_decode(bool): Decode with c backend if True, otherwise using python decode
        """
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
        """enable_c_decode is True call c decode function, otherwise call python decode

        Args:
            data:   related column of data records
            **kwargs: for further usage, not used now

        Returns: np nd array, element_num * feature_dim

        """
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
                    # if it is instance of bytes (encoded by utf-8). call multi_dense_decode_bytes
                    # (implemented with c++) and pass those data to c++ in a zero copy way
                    from pyagl import multi_dense_decode_bytes

                    data_bytesarray = [bytearray(data_t) for data_t in data]
                    res = multi_dense_decode_bytes(
                        data_bytesarray,
                        self._out_sep,
                        self._in_sep,
                        self._dim,
                        np_to_agl_dtype[self._dtype],
                    )
                    # list -> batch_size * np.array (element_size * dim)
                    res_np_array_list = [np.array(res_i) for res_i in res]
                elif isinstance(data[0], str):
                    # if data is instance of str, passing it from Python to C++ using pybind11 will trigger a copy.
                    from pyagl import multi_dense_decode_string

                    res = multi_dense_decode_string(
                        data,
                        self._out_sep,
                        self._in_sep,
                        self._dim,
                        np_to_agl_dtype[self._dtype],
                    )
                    res_np_array_list = [np.array(res_i) for res_i in res]
                else:
                    raise NotImplementedError("only support str or bytes")

                if self._concat:
                    final_result = np.concatenate(res_np_array_list, axis=0)
                    final_result = final_result.reshape(-1, self._dim)
                    return torch.as_tensor(
                        final_result
                    )  # as_tensor: if data is numpy array, would call from_numpy. won't copy
                else:
                    raise NotImplementedError("only support flat concat now!")

        else:
            raise NotImplementedError("now only support list")

    def _py_decode(self, data, **kwargs):
        # This method is just for benchmark
        if isinstance(data, List):
            if len(data) > 0:
                result = []
                # The logic for processing a batch is as follows:
                for data_t in data:
                    # The logic for processing each sample is as follows:
                    if isinstance(data_t, bytes):
                        # Each sample may contain multiple dense features, and the number of features is not fixed.
                        data_splited = data_t.split(self._out_sep.encode("utf-8"))
                        one_result = []
                        for data_s_t in data_splited:
                            data_list = list(
                                map(
                                    self._dtype,
                                    data_s_t.split(self._in_sep.encode("utf-8")),
                                )
                            )
                            assert len(data_list) == self._dim
                            one_result.append(data_list)
                        result.append(one_result)
                    elif isinstance(data_t, str):
                        data_splited = data_t.split(self._out_sep)
                        one_result = []
                        for data_s_t in data_splited:
                            data_list = list(
                                map(self._dtype, data_s_t.split(self._in_sep))
                            )
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
                    return torch.as_tensor(
                        final_result
                    )  # as_tensor: if data is numpy array, would call from_numpy. won't copy
        else:
            raise NotImplementedError("now only support list")


class AGLDenseColumn(AGLColumn):
    def __init__(self, name: str, dim: int, dtype: np.dtype, sep: str = " "):
        """

        Args:
            name(str): feature name
            dim(int):  feature dimension
            dtype(np.dtype): feature dtype
            sep(str): separator used split feature
        """
        assert name not in built_in_name
        self._name = name
        self._dim = dim
        self._sep = sep
        self._dtype = dtype

    @property
    def name(self):
        return self._name

    def decode(self, data, **kwargs):
        """

        Args:
            data:   related column of data records
            **kwargs: for further usage, not used now

        Returns: np nd array, element_num * feature_dim

        """
        if isinstance(data, List):
            if len(data) > 0:
                result = []
                for data_t in data:
                    if isinstance(data_t, bytes):
                        data_list = list(
                            map(self._dtype, data_t.split(self._sep.encode("utf-8")))
                        )
                    elif isinstance(data_t, str):
                        data_list = list(map(self._dtype, data_t.split(self._sep)))
                    else:
                        raise NotImplementedError("only support str or bytes")
                    assert len(data_list) == self._dim
                    result.append(data_list)
                result = np.asarray(result)  # won't copy
                return torch.as_tensor(
                    result
                )  # as_tensor: if data is numpy array, would call from_numpy. won't copy
        else:
            raise NotImplementedError("now only support list")


class AGLRowColumn(AGLColumn):
    def __init__(self, name):
        """

        Args:
            name:  feature name
        """
        self._name = name

    @property
    def name(self):
        return self._name

    def decode(self, data, **kwargs):
        """

        Args:
            data:   related column of data records
            **kwargs: for further usage, not used now

        Returns: return data input directly

        """
        return data
