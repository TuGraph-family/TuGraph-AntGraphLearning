import collections
from typing import List, Union, NamedTuple, Tuple, Dict, Optional, Any

import numpy as np
import torch
from agl.python.subgraph.numpy_inputs import Features, Adj
from torch import Tensor
from abc import ABC, abstractmethod

__all__ = ["TorchBatchOfEgoGraph", "TorchBatchOfSubGraph", "TorchBatchPaths", ""]


def optional_func(x: Optional[np.ndarray], func):
    return func(x) if x is not None else None


def optional_to(x: Optional, *args, **kwargs):
    return x.to(*args, **kwargs) if x is not None else None


class TorchEdgeIndex(NamedTuple):
    private_edge_index: Tensor  # 目前只会用到这个字段
    size: Optional[Tuple[int, int]]
    edge_indices: Optional[Tensor]

    # 如果是 coo 或者 csr 创建的
    row: Optional[Tensor]
    col: Optional[Tensor]
    row_ptr:  Optional[Tensor]

    @property
    def edge_index(self):
        if self.private_edge_index is None:
            # todo NamedTuple 属性都是 readonly 的，因此构造完不能更新对应的属性
            if self.row is not None:
                assert self.col is not None
                # coo 格式
                return torch.stack([torch.as_tensor(self.row), torch.as_tensor(self.col)], dim=0)
            elif self.row_ptr is not None:
                assert self.col is not None
                # CSR 格式
                from torch_sparse import SparseTensor
                # is sorted should be Ture, otherwise, edge_f_index [0~nnz-1] would be changed
                sp = SparseTensor(rowptr=self.row_ptr, col=self.col,
                                  value=self.edge_indices, sparse_sizes=(self.size[0], self.size[1]), is_sorted=True)
                coo = sp.coo()
                dst_indices, src_indices, edge_indices = coo[0], coo[1], coo[2]
                # 转换index 方向
                return torch.stack([src_indices, dst_indices], dim=0)
        else:
            return self.private_edge_index

    def to(self, *args, **kwargs):
        if self.private_edge_index is None:
            if self.row is not None:
                assert self.col is not None
                # coo 格式
                row = optional_to(self.row, *args, **kwargs)
                col = optional_to(self.col, *args, **kwargs)
                edge_index = torch.stack([torch.as_tensor(row), torch.as_tensor(col)], dim=0)
                return TorchEdgeIndex(private_edge_index=edge_index.to(*args, **kwargs),
                                      size=self.size,
                                      edge_indices=optional_to(self.edge_indices, *args, **kwargs),
                                      row=row,
                                      col=col,
                                      row_ptr=optional_to(self.row_ptr, *args, **kwargs))
            elif self.row_ptr is not None:
                assert self.col is not None
                # CSR 格式
                p_row_ptr = optional_to(self.row_ptr, *args, **kwargs)
                p_col = optional_to(self.col, *args, **kwargs)
                p_edge_indices = optional_to(self.edge_indices, *args, **kwargs)
                from torch_sparse import SparseTensor
                # is sorted should be Ture, otherwise, edge_f_index [0~nnz-1] would be changed
                sp = SparseTensor(rowptr=p_row_ptr, col=p_col,
                                  value=p_edge_indices, sparse_sizes=(self.size[0], self.size[1]), is_sorted=True)
                coo = sp.coo()
                dst_indices, src_indices, edge_indices = coo[0], coo[1], coo[2]
                # 转换index 方向
                edge_index = torch.stack([src_indices, dst_indices], dim=0)
                return TorchEdgeIndex(private_edge_index=edge_index.to(*args, **kwargs),
                                      size=self.size,
                                      edge_indices=optional_to(edge_indices, *args, **kwargs),
                                      row=optional_to(self.row, *args, **kwargs),
                                      col=p_col,
                                      row_ptr=p_row_ptr)
            else:
                raise NotImplementedError
        else:
            return TorchEdgeIndex(private_edge_index=self.edge_index.to(*args, **kwargs),
                                  size=self.size,
                                  edge_indices=optional_to(self.edge_indices, *args, **kwargs),
                                  row=optional_to(self.row, *args, **kwargs),
                                  col=optional_to(self.col, *args, **kwargs),
                                  row_ptr=optional_to(self.row_ptr, *args, **kwargs))

    @staticmethod
    def create_from_coo_tensor(src: Tensor, dst: Tensor, size: Optional[Tuple[int, int]],
                               edge_indices: Optional[Tensor]):
        return TorchEdgeIndex(private_edge_index=None, size=size, edge_indices=edge_indices, row=src, col=dst, row_ptr=None)

    @staticmethod
    def create_from_csr_tensor(row_ptr: Tensor,  col: Tensor, size: Optional[Tuple[int, int]],
                               edge_indices: Optional[Tensor]):
        return TorchEdgeIndex(private_edge_index=None, size=size, edge_indices=edge_indices, row=None, col=col, row_ptr=row_ptr)


    @staticmethod
    def create_from_tensor(adj: Tensor, size: Optional[Tuple[int, int]], edge_indices: Optional[Tensor]):
        return TorchEdgeIndex(private_edge_index=adj, size=size, edge_indices=edge_indices, row=None, col=None, row_ptr=None)

    @staticmethod
    def create(adj: Optional[Adj]):
        if adj is None:
            return None
        return TorchEdgeIndex(private_edge_index=torch.as_tensor(adj.edges.astype('int64')),
                              size=(adj.shape[0], adj.shape[1]),
                              edge_indices=None)


