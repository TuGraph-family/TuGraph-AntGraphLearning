base=`dirname "$0"`
cd "$base"

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./edge_table.csv \
    --input_label_table_name ./link_table.csv \
    --input_node_table_name ./node_table.csv \
    --output_table_name_prefix ./output_graph_feature \
    --neighbor_distance 2 \
    --sample_condition 'random_sampler(limit=20, seed=34, replacement=false)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'node_feature','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'edge_feature','type':'dense','dim':1,'value':'int64'}]}]}" \
    --algorithm kcan
