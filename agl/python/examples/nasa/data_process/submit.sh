base=`dirname "$0"`
cd "$base"

python data_prepare.py

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name ./photo_edge.csv \
    --input_label_table_name ./photo_label.csv \
    --input_node_table_name ./photo_node.csv \
    --output_table_name_prefix ./output_graph_feature \
    --sample_condition 'random_sampler(limit=1000, replacement=false)' \
    --neighbor_distance 1 \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'sparse_kv','type':'kv','dim':745,'key':'uint32','value':'float32'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}],'seed':{'type':'node'}}" \
    --algorithm graph_level \
    --label_level label_on_node

cp ./output_graph_feature/part-* graph_features.csv
sed -i 's/\btrain\b/0/g' graph_features.csv
sed -i 's/\bval\b/1/g' graph_features.csv
sed -i 's/\btest\b/2/g' graph_features.csv
