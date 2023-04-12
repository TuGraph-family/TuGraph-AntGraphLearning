from dataclasses import dataclass

import numpy as np


@dataclass
class Adj:
    edges: np.ndarray
    shape: np.ndarray

    def __init__(self, edges, dst_n, src_n):
        self.edges = edges
        self.shape = np.array([dst_n, src_n], np.int64)

    @staticmethod
    def tf_set_shape(edges, shape):
        edges.set_shape([None, 2])
        shape.set_shape([2])
        return edges, shape


@dataclass
class Features:
    id_num: int  # num of unique id
    features: dict  # unique_id's feature
    _indices: np.ndarray = None  # to get subgraph-node's feature, by K.gather(features, indices)

    def __init__(self, id_num, features, indices=None):
        self.id_num = id_num
        self.features = features
        self._indices = indices

    @staticmethod
    def tf_set_shape(id_num, indices):
        id_num.set_shape([])
        indices.set_shape([None])
        return id_num, indices

    @property
    def indices(self):
        if self._indices is None:
            self._indices = np.arange(self.id_num)
        return self._indices
