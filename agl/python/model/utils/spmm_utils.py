import torch
import torch.nn as nn

CHUNK_SIZE_FOR_SPMM = 1000000


class SpecialSpmmFunction(torch.autograd.Function):
    """Special function for only sparse region backpropataion layer. """

    @staticmethod
    def forward(ctx, indices, values, shape, b):
        assert indices.requires_grad is False
        a = torch.sparse_coo_tensor(indices, values, shape)
        ctx.save_for_backward(a, b)
        ctx.N = shape[0]
        return torch.matmul(a, b)

    @staticmethod
    # grad_output shape [N,d]
    def backward(ctx, grad_output, CHUNK_SIZE=CHUNK_SIZE_FOR_SPMM):
        a, b = ctx.saved_tensors  # shape [N,N] [N,d]
        grad_values = grad_b = None  # shape [E] [N,d]

        if ctx.needs_input_grad[3]:
            grad_b = a.t().matmul(grad_output)
        if ctx.needs_input_grad[1]:
            L = a._indices().shape[1]
            grad_values = torch.zeros(L, dtype=b.dtype, device=a.device)
            for idx in range(0, L, CHUNK_SIZE):
                batch_indices = a._indices()[:, idx:idx + CHUNK_SIZE]

                a_batch = torch.index_select(grad_output, 0,
                                             batch_indices[0, :])
                b_batch = torch.index_select(b, 0, batch_indices[1, :])

                dot_prods = torch.sum(a_batch * b_batch, dim=1)
                grad_values[idx:idx + CHUNK_SIZE] = dot_prods

        return None, grad_values, None, grad_b


class SpecialSpmm(nn.Module):

    def forward(self, adj, b):
        # return SpecialSpmmFunction.apply(adj.indices(), adj.values(), adj.shape, b)
        return SpecialSpmmFunction.apply(adj._indices(), adj._values(),
                                         adj.shape, b)
