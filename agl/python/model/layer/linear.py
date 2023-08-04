import torch
from torch import Tensor
from torch.nn import Linear


class AGLLinear(Linear):
    """AGLLinear Applies a linear transformation to dense/sparse feature"""

    def __init__(
        self,
        in_features: int,
        out_features: int,
        bias: bool = True,
        device=None,
        dtype=None,
    ) -> None:
        super().__init__(in_features, out_features, bias, device, dtype)

    def forward(self, input: Tensor) -> Tensor:
        if input.is_sparse:
            h = torch.sparse.mm(input, self.weight)
            if self.bias is not None:
                h = h + self.bias
            return h
        else:
            return super().forward(input)
