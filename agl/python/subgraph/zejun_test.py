#!/usr/bin/python
# coding: utf-8
import sys
import numpy as np
from agl.python.data.dataset import PPITorchDataset
from agl.python.data.collate import AGLHomoCollateForPyG
from agl.python.data.column import AGLDenseColumn, AGLRowColumn, AGLMultiDenseColumn
from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray
from agl.python.encoder.geniepath import Breadth, Depth, GeniePathLayer
from agl.python.subgraph.pyg_inputs import TorchSubGraphBatchData

import numpy as np

from agl.python.subgraph.subgraph import PySubGraph
from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray


node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddDenseSpec("node_id", DenseFeatureSpec("node_id", 1, AGLDType.INT64))
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddDenseSpec("time", DenseFeatureSpec("time", 172, AGLDType.FLOAT))

sg = PySubGraph([node_spec], [edge_spec])

pb_string = [b'CjAKB2RlZmF1bHQSJQoLCgkKATEKBDgyMjgSFgoUCgdub2RlX2lkEgkIASIFCgMBpEASLQoHZGVmYXVsdBIiCgIKABIICgQKAgAAEgAiB2RlZmF1bHQqB2RlZmF1bHQyABoVGhMIARIPCgdkZWZhdWx0EgQKAgAB']
pb_string_double = [bytearray(pb_string[0])]
sg.from_pb_bytes(pb_string_double)

print(f">>>>> edge index: {sg.get_edge_index()}")
e_time = sg.get_edge_dense_feature("default", "time")
print(f">>>>> edge feature: {e_time}")



# node 上无特征，边上有时间维度特征，特征长度为1，类型为int64
node_spec = NodeSpec("default", AGLDType.STR)
node_spec.AddDenseSpec("node_id", DenseFeatureSpec("node_id", 1, AGLDType.INT64))
# edge related spec
edge_spec = EdgeSpec("default", node_spec, node_spec, AGLDType.STR)
edge_spec.AddDenseSpec("time", DenseFeatureSpec("time", 172, AGLDType.FLOAT))

node1_id_column = AGLRowColumn(name="node1_id")
node2_id_column = AGLRowColumn(name="node2_id")
time_column = AGLDenseColumn(name="time", dim=1, dtype=np.float32)
label_column = AGLDenseColumn(name="label", dim=1, dtype=np.float32)
my_collate = AGLHomoCollateForPyG(node_spec, edge_spec, columns=[node1_id_column, node2_id_column, time_column, label_column],
                                  label_name="label")
gf = pb_string[0]
node1_id = "25"
node2_id = "8252"
time = "1543.0"
label = "0"
data = {
    "graph_feature": gf,
    "node1_id": node1_id,
    "node2_id": node2_id,
    "time": time,
    "label": label
}
res = my_collate([data])

# data = [{'node1_id': b'25', 'node2_id': b'8252', 'graph_feature': b'CjEKB2RlZmF1bHQSJgoMCgoKAjI1CgQ4MjUyEhYKFAoHbm9kZV9pZBIJCAEiBQoDGbxAEi0KB2RlZmF1bHQSIgoCCgASCAoECgIAABIAIgdkZWZhdWx0KgdkZWZhdWx0MgAaFRoTCAESDwoHZGVmYXVsdBIECgIAAQ==', 'time': '1543.0', 'label': '0'}, {'node1_id': b'13', 'node2_id': b'8239', 'graph_feature': b'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', 'time': b'1555.0', 'label': b'0'}]
# res = my_collate(data)
print(res)

edge_index = res.adjs_t
print(f">>>>>>> edge index original: {edge_index}")
print(f">>>>>>> edge index get index: {edge_index.edge_index}")