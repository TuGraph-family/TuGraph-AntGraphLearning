from typing import List, NamedTuple, Tuple, Dict, Optional, Any, Union
from abc import abstractmethod

import torch
import numpy as np
from torch import Tensor


def optional_func(x: Optional[np.ndarray], func):
    return func(x) if x is not None else None


def optional_to(x: Optional, *args, **kwargs):
    return x.to(*args, **kwargs) if x is not None else None


class TorchEdgeIndex(NamedTuple):
    private_edge_index: Optional[Tensor]
    size: Optional[Tuple[int, int]]
    edge_indices: Optional[Tensor]

    # if create from coo or csr，following property exists
    row: Optional[Tensor]
    col: Optional[Tensor]
    row_ptr: Optional[Tensor]

    @property
    def edge_index(self):
        if self.private_edge_index is None:
            # todo property of NamedTuple readonly, update related property is not allowed
            if self.row is not None:
                assert self.col is not None
                # coo format. direction has exchange before. refer collate::get_ego_edge_index
                return torch.stack(
                    [torch.as_tensor(self.row), torch.as_tensor(self.col)], dim=0
                )
            elif self.row_ptr is not None:
                assert self.col is not None
                # CSR format
                from torch_sparse import SparseTensor

                # is sorted should be Ture, otherwise, edge_f_index [0~nnz-1] would be changed
                sp = SparseTensor(
                    rowptr=self.row_ptr,
                    col=self.col,
                    value=self.edge_indices,
                    sparse_sizes=(self.size[0], self.size[1]),
                    is_sorted=True,
                )
                coo = sp.coo()
                dst_indices, src_indices, edge_indices = coo[0], coo[1], coo[2]
                # should adapt pyg for direction of edge index when format is coo
                return torch.stack([src_indices, dst_indices], dim=0)
            else:
                raise NotImplementedError
        else:
            return self.private_edge_index

    def to(self, *args, **kwargs):
        if self.private_edge_index is None:
            if self.row is not None:
                assert self.col is not None
                # coo format
                row = optional_to(self.row, *args, **kwargs)
                col = optional_to(self.col, *args, **kwargs)
                edge_index = torch.stack(
                    [torch.as_tensor(row), torch.as_tensor(col)], dim=0
                )
                return TorchEdgeIndex(
                    private_edge_index=edge_index.to(*args, **kwargs),
                    size=self.size,
                    edge_indices=optional_to(self.edge_indices, *args, **kwargs),
                    row=row,
                    col=col,
                    row_ptr=optional_to(self.row_ptr, *args, **kwargs),
                )
            elif self.row_ptr is not None:
                assert self.col is not None
                # CSR format
                p_row_ptr = optional_to(self.row_ptr, *args, **kwargs)
                p_col = optional_to(self.col, *args, **kwargs)
                p_edge_indices = optional_to(self.edge_indices, *args, **kwargs)
                from torch_sparse import SparseTensor

                # is sorted should be Ture, otherwise, edge_f_index [0~nnz-1] would be changed
                sp = SparseTensor(
                    rowptr=p_row_ptr,
                    col=p_col,
                    value=p_edge_indices,
                    sparse_sizes=(self.size[0], self.size[1]),
                    is_sorted=True,
                )
                coo = sp.coo()
                dst_indices, src_indices, edge_indices = coo[0], coo[1], coo[2]
                # should adapt pyg for direction of edge index when format is coo
                edge_index = torch.stack([src_indices, dst_indices], dim=0)
                return TorchEdgeIndex(
                    private_edge_index=edge_index.to(*args, **kwargs),
                    size=self.size,
                    edge_indices=optional_to(edge_indices, *args, **kwargs),
                    row=optional_to(self.row, *args, **kwargs),
                    col=p_col,
                    row_ptr=p_row_ptr,
                )
            else:
                raise NotImplementedError
        else:
            return TorchEdgeIndex(
                private_edge_index=self.edge_index.to(*args, **kwargs),
                size=self.size,
                edge_indices=optional_to(self.edge_indices, *args, **kwargs),
                row=optional_to(self.row, *args, **kwargs),
                col=optional_to(self.col, *args, **kwargs),
                row_ptr=optional_to(self.row_ptr, *args, **kwargs),
            )

    @staticmethod
    def create_from_coo_tensor(
        src: Tensor,
        dst: Tensor,
        size: Optional[Tuple[int, int]],
        edge_indices: Optional[Tensor],
    ):
        return TorchEdgeIndex(
            private_edge_index=None,
            size=size,
            edge_indices=edge_indices,
            row=src,
            col=dst,
            row_ptr=None,
        )

    @staticmethod
    def create_from_csr_tensor(
        row_ptr: Tensor,
        col: Tensor,
        size: Optional[Tuple[int, int]],
        edge_indices: Optional[Tensor],
    ):
        return TorchEdgeIndex(
            private_edge_index=None,
            size=size,
            edge_indices=edge_indices,
            row=None,
            col=col,
            row_ptr=row_ptr,
        )

    @staticmethod
    def create_from_tensor(
        adj: Tensor, size: Optional[Tuple[int, int]], edge_indices: Optional[Tensor]
    ):
        return TorchEdgeIndex(
            private_edge_index=adj,
            size=size,
            edge_indices=edge_indices,
            row=None,
            col=None,
            row_ptr=None,
        )


