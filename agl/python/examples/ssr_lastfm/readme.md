# SSR 模型说明
## Model
    论文：[A Scalable Social Recommendation Framework with Decoupled Graph Neural Network] <https://dl.acm.org/doi/abs/10.1007/978-3-031-30678-5_39>

```
@inproceedings{tu2023scalable,
  title={A Scalable Social Recommendation Framework with Decoupled Graph Neural Network},
  author={Tu, Ke and Wu, Zhengwei and Hu, Binbin and Zhang, Zhiqiang and Cui, Peng and Li, Xiaolong and Zhou, Jun},
  booktitle={International Conference on Database Systems for Advanced Applications},
  pages={519--531},
  year={2023},
  organization={Springer}
}
```

### 数据预处理
以movielens为例子
#### 开源数据预处理
从 http://files.grouplens.org/datasets/hetrec2011/hetrec2011-lastfm-2k.zip 下载lastfm数据，创建data_process/data目录,将下载数据解压后放在data目录下，使用下述命令预处理成我们需要的格式。

```
python data_preprocess.py
```

#### 第一阶段数据预处理
首先我们要把原始数据压缩成子图(pb string)的形式，使用data_process/run_ssr_link.sh脚本命令
```
python ../../run_spark.py --spark_client_path /graph_ml/spark-3.1.1-bin-hadoop3.2 \
    --jar_resource_path ../../../java/target/flatv3-1.0-SNAPSHOT.jar \
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
```
其中包含以下几个文件,必须是csv文件：
- input_edge
    - 包含node1_id,node2_id,edge_id三个字段
    - 包含user-item边以及user-user社交边，user id范围从[0, user_num), item_id范围从[user_num, user_num+item_num)
- input_label
    - 包含node1_id,node2_id,seed,label,train_flag五个字段
        - 所有节点id从[0, user_num_item_num)，seed等于node1_id拼接node2_id, trai_flag为0, 1表示是否为训练集
- input_node_feature
    - 包含node_id,node_feature两个字段
    - 这个例子从node_feature为id特征，只有一个id
输出为根据train_flag切分的两张表, scheme为node1_id,node2_id,link,label,train_flag,graph_feature,graph_feature2。
其中link为上面input_label中的seed，graph_feature，graph_feature_2分别为node1_id和node2_id的图特征pb string。

#### 第一阶段模型训练
```
python ssr_ego.py
```
ssr模型第一阶段训练

#### 第一阶段模型打分
```
python ssr_ego_infer.py
```
利用一阶段模型，输入每个节点特征以及边表，离线inference产出节点表征result/features.txt以及每条边预训练的边权result/edge_weight.txt。

#### 第二阶段离线特征聚合
```
sh data_process/feature_propagation.sh
```
离线聚合path guilded 表征，得到U,UI, UU, UUI, I, IU, IUI, IUU 8条路径的特征。

#### 第二阶段模型训练
```
python ssr2_ego.py
```
输入8条路径表征和训练表训练模型第二阶段

#### 第二阶段模型测试
```
python ssr2_ego_eval.py
```
测试整个模型效果，计算prec@K和recall@K。

## BenchMark
* 效果
  * ssr 未调参，50 epoch, auc ~0.8左右, prec@15 ~ 0.36 左右 , recall@15 ~ 0.54左右 