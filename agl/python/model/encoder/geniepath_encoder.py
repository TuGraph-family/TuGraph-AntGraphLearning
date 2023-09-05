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

from typing import Dict, Union
from abc import ABC, abstractmethod

import torch

from agl.python.data.subgraph.pyg_inputs import (
    TorchEgoBatchData,
    TorchSubGraphBatchData,
)
from agl.python.model.layer.geniepath_layer import Breadth, Depth
from agl.python.model.encoder.base import AGLAlgorithm
from agl.python.model.layer.linear import AGLLinear


class GeniePathLazyEncoder(AGLAlgorithm):
    def __init__(self, in_dim: int, hidden_dim: int, hops: int, residual: bool = True):
        """

        Args:
            in_dim(int): input dimension
            hidden_dim(int): dimension of hidden embedding
            hops(int): number of gnn layers
            residual(bool): whether we use residual or not
        """
        super().__init__()
        self._in_dim = in_dim
        self._hidden_dim = hidden_dim
        self._hops = hops
        self._residual = residual

        self._lin = lambda x: x
        if self._in_dim != hidden_dim:
            self._lin = AGLLinear(in_dim, hidden_dim)

        self._breadths = torch.nn.ModuleList(
            [Breadth(hidden_dim, hidden_dim) for _ in range(hops)]
        )

        self._depths = torch.nn.ModuleList(
            [
                Depth(hidden_dim * 2, hidden_dim)
                if self._residual
                else Depth(hidden_dim, hidden_dim)
                for _ in range(hops)
            ]
        )

    def encode(
        self,
        subgraph: Union[TorchEgoBatchData, TorchSubGraphBatchData],
        x: torch.Tensor,
        **kwargs
    ):
        """

        Args:
            subgraph: Union[TorchEgoBatchData, TorchSubGraphBatchData]
            x: initialized node feat
            **kwargs: other inputs

        Returns:
            embeddings or other information you need

        """
        x = self._lin(x)
        h = torch.zeros(1, x.shape[0], self._hidden_dim, device=x.device)
        c = torch.zeros(1, x.shape[0], self._hidden_dim, device=x.device)

        h_tmps = []
        tmp = x
        if isinstance(subgraph, TorchEgoBatchData):
            for conv, adj in zip(self._breadths, subgraph.adjs_t):
                tmp = conv(tmp, adj.edge_index, None)
                h_tmps.append(tmp)
        else:
            for i, conv in enumerate(self._breadths):
                tmp = conv(tmp, subgraph.adjs_t.edge_index, None)
                h_tmps.append(tmp)

        x = x[None, :]
        if self._residual:
            for conv, h_i in zip(self._depths, h_tmps):
                in_cat = torch.cat((h_i[None, :], x), -1)
                x, (h, c) = conv(in_cat, h, c)
        else:
            for conv, h_i in zip(self._depths, h_tmps):
                x, (h, c) = conv(h_i[None, :], h, c)
        return x[0]

    def forward(
        self,
        subgraph: Union[TorchEgoBatchData, TorchSubGraphBatchData],
        x: torch.Tensor,
    ):
        res = self.encode(subgraph, x)
        return res
