base=`dirname "$0"`
cd "$base"

python ../../run_spark.py \
    --jar_resource_path ../../../../java/target/flatv3-1.0-SNAPSHOT.jar \
    --input_edge_table_name data/agl_gzoo_bmdata_ssr_lastfm_open_source_edge_table.csv \
    --input_label_table_name data/agl_gzoo_bmdata_ssr_lastfm_open_source_node_label.csv \
    --input_node_table_name data/agl_gzoo_bmdata_ssr_lastfm_open_source_node_table.csv \
    --output_table_name_prefix data/output_graph_feature \
    --neighbor_distance 2 \
    --train_flag "train_flag" \
	--sample_condition 'random_sampler(limit=50, seed=34, replacement=false)' \
    --subgraph_spec "{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'node_feature','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}]}" \
    --algorithm link_level

cp data/output_graph_feature_1/part* subgraph_ssr_lastfm_train.csv
cp data/output_graph_feature_0/part* subgraph_ssr_lastfm_test.csv
