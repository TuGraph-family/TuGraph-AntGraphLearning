from agl.python.proto.graph_feature_pb2 import GraphFeature, Nodes, IDs, BytesList,
from agl.python.proto.graph_feature_pb2 import Features, DenseFeatures, SparseKVFeatures, SparseKFeatures

def mock_graph_feature():
    gfeat = GraphFeature(
        nodes={
            "user": Nodes(
                nids=IDs(str=BytesList(value=["u1", "u23", "u100"])),
                features=Features(
                    dfs={
                        "df": DenseFeatures(dim=2, df=)
                    },
                )
            )
        }
    )