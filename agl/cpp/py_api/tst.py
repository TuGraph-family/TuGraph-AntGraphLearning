from pyagl import pyagl
from pyagl.pyagl import AGLDType, DenseFeatureSpec, SparseKVSpec, SparseKSpec, NodeSpec, EdgeSpec, SubGraph, NDArray
import numpy as np

def test():
    # not used
    import time
    sg = pyagl.SubGraph()
    data = [f"2088_{i}" for i in range(1000 * 1000)]
    data2 = [str.encode('utf-8') for str in data]
    s1 = time.time()
    sg.TestString(data)
    s2 = time.time()
    sg.TestV2(data)
    s3 = time.time()
    sg.TestV3(data)
    s4 = time.time()
    sg.TestV4(data2)
    s5 = time.time()
    print(f"v1 :, time:{s2 - s1:.4f}s")
    print(f"v2 :, time:{s3 - s2:.4f}s")
    print(f"v3 :, time:{s4 - s3:.4f}s")
    print(f"v4 :, time:{s5 - s4:.4f}s")

def test2():
    # not used
    import time
    sg = pyagl.SubGraph()
    s0 = time.time()
    data = [f"2088_{i}" for i in range(1000 * 1000)]
    data2 = [bytearray(str.encode('utf-8')) for str in data]
    s1 = time.time()
    sg.TestString(data)
    s2 = time.time()
    sg.TestBytesArray(data2)
    s3 = time.time()
    print(f"v1 :, time:{s2 - s1:.4f}s")
    print(f"v2 bytes array :, time:{s3 - s2:.4f}s")
    print(f"encode time:{s1 -s0}")


def check_dense_spec(df_name:str, df_dim:int, df_dtype, df_spec_to_check):
    print(f"dense ground truth, name:{df_name}, dim:{df_dim}, dtype:{df_dtype}")
    print(f"dense speck_to_check， name:{df_spec_to_check.GetFeatureName()}, "
          f"dim:{df_spec_to_check.GetDim()}, dtype:{df_spec_to_check.GetFeatureDtype()}")


def check_spkv_spec(name:str, max_dim:int, key_dtype, val_dtype, spkv_spec_to_check):
    print(f"spkv ground truth, name:{name}, max_dim:{max_dim}, key_dtype:{key_dtype}, val_dtype:{val_dtype}")
    print(f"spkv_spec_to_check,  name:{spkv_spec_to_check.GetFeatureName()}, max_dim:{spkv_spec_to_check.GetMaxDim()}, "
          f"key_dtype:{spkv_spec_to_check.GetKeyDtype()}, val_dtype:{spkv_spec_to_check.GetValDtype()}")


