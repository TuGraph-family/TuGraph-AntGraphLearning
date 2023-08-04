from typing import Dict, Union

import torch

from agl.python.model.layer.linear import AGLLinear


class InitialLayer(torch.nn.Module):
    """for each node or edge, we may have different (type) features, e.g. dense features, sparse kv features
    This layer is used to transfer those features

    """

    def __init__(
        self,
        feat_dim: Dict[str, int],
        hidden_dim: Union[int, Dict[str, int]],
        merge_op: str = "concat",
    ):
        super().__init__()
        self._feat_dim = feat_dim
        self._hidden_dim = hidden_dim
        self._merge_op = merge_op
        self._module_dict = None
        self._build_module_dict()

    def get_out_dim(self):
        if self._merge_op == "concat":
            if isinstance(self._hidden_dim, int):
                return self._hidden_dim * len(self._feat_dim)
            elif isinstance(self._hidden_dim, dict):
                import numpy as np

                return np.sum([v for v in self._hidden_dim.values()])

    def _build_module_dict(self):
        if isinstance(self._hidden_dim, int):
            self._module_dict = torch.nn.ModuleDict(
                {
                    name: AGLLinear(in_dim, self._hidden_dim)
                    for name, in_dim in self._feat_dim.items()
                }
            )
        elif isinstance(self._hidden_dim, dict):
            self._module_dict = torch.nn.ModuleDict(
                {
                    name: AGLLinear(in_dim, self._hidden_dim[name])
                    for name, in_dim in self._feat_dim.items()
                }
            )
        else:
            raise NotImplementedError

    def forward(self, feat: Dict[str, torch.Tensor]):
        if self._module_dict is None:
            return feat

        if self._merge_op == "concat":
            return torch.cat([lin(feat[k]) for k, lin in self._module_dict.items()])
        else:
            raise NotImplementedError
