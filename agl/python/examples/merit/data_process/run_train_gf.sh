#!/bin/sh
#****************************************************************#
# ScriptName: set_env.sh
# Author: $SHTERM_REAL_USER@alibaba-inc.com
# Create Date: 2023-04-26 16:53
# Modify Author: $SHTERM_REAL_USER@alibaba-inc.com
# Modify Date: 2023-06-26 10:33
# Function: 
#***************************************************************#
#wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/flatv3-1.0-SNAPSHOT.jar -O flatv3-1.0-SNAPSHOT.jar
#mvn clean package -Dmaven.test.skip
export SPARK_HOME=/ossfs/workspace/merit_model_test/data_process/spark-3.1.1-odps0.34.1
export PATH=$SPARK_HOME/bin/:$PATH
export HADOOP_CONF_DIR=$SPARK_HOME/conf
export ODPS_CONF_FILE=/ossfs/workspace/merit_model_test/data_process/spark-3.1.1-odps0.34.1/conf/odps.conf

# node_id,seed,time
#/ossfs/workspace/spark-3.1.1-odps0.34.1/bin/spark-submit  --class com.alipay.alps.flatv3.spark.DynamicGraph gnn.jar hop=2     index_metas='range_index:time:long'  filter_cond='index.time - seed.time <= 0'  other_output_schema='node:index.time' sample_cond='random_sampler(limit=10, seed=34, replacement=false)' intput_edge='select node1_id, node2_id, concat(node1_id, '_', node2_id, '_', ts) as edge_id, ts as time from wiki_dynamic_edge_lx_bidirec;' input_label='select node_id, node_id as seed, time from node_table_wiki_dynamic;' subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[]}]}" output_results="wiki_dynamic_subgraph_outputs_0615"   | tee logfile.txt

#/ossfs/workspace/merit_model_test/data_process/spark-3.1.1-odps0.34.1/bin/spark-submit  --class com.alipay.alps.flatv3.spark.DynamicGraph \
#    gnn.jar hop=2     \
#    index_metas='range_index:time:long'  \
#    filter_cond='index.time - seed.time <= 0'  \
#    other_output_schema='node:index.time' \
#    sample_cond='random_sampler(limit=10, seed=34, replacement=false)' \
#    input_edge="select node1_id, node2_id, edge_id, time from wiki_dynamic_edge_bidirec;" \
#    input_label="select node_id, node_id as seed, time from node_table_wiki_dynamic;"  \
#    input_edge_feature="select edge_id, edge_feature from wiki_dynamic_edge_feature;" \
#    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'time','type':'dense','dim':1,'value':'int64'}]}]}" \
#    output_results="wiki_dynamic_subgraph_outputs_0619"   | tee logfile.txt

/ossfs/workspace/merit_model_test/data_process/spark-3.1.1-odps0.34.1/bin/spark-submit --master local --class com.alipay.alps.flatv3.spark.DynamicGraph \
    print_gnn.jar hop=2 \
    index_metas='range_index:time:long' \
    filter_cond='index.time + 1 <= seed.time' \
    other_output_schema='node:index.time' \
    sample_cond='topk(limit=10, by=time, reverse=true, replacement=false)' \
    input_edge='file:////ossfs/workspace/merit_model_test/data_process/wiki_data2/train_edge_table.csv' \
    input_edge_feature='file:////ossfs/workspace/merit_model_test/data_process/wiki_data2/edge_feature.csv' \
    input_node_feature='file:////ossfs/workspace/merit_model_test/data_process/wiki_data2/node_feature.csv' \
    input_label='file:////ossfs/workspace/merit_model_test/data_process/wiki_data2/train_seed_table.csv' \
    subgraph_spec="{'node_spec':[{'node_name':'default','id_type':'string','features':[{'name':'node_id','type':'dense','dim':1,'value':'int64'}]}],'edge_spec':[{'edge_name':'default','n1_name':'default','n2_name':'default','id_type':'string','features':[{'name':'edge_id','type':'dense','dim':1,'value':'int64'},{'name':'time','type':'dense','dim':1,'value':'float32'}]}]}" \
    output_results='file:////ossfs/workspace/merit_model_test/data_process/wiki_data2/train_output_wikipedia' | tee logfile.txt
