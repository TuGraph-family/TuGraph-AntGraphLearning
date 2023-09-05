base=`dirname "$0"`
cd "$base"

python data_prepare.py

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./train_edge_table.csv \
    --input_label_table_name ./train_seed_table.csv \
    --input_node_table_name ./node_feature.csv \
    --output_table_name_prefix ./train_output_graph_feature \
    --neighbor_distance 2 \
    --index_metas "range_index:time:long" \
    --filter_condition 'index.time + 1 <= seed.time' \
    --sample_condition 'topk(limit=10, by=time, reverse=true, replacement=false)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'node_id','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'edge_id','type':'dense','dim':1,'value':'int64'},{'name':'time','type':'dense','dim':1,'value':'float32'}]}],'edge_attr':[{'field':'time','dtype':'long'}],'seed':{'type':'node','attr':[{'field':'time','dtype':'long'}]}}" \
    --algorithm node_level

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./full_edge_table.csv \
    --input_label_table_name ./full_seed_table.csv \
    --input_node_table_name ./node_feature.csv \
    --output_table_name_prefix ./full_output_graph_feature \
    --neighbor_distance 2 \
    --index_metas "range_index:time:long" \
    --filter_condition 'index.time + 1 <= seed.time' \
    --sample_condition 'topk(limit=10, by=time, reverse=true, replacement=false)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'node_id','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'edge_id','type':'dense','dim':1,'value':'int64'},{'name':'time','type':'dense','dim':1,'value':'float32'}]}],'edge_attr':[{'field':'time','dtype':'long'}],'seed':{'type':'node','attr':[{'field':'time','dtype':'long'}]}}" \
    --algorithm node_level

cp train_output_graph_feature/part* train_wiki_graph_feature.csv
cp full_output_graph_feature/part* full_wiki_graph_feature.csv
python gen_train_data.py
