export SPARK_HOME=/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1
export PATH=$SPARK_HOME/bin/:$PATH
export HADOOP_CONF_DIR=$SPARK_HOME/conf
export ODPS_CONF_FILE=/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/conf/odps.conf


# /graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.SGC \
#     /graph_ml/spark/gnn.jar hop=2 \
#     sample_cond='topk(by=weight,limit=50, seed=34, replacement=False,reverse=True)' \
#     input_edge='file:////graph_ml/agl/python/examples/ssr_lastfm/result/edge_weight.txt' \
#     input_node_feature='file:////graph_ml/agl/python/examples/ssr_lastfm/result/features.txt' \
#     subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}], 'edge_spec':[]}" \
#     output_results='file:////graph_ml/agl/python/examples/ssr_lastfm/result/out_node_features.txt' | tee logfile.txt
# UU
/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.SGC \
    gnn.jar hop=1 \
    index_metas='hash_index:edge_type:string' \
    filter_cond='index.edge_type in (U_U)' \
    sample_cond='topk(by=weight,limit=2, replacement=False, reverse=True)' \
    input_edge='file:////graph_ml/agl/python/examples/ssr_lastfm/result/edge_weight.csv' \
    input_node_feature='file:////graph_ml/agl/python/examples/ssr_lastfm/result/features.txt' \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    output_results='file:////graph_ml/agl/python/examples/ssr_lastfm/result/out_node_features_uu' | tee logfile.txt

# UI
/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.SGC \
    gnn.jar hop=1 \
    index_metas='hash_index:edge_type:string' \
    filter_cond='index.edge_type in (U_I);' \
    sample_cond='topk(by=weight,limit=2, replacement=False, reverse=True)' \
    input_edge='file:////graph_ml/agl/python/examples/ssr_lastfm/result/edge_weight.csv' \
    input_node_feature='file:////graph_ml/agl/python/examples/ssr_lastfm/result/features.txt' \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    output_results='file:////graph_ml/agl/python/examples/ssr_lastfm/result/out_node_features_ui' | tee logfile.txt

# UIU
/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.SGC \
    gnn.jar hop=2 \
    index_metas='hash_index:edge_type:string' \
    filter_cond='index.edge_type in (I_U);index.edge_type in (U_I)' \
    sample_cond='topk(by=weight,limit=2, replacement=False, reverse=True)' \
    input_edge='file:////graph_ml/agl/python/examples/ssr_lastfm/result/edge_weight.csv' \
    input_node_feature='file:////graph_ml/agl/python/examples/ssr_lastfm/result/features.txt' \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    output_results='file:////graph_ml/agl/python/examples/ssr_lastfm/result/out_node_features_uiu' | tee logfile.txt

# IU
/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.SGC \
    gnn.jar hop=1 \
    index_metas='hash_index:edge_type:string' \
    filter_cond='index.edge_type in (I_U);' \
    sample_cond='topk(by=weight,limit=2, replacement=False, reverse=True)' \
    input_edge='file:////graph_ml/agl/python/examples/ssr_lastfm/result/edge_weight.csv' \
    input_node_feature='file:////graph_ml/agl/python/examples/ssr_lastfm/result/features.txt' \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    output_results='file:////graph_ml/agl/python/examples/ssr_lastfm/result/out_node_features_iu' | tee logfile.txt

# IUI
/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.SGC \
    gnn.jar hop=2 \
    index_metas='hash_index:edge_type:string' \
    filter_cond='index.edge_type in (U_I);index.edge_type in (I_U)' \
    sample_cond='topk(by=weight,limit=2, replacement=False, reverse=True)' \
    input_edge='file:////graph_ml/agl/python/examples/ssr_lastfm/result/edge_weight.csv' \
    input_node_feature='file:////graph_ml/agl/python/examples/ssr_lastfm/result/features.txt' \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    output_results='file:////graph_ml/agl/python/examples/ssr_lastfm/result/out_node_features_iui' | tee logfile.txt

# IUU
/graph_ml/spark/spark_client_agl/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.SGC \
    gnn.jar hop=2 \
    index_metas='hash_index:edge_type:string' \
    filter_cond='index.edge_type in (U_U);index.edge_type in (I_U)' \
    sample_cond='topk(by=weight,limit=2, replacement=False, reverse=True)' \
    input_edge='file:////graph_ml/agl/python/examples/ssr_lastfm/result/edge_weight.csv' \
    input_node_feature='file:////graph_ml/agl/python/examples/ssr_lastfm/result/features.txt' \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    output_results='file:////graph_ml/agl/python/examples/ssr_lastfm/result/out_node_features_iuu' | tee logfile.txt