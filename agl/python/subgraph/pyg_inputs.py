import collections
from typing import List, Union, NamedTuple, Tuple, Dict, Optional

import numpy as np
import torch
from agl.python.subgraph.numpy_inputs import Features, Adj
from torch import Tensor

__all__ = ["TorchBatchOfEgoGraph", "TorchBatchOfSubGraph", "TorchBatchPaths", ""]


def optional_func(x: Optional[np.ndarray], func):
    return func(x) if x else None


def optional_to(x: Optional, *args, **kwargs):
    return x.to(*args, **kwargs) if x else None


class TorchEdgeIndex(NamedTuple):
    edge_index: Tensor  # [dst, src]
    size: Tuple[int, int]

    def to(self, *args, **kwargs):
        edge_index = self.edge_index.to(*args, **kwargs)
        return TorchEdgeIndex(edge_index, self.size)

    @staticmethod
    def create(adj: Optional[Adj]):
        if adj is None:
            return None
        return TorchEdgeIndex(edge_index=torch.as_tensor(adj.edges.astype('int64')),
                              size=(adj.shape[0], adj.shape[1]))


class TorchFeature(NamedTuple):
    """
        raw/dense feature: Tensor
        sparse feature: Tuple[Tensor]
    """
    x: Union[Tensor, Tuple[Tensor, ...]]

    def to(self, *args, **kwargs):
        return TorchFeature(x=self.x.to(*args, **kwargs))

    @staticmethod
    def create(feat: Union[Tuple[np.ndarray, np.ndarray, np.ndarray], np.ndarray]):
        if isinstance(feat, collections.Sequence):
            "COO SpMatrix: index, values, dense_shape"
            assert len(feat) == 3
            return TorchFeature(x=torch.sparse_coo_tensor(feat[0], feat[1], feat[2]))
        else:
            return TorchFeature(x=torch.as_tensor(feat))

    def is_sparse(self):
        return len(self.x) == 3


class TorchFeatures(NamedTuple):
    # if subgraph_index is None:
    #   feature's id-order is same as adj id-order
    # else:
    #   feature's id-order use subgraph_index to map to adj's id-order
    subgraph_index: Tensor
    features: Dict[str, TorchFeature]
    id_num: int

    def to(self, *args, **kwargs):
        return TorchFeatures(
            subgraph_index=self.subgraph_index.to(*args, **kwargs),
            features={k: v.to(*args, **kwargs) for k, v in self.features.items()},
            id_num=self.id_num)

    @staticmethod
    def create(features: Optional[Features]):
        if features is None:
            return None
        return TorchFeatures(subgraph_index=torch.as_tensor(features.indices),
                             features={k: TorchFeature.create(v) for k, v in features.features.items()},
                             id_num=features.id_num)


class TorchBatchOfEgoGraph(NamedTuple):
    n_feats: Optional[TorchFeatures]
    e_feats: Optional[TorchFeatures]
    y: Optional[Tensor]
    adjs_t: List[TorchEdgeIndex]

    def to(self, *args, **kwargs):
        assert self.adjs_t is not None
        return TorchBatchOfEgoGraph(
            n_feats=optional_to(self.n_feats, *args, **kwargs),
            e_feats=optional_to(self.e_feats, *args, **kwargs),
            y=optional_to(self.y, *args, **kwargs),
            adjs_t=[adj_t.to(*args, **kwargs) for adj_t in self.adjs_t])

    @staticmethod
    def create(n_feats: Optional[Features], e_feats: Optional[Features], y: Optional[np.ndarray],
               adjs_t: List[Adj]):
        assert adjs_t is not None
        return TorchBatchOfEgoGraph(n_feats=TorchFeatures.create(n_feats),
                                    e_feats=TorchFeatures.create(e_feats),
                                    y=optional_func(y, torch.as_tensor),
                                    adjs_t=[TorchEdgeIndex.create(adj) for adj in adjs_t])


class TorchBatchOfSubGraph(NamedTuple):
    n_feats: Optional[TorchFeatures]
    e_feats: Optional[TorchFeatures]
    y: Optional[Tensor]
    adj: TorchEdgeIndex

    def to(self, *args, **kwargs):
        assert self.adj is not None
        return TorchBatchOfSubGraph(
            n_feats=optional_to(self.n_feats, *args, **kwargs),
            e_feats=optional_to(self.e_feats, *args, **kwargs),
            y=optional_to(self.y, *args, **kwargs),
            adj=self.adj.to(*args, **kwargs))

    @staticmethod
    def create(n_feats: Optional[Features], e_feats: Optional[Features], y: Optional[np.ndarray],
               adj: Adj):
        assert adj is not None
        return TorchBatchOfSubGraph(n_feats=TorchFeatures.create(n_feats),
                                    e_feats=TorchFeatures.create(e_feats),
                                    y=optional_func(y, torch.as_tensor),
                                    adj=TorchEdgeIndex.create(adj))


class TorchBatchPaths(NamedTuple):
    root_node_indices: Optional[Tensor]
    node_indices_path: Optional[Tensor]
    other_path_info: Dict[str, Tensor]
    node_features: Optional[TorchFeatures]
    other_tensor: Dict[str, Tensor]

    def to(self, *args, **kwargs):
        assert self.other_tensor is not None
        assert self.other_path_info is not None
        return TorchBatchPaths(
            root_node_indices=optional_to(self.root_node_indices, *args, **kwargs),
            node_indices_path=optional_to(self.node_indices_path, *args, **kwargs),
            other_path_info={k: v.to(*args, **kwargs) for k, v in self.other_path_info.items()},
            node_features=optional_to(self.node_features, *args, **kwargs),
            other_tensor={k: v.to(*args, **kwargs) for k, v in self.other_tensor.items()})

    @staticmethod
    def create(root_node_indices: Optional[np.ndarray],
               node_indices_path: Optional[np.ndarray],
               other_path_info: Dict[str, np.ndarray],
               node_features: Optional[Features],
               other_tensor: Dict[str, np.ndarray]):
        assert other_path_info is not None
        assert other_tensor is not None
        return TorchBatchPaths(root_node_indices=optional_func(root_node_indices, torch.as_tensor),
                               node_indices_path=optional_func(node_indices_path, torch.as_tensor),
                               other_path_info={k: optional_func(v, torch.as_tensor) for k, v in
                                                other_path_info.items()},
                               node_features=TorchFeatures.create(node_features),
                               other_tensor={k: optional_func(v, torch.as_tensor) for k, v in other_tensor.items()})