class TorchFeature(NamedTuple):

    @abstractmethod
    def to(self, *args, **kwargs):
        pass

    @abstractmethod
    def to_dense(self):
        raise NotImplementedError(f"{self.__class__}.TorchFeature")


class TorchDenseFeature(TorchFeature, NamedTuple):
    x: Tensor

    def to(self, *args, **kwargs):
        return TorchDenseFeature(x=self.x.to(*args, **kwargs))

    def to_dense(self):
        return self.x

    # 考虑是否将下面两个方法合并成一个
    @staticmethod
    def create_from_tensor(feat: Tensor):
        return TorchDenseFeature(x=feat)

    @staticmethod
    def create(feat:  np.ndarray):
        return TorchDenseFeature(x=torch.as_tensor(feat))


class TorchSparseFeature(TorchFeature, NamedTuple):
    # todo now only support coo and csr, 考虑直接使用 torch_sparse中的 sparse tensor
    row: Optional[Tensor]
    row_ptr: Optional[Tensor]
    col: Optional[Tensor]
    value: Optional[Tensor]
    size: Tuple[int, int]

    def to(self, *args, **kwargs):
        return TorchSparseFeature(
            row=optional_to(self.row, *args, **kwargs),
            row_ptr=optional_to(self.row_ptr, *args, **kwargs),
            col=optional_to(self.col, *args, **kwargs),
            value=optional_to(self.value, *args, **kwargs),
            size=self.size
        )

    # 必须在 collate function 之外使用
    # dataloader 多进程时 sparse tensor无法 pickle
    def to_dense(self):
        # todo check device 需要一致
        if self.row is not None:
            assert self.col is not None
            index = torch.stack([self.row, self.col], dim=0)
            return torch.sparse_coo_tensor(index, self.value, self.size).to_dense()
        elif self.row_ptr is not None:
            assert self.col is not None
            return torch.sparse_csr_tensor(
                crow_indices=self.row_ptr,
                col_indices=self.col,
                values=self.value,
                size=self.size
            ).to_dense()
        else:
            raise NotImplementedError(
                f"row = None? {self.row is None}, row_ptr = None ? {self.row_ptr is None}, not supported")

    # 考虑将下面几个方法合并成2个 from tensor/ndarray
    @staticmethod
    def create_from_coo_tensor(row: Tensor, col: Tensor, value: Tensor, size: Tuple[int, int]):
        return TorchSparseFeature(row=row, row_ptr=None, col=col, value=value, size=size)

    @staticmethod
    def create_from_coo_ndarray(row: np.ndarray, col: np.ndarray, value: np.ndarray, size: Tuple[int, int]):
        return TorchSparseFeature(row=torch.as_tensor(row), row_ptr=None, col=torch.as_tensor(col),
                                  value=torch.as_tensor(value), size=size)

    @staticmethod
    def create_from_csr_tensor(row_ptr: Tensor, col: Tensor, value: Tensor, size: Tuple[int, int]):
        return TorchSparseFeature(row=None, row_ptr=row_ptr, col=col, value=value, size=size)

    @staticmethod
    def create_from_csr_ndarray(row_ptr: np.ndarray, col: np.ndarray, value: np.ndarray, size: Tuple[int, int]):
        return TorchSparseFeature(row=None, row_ptr=torch.as_tensor(row_ptr), col=torch.as_tensor(col),
                                  value=torch.as_tensor(value), size=size)


