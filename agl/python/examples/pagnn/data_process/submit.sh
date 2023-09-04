base=`dirname "$0"`
cd "$base"

python data_prepare.py

python ../../run_spark.py \
    --mode yarn \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./facebook_edge_t.csv \
    --input_label_table_name ./facebook_sample_t.csv \
    --input_node_table_name ./facebook_node_t.csv \
    --output_table_name_prefix ./output_graph_feature \
    --neighbor_distance 2 \
    --remove_edge_among_roots "True" \
    --sample_condition "random_sampler(limit=200, replacement=false)" \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'node_feature','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}]}" \
    --algorithm pagnn

python split_graph_features.py
