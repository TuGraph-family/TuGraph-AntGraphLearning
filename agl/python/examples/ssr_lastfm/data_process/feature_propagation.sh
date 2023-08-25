base=`dirname "$0"`
cd "$base"
# UU
python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ../result/edge_weight.txt \
    --input_node_table_name ../result/features.txt \
    --output_table_name_prefix ../result/out_node_features_uu \
    --neighbor_distance 1 \
    --index_metas 'hash_index:edge_type:string' \
    --filter_cond 'index.edge_type in (U_U)' \
    --sample_condition 'topk(by=weight,limit=2, replacement=False, reverse=True)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    --algorithm sgc

# UI
python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ../result/edge_weight.txt \
    --input_node_table_name ../result/features.txt \
    --output_table_name_prefix ../result/out_node_features_ui \
    --neighbor_distance 1 \
    --index_metas 'hash_index:edge_type:string' \
    --filter_cond 'index.edge_type in (U_I)' \
    --sample_condition 'topk(by=weight,limit=2, replacement=False, reverse=True)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    --algorithm sgc

# UIU
python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ../result/edge_weight.txt \
    --input_node_table_name ../result/features.txt \
    --output_table_name_prefix ../result/out_node_features_uiu \
    --neighbor_distance 2 \
    --index_metas 'hash_index:edge_type:string' \
    --filter_cond 'index.edge_type in (I_U);index.edge_type in (U_I)' \
    --sample_condition 'topk(by=weight,limit=2, replacement=False, reverse=True)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    --algorithm sgc

# IU
python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ../result/edge_weight.txt \
    --input_node_table_name ../result/features.txt \
    --output_table_name_prefix ../result/out_node_features_iu \
    --neighbor_distance 1 \
    --index_metas 'hash_index:edge_type:string' \
    --filter_cond 'index.edge_type in (I_U);' \
    --sample_condition 'topk(by=weight,limit=2, replacement=False, reverse=True)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    --algorithm sgc

# IUI
python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ../result/edge_weight.txt \
    --input_node_table_name ../result/features.txt \
    --output_table_name_prefix ../result/out_node_features_iui \
    --neighbor_distance 2 \
    --index_metas 'hash_index:edge_type:string' \
    --filter_cond 'index.edge_type in (U_I);index.edge_type in (I_U)' \
    --sample_condition 'topk(by=weight,limit=2, replacement=False, reverse=True)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    --algorithm sgc

# IUU
python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ../result/edge_weight.txt \
    --input_node_table_name ../result/features.txt \
    --output_table_name_prefix ../result/out_node_features_iuu \
    --neighbor_distance 2 \
    --index_metas 'hash_index:edge_type:string' \
    --filter_cond 'index.edge_type in (U_U);index.edge_type in (I_U)' \
    --sample_condition 'topk(by=weight,limit=2, replacement=False, reverse=True)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'features','type':'dense','dim':64,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'edge_type','dtype':'string'}]}" \
    --algorithm sgc
