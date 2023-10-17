base=`dirname "$0"`
cd "$base"

python process_for_agl.py

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./datasets/lastfm/full_batch_datasets/edge_table.csv \
    --input_label_table_name ./datasets/lastfm/full_batch_datasets/full_batch_seed_table.csv \
    --input_node_table_name ./datasets/lastfm/full_batch_datasets/node_concated_feature_table.csv \
    --output_table_name_prefix ./datasets/lastfm/full_batch_datasets/output_graph_feature \
    --sample_condition 'random_sampler(limit=2000000, seed=34, replacement=false)' \
    --neighbor_distance 1 \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'node_feature','type':'dense','dim':1,'value':'int64'},{'name':'node_degree','type':'dense','dim':1,'value':'int64'},{'name':'node_type','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}]}" \
    --algorithm graph_level \
    --label_level label_on_node

cp ./datasets/lastfm/full_batch_datasets/output_graph_feature/part-* ./datasets/lastfm/full_batch_datasets/full_graph_feature.csv
