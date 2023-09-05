base=`dirname "$0"`
cd "$base"

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./edge.csv \
    --input_label_table_name ./label.csv \
    --input_node_table_name ./node.csv \
    --output_table_name_prefix ./output_graph_feature_p_a_p \
    --hegnn_mode path \
    --index_metas "hash_index:relation:string" \
    --filter_condition 'neighbor.relation in (p_a);neighbor.relation in (a_p)' \
    --sample_condition 'random_sampler(limit=50, replacement=false)' \
    --neighbor_distance 2 \
    --train_flag flag \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'sparse_kv','type':'sparse_kv','dim':1902,'key':'int64','value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'relation','dtype':'string'}],'seed':{'type':'node'}}" \
    --algorithm hegnn

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./edge.csv \
    --input_label_table_name ./label.csv \
    --input_node_table_name ./node.csv \
    --output_table_name_prefix ./output_graph_feature_p_s_p \
    --hegnn_mode path \
    --index_metas "hash_index:relation:string" \
    --filter_condition 'neighbor.relation in (p_s);neighbor.relation in (s_p)' \
    --sample_condition 'random_sampler(limit=50, replacement=false)' \
    --neighbor_distance 2 \
    --train_flag flag \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'sparse_kv','type':'sparse_kv','dim':1902,'key':'int64','value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'edge_attr':[{'field':'relation','dtype':'string'}],'seed':{'type':'node'}}" \
    --algorithm hegnn

python merge_metapath.py