class TorchFeature(NamedTuple):
    @abstractmethod
    def to(self, *args, **kwargs):
        pass

    @abstractmethod
    def to_dense(self):
        raise NotImplementedError(f"{self.__class__}.TorchFeature")

    @abstractmethod
    def get(self):
        pass


class TorchDenseFeature(TorchFeature, NamedTuple):
    x: Tensor

    def to(self, *args, **kwargs):
        return TorchDenseFeature(x=self.x.to(*args, **kwargs))

    def get(self):
        return self.x

    def to_dense(self):
        return self.x

    @staticmethod
    def create(feat: Union[Tensor, np.ndarray]):
        return TorchDenseFeature(x=torch.as_tensor(feat))


class TorchSparseFeature(TorchFeature, NamedTuple):
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
            size=self.size,
        )

    # todo should get sparse out of dataloader,
    #  torch sparse tensor now can not be pickled when using multi-process in dataloader
    def get(self):
        if self.row is not None:
            assert self.col is not None
            index = torch.stack([self.row, self.col], dim=0)
            return torch.sparse_coo_tensor(index, self.value, self.size)
        elif self.row_ptr is not None:
            assert self.col is not None
            return torch.sparse_csr_tensor(
                crow_indices=self.row_ptr,
                col_indices=self.col,
                values=self.value,
                size=self.size,
            )
        else:
            raise NotImplementedError(
                f"row = None? {self.row is None}, row_ptr = None ? {self.row_ptr is None}, not supported"
            )

    def to_dense(self):
        return self.get().to_dense()

    @staticmethod
    def create_from_coo(
        row: Union[Tensor, np.ndarray],
        col: Union[Tensor, np.ndarray],
        value: Union[Tensor, np.ndarray],
        size: Tuple[int, int],
    ):
        return TorchSparseFeature(
            row=torch.as_tensor(row),
            row_ptr=None,
            col=torch.as_tensor(col),
            value=torch.as_tensor(value),
            size=size,
        )

    @staticmethod
    def create_from_csr(
        row_ptr: Union[Tensor, np.ndarray],
        col: Union[Tensor, np.ndarray],
        value: Union[Tensor, np.ndarray],
        size: Tuple[int, int],
    ):
        return TorchSparseFeature(
            row=None,
            row_ptr=torch.as_tensor(row_ptr),
            col=torch.as_tensor(col),
            value=torch.as_tensor(value),
            size=size,
        )


class TorchFeatures(NamedTuple):
    features: Dict[str, TorchFeature]

    def to(self, *args, **kwargs):
        return TorchFeatures(
            features={k: v.to(*args, **kwargs) for k, v in self.features.items()},
        )

    @staticmethod
    def create_from_torch_feature(features: Dict[str, TorchFeature]):
        return TorchFeatures(features=features)


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
            other_raw=self.other_raw,
        )

    @staticmethod
    def create_from_tensor(
        n_feats: Optional[TorchFeatures],
        e_feats: Optional[TorchFeatures],
        y: Optional[Tensor],
        adjs_t: List[TorchEdgeIndex],
        root_index: Optional[Tensor],
        other_feats: Optional[Dict[str, Tensor]],
        other_raw: Optional[Any],
    ):
        return TorchEgoBatchData(
            n_feats=n_feats,
            e_feats=e_feats,
            y=y,
            adjs_t=adjs_t,
            root_index=root_index,
            other_feats=other_feats,
            other_raw=other_raw,
        )


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
            other_raw=self.other_raw,
        )

    @staticmethod
    def create_from_tensor(
        n_feats: Optional[TorchFeatures],
        e_feats: Optional[TorchFeatures],
        y: Optional[Tensor],
        adjs_t: TorchEdgeIndex,
        root_index: Optional[Tensor],
        n_num_per_sample: Optional[Tensor],
        e_num_per_sample: Optional[Tensor],
        other_feats: Optional[Dict[str, Tensor]],
        other_raw: Optional[Any],
    ):
        return TorchSubGraphBatchData(
            n_feats=n_feats,
            e_feats=e_feats,
            y=y,
            adjs_t=adjs_t,
            root_index=root_index,
            n_num_per_sample=n_num_per_sample,
            e_num_per_sample=e_num_per_sample,
            other_feats=other_feats,
            other_raw=other_raw,
        )