def test_parse_from_python():
    # 1. node related spec
    n_name = "default"
    n_id_dtype = AGLDType.STR
    # 1.1 node dense spec
    n_df_name = "dense"
    n_df_dim = 3
    n_df_dtype = AGLDType.FLOAT
    # 1.2 node sp kv spec
    n_spkv_name = "nf"
    n_max_dim = 10
    n_key_dtype = AGLDType.INT64
    n_val_dtype = AGLDType.INT64
    node_spec = NodeSpec(n_name, n_id_dtype)
    node_spec.AddDenseSpec(n_df_name, DenseFeatureSpec(n_df_name, n_df_dim, n_df_dtype))
    node_spec.AddSparseKVSpec(n_spkv_name, SparseKVSpec(n_spkv_name, n_max_dim, n_key_dtype, n_val_dtype))

    # check node dense
    check_dense_spec(n_df_name, n_df_dim, n_df_dtype, node_spec.GetDenseFeatureSpec()[n_df_name])
    check_spkv_spec(n_spkv_name, n_max_dim, n_key_dtype, n_val_dtype, node_spec.GetSparseKVSpec()[n_spkv_name])

    # 2. edge related spec
    e_name = "default"
    n1_name = "default"
    n2_name = "default"
    e_id_dtype = AGLDType.STR
    # 2.1 edge spkv spec
    e_kv_name = "ef"
    e_max_dim = 10
    e_key_dtype = AGLDType.INT64
    e_val_dtype = AGLDType.FLOAT
    edge_spec = EdgeSpec(e_name, node_spec, node_spec, e_id_dtype)
    edge_spec.AddSparseKVSpec(e_kv_name, SparseKVSpec(e_kv_name, e_max_dim, e_key_dtype, e_val_dtype))

    sg = SubGraph()
    sg.AddNodeSpec(n_name, node_spec)
    sg.AddEdgeSpec(e_name, edge_spec)
    check_spkv_spec(e_kv_name, e_max_dim, e_key_dtype, e_val_dtype, edge_spec.GetSparseKVSpec()[e_kv_name])

    pb_string = ["CnIKB2RlZmF1bHQSZwoLCgkKATEKATIKATMSWAozCgVkZW5zZRIqCAMSJgokzczMPc3MjD8AAIA/zcxMPs3MDEAAAABAmpmZPjMzU0AAAEBAEiEKAm5mEhsKBQoDAgMGEggKBgEKAgMECioICgYBAQIDAwMScAoHZGVmYXVsdBJlCgwKCgoDMS0yCgMyLTMSDQoFCgMBAgISBAoCAQIiB2RlZmF1bHQqB2RlZmF1bHQyNBIyCgJlZhIsCgQKAgMGEggKBgECCQIDChoaChjNzIw/zcwMQJqZIUHNzAxAMzNTQGZmBkAaCwoJEgdkZWZhdWx0"]
    sg.CreateFromPB(pb_string, False, False)
    n_per_samle = sg.GetNodeNumPerSample()
    print(f"==========n_per_samle=======>>>>:{n_per_samle}")
    e_per_sample = sg.GetEdgeNumPerSample()
    print(f"==========e_per_sample=======>>>>:{e_per_sample}")
    #nd_array_test = sg.TestGetNodeDenseFeature(n_name, n_df_name)
    #py_n_d_array_test = np.array(nd_array_test)
    #print(f"======== node:{n_name}, f:{n_df_name} array >>>:{py_n_d_array_test}")
    # node dense
    n_d_f_array = sg.GetNodeDenseFeatureArray(n_name, n_df_name)
    n_d_array = n_d_f_array.GetFeatureArray()
    py_n_d_array = np.array(n_d_array)
    print(f"======== node:{n_name}, f:{n_df_name} array >>>:{py_n_d_array}")
    # node sparse kv
    n_spkv_f_array = sg.GetNodeSparseKVArray(n_name, n_spkv_name)
    spkv_nd_array = n_spkv_f_array.GetFeatureArray()
    print(f"======== node:{n_name}, f:{n_spkv_name} array >>> indicces offset:{np.array(spkv_nd_array.GetIndOffset())}, "
          f"keys: {np.array(spkv_nd_array.GetKeys())}, value:{np.array(spkv_nd_array.GetVals())}")

    # edge feature
    # edge sparse feature
    e_spkv_f_array = sg.GetEdgeSparseKVArray(e_name, e_kv_name)
    e_kv_nd_array = e_spkv_f_array.GetFeatureArray()
    print(f"======== edge:{e_name}, f:{e_kv_name} array >>> indicces offset:{np.array(e_kv_nd_array.GetIndOffset())}, "
          f"keys: {np.array(e_kv_nd_array.GetKeys())}, value:{np.array(e_kv_nd_array.GetVals())}")

    # adj
    e_adj = sg.GetEdgeIndexCSR(e_name)
    ind_ptr = np.array(e_adj.GetIndPtr())
    indices = np.array(e_adj.GetIndices())

    print(f"==========edge csr, name: {e_name}, row number:{e_adj.row_num}, col number:{e_adj.col_num}, indptr:{ind_ptr}, indices: {indices}")





def test_nd_array():
    my_nd = NDArray(2,1, AGLDType.FLOAT)
    my_nd.FloatNd()
    ny_nd = np.array(my_nd)
    print(f"============= nynd: {ny_nd}")


test2()
#test_parse_from_python()
#test_nd_array()