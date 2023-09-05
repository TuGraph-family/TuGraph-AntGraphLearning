base=`dirname "$0"`
cd "$base"

python data_process.py

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./citeseer_edge_table.csv \
    --input_label_table_name ./citeseer_label.csv \
    --input_node_table_name ./citeseer_node_table.csv \
    --output_table_name_prefix ./output_graph_feature \
    --train_flag train_flag \
    --neighbor_distance 2 \
    --sample_condition 'random_sampler(limit=100, replacement=false)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'sparse_kv','type':'kv','dim':3703,'key':'uint32','value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'seed':{'type':'node'}}" \
    --algorithm node_level

cp output_graph_feature_-1/part* graph_feature_-1.csv
cp output_graph_feature_0/part* graph_feature_0.csv
cp output_graph_feature_1/part* graph_feature_1.csv
cp output_graph_feature_2/part* graph_feature_2.csv
