import torch


class AGLAlgorithm(torch.nn.Module):
    def __init__(self):
        super().__init__()

    def encode(self, subgraph, node_x, edge_x=None, **kwargs):
        """
        gnns encode logic with initialized features.
        you should call such function in your subclass

        Args:
            subgraph: Union[TorchEgoBatchData, TorchSubGraphBatchData], include edge_index, node_feat, edge_feat et al.
            node_x: initialized node feat, that is,
                    transformation (such as linear transformation) over original node feats
            edge_x: initialized edge feat, that is,
                    transformation (such as linear transformation) over original edge feats
            **kwargs: other optional input

        Returns:
            embeddings. or dict of a set of tensors

        """
        pass
