base=`dirname "$0"`
cd "$base"

python data_prepare.py

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./ppi_edge_feature.csv \
    --input_label_table_name ./ppi_label.csv \
    --input_node_table_name ./ppi_node_feature_normalized.csv \
    --output_table_name_prefix ./output_graph_feature \
    --sample_condition 'random_sampler(limit=5000, replacement=false)' \
    --neighbor_distance 1 \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'feature','type':'dense','dim':50,'value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'seed':{'type':'node'}}" \
    --algorithm graph_level \
    --label_level label_on_node

python split_graph_features.py