class TorchFeatures(NamedTuple):
    subgraph_index: Optional[Tensor]  # 内部兼容
    features: Dict[str, TorchFeature]
    id_num: Optional[int]  # 内部兼容

    def to(self, *args, **kwargs):
        return TorchFeatures(subgraph_index= optional_to(self.subgraph_index, *args, **kwargs),
                             features={k: v.to(*args, **kwargs) for k,v in self.features.items()},
                             id_num=self.id_num)

    @staticmethod
    def create_from_torch_feature(features: Dict[str, TorchFeature]):
        return TorchFeatures(subgraph_index=None, features=features, id_num=None)

    @staticmethod
    def create(features: Optional[Features]):
        if features is None:
            return None
        return TorchFeatures(subgraph_index=torch.as_tensor(features.indices),
                             features={k: TorchFeature.create(v) for k, v in features.features.items()},
                             id_num=features.id_num)


class TorchEgoBatchData(NamedTuple):
    n_feats: Optional[TorchFeatures]
    e_feats: Optional[TorchFeatures]
    y: Optional[Tensor]
    other_feats: Optional[Dict[str, Tensor]]
    other_raw: Optional[Any]
    adjs_t: List[TorchEdgeIndex]
    root_index: Optional[Tensor]

    def to(self, *args, **kwargs):
        assert self.adjs_t is not None
        return TorchEgoBatchData(
            n_feats=optional_to(self.n_feats, *args, **kwargs),
            e_feats=optional_to(self.e_feats, *args, **kwargs),
            y=optional_to(self.y, *args, **kwargs),
            adjs_t=[adj_t.to(*args, **kwargs) for adj_t in self.adjs_t],
            root_index=optional_to(self.root_index, *args, **kwargs),
            other_feats={k: v.to(*args, **kwargs) for k, v in self.other_feats.items()},
            other_raw=self.other_raw
        )

    @staticmethod
    def create_from_tensor(n_feats: Optional[TorchFeatures],
                           e_feats: Optional[TorchFeatures],
                           y: Optional[Tensor],
                           adjs_t: List[TorchEdgeIndex],
                           root_index: Optional[Tensor],
                           other_feats: Optional[Dict[str, Tensor]],
                           other_raw: Optional[Any]
                           ):
        return TorchEgoBatchData(n_feats=n_feats,
                                 e_feats=e_feats,
                                 y=y,
                                 adjs_t=adjs_t,
                                 root_index=root_index,
                                 other_feats=other_feats,
                                 other_raw=other_raw)


class TorchSubGraphBatchData(NamedTuple):
    n_feats: Optional[TorchFeatures]
    e_feats: Optional[TorchFeatures]
    y: Optional[Tensor]
    other_feats: Optional[Dict[str, Tensor]]
    other_raw: Optional[Any]
    root_index: Optional[Tensor]
    adjs_t: TorchEdgeIndex
    n_num_per_sample: Optional[Tensor]
    e_num_per_sample: Optional[Tensor]

    def to(self, *args, **kwargs):
        assert self.adjs_t is not None
        return TorchSubGraphBatchData(
            n_feats=optional_to(self.n_feats, *args, **kwargs),
            e_feats=optional_to(self.e_feats, *args, **kwargs),
            y=optional_to(self.y, *args, **kwargs),
            adjs_t=self.adjs_t.to(*args, **kwargs),
            root_index=optional_to(self.root_index, *args, **kwargs),
            n_num_per_sample=optional_to(self.n_num_per_sample, *args, **kwargs),
            e_num_per_sample=optional_to(self.e_num_per_sample, *args, **kwargs),
            other_feats={k: v.to(*args, **kwargs) for k, v in self.other_feats.items()},
            other_raw=self.other_raw
        )

    @staticmethod
    def create_from_tensor(n_feats: Optional[TorchFeatures],
                           e_feats: Optional[TorchFeatures],
                           y: Optional[Tensor],
                           adjs_t: TorchEdgeIndex,
                           root_index: Optional[Tensor],
                           n_num_per_sample: Optional[Tensor],
                           e_num_per_sample: Optional[Tensor],
                           other_feats: Optional[Dict[str, Tensor]],
                           other_raw: Optional[Any]
                           ):
        return TorchSubGraphBatchData(n_feats=n_feats,
                                      e_feats=e_feats,
                                      y=y,
                                      adjs_t=adjs_t,
                                      root_index=root_index,
                                      n_num_per_sample=n_num_per_sample,
                                      e_num_per_sample=e_num_per_sample,
                                      other_feats=other_feats,
                                      other_raw=other_raw)
