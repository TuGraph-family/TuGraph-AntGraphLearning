from agl.python.proto.graph_feature_pb2 import *

DF = Features.DenseFeatures
SpK = Features.SparseKFeatures
SpKV = Features.SparseKVFeatures


def mock_graph_feature():
    gfeat = GraphFeature(
        nodes={
            "user": Nodes(
                nids=IDs(str=BytesList(value=[b"u1", b"u23", b"u100"])),
                features=Features(
                    dfs={
                        "label": DF(dim=1, i64s=Int64List(value=[0, 5, 0])),
                        "df_f32": DF(dim=8, f32s=FloatList(value=[1.0] * 8 + [2.0] * 8 + [3.0] * 8)),
                        "df_f64": DF(dim=8, f64s=Float64List(value=[1.0] * 8 + [2.0] * 8 + [3.0] * 8)),
                        "df_i64": DF(dim=8, i64s=Int64List(value=[1] * 8 + [2] * 8 + [3] * 8)),
                        "df_raws": DF(dim=2, raws=BytesList(value=[b"1"] * 2 + [b"2"] * 2 + [b"3"] * 2)),
                    },
                    spkvs={
                        "spkv_f32": SpKV(lens=Int64List(value=[3, 2, 1]),
                                         keys=Int64List(value=[0, 2, 3, 0, 2, 1]),
                                         f32s=FloatList(value=[1.0] * 6))
                    },
                )
            ),
        },
        edges={
            "u2u": Edges(
                csr=Edges.CSR(indptr=Int64List(value=[0, 3, 5, 6]),
                              nbrs_indices=Int64List(value=[0, 1, 2, 0, 1, 2])),
                n1_name="user", n2_name="user",
                feats=Features(
                    dfs={"edf": DF(dim=1, f32s=FloatList(value=[0.1] * 6))}
                )
            )
        },
        root=GraphFeature.Root(nidx=GraphFeature.Root.OneItem(idx=0, name="user"))
    )

    with open("graph_feature.data", "wb") as writer:
        import base64
        writer.write(base64.b64encode(gfeat.SerializeToString()))


if __name__ == "__main__":
    print(mock_graph_feature())