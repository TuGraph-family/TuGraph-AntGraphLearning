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
