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
```
python data_preprocess.py
```
从http://files.grouplens.org/datasets/hetrec2011/hetrec2011-lastfm-2k.zip下载lastfm数据，创建data_process/data目录,将下载数据解压后放在data目录下，使用上述命令预处理成我们需要的格式。

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
  * ssr 未调参，50 epoch, auc ~0.8左右, prec@15 ~ 0.36 左右 (原论文 0.353), recall@15 ~ 0.54左右 (原论文0.536)
  ```
    Epoch: 01, Loss: 0.8043, auc: 0.5971, train_time: 12.107816, val_time: 0.483347
    Epoch: 02, Loss: 0.7139, auc: 0.6542, train_time: 11.733946, val_time: 0.328861
    Epoch: 03, Loss: 0.6792, auc: 0.7151, train_time: 11.480709, val_time: 0.303506
    Epoch: 04, Loss: 0.6656, auc: 0.7291, train_time: 11.522599, val_time: 0.294176
    Epoch: 05, Loss: 0.6560, auc: 0.7250, train_time: 11.682429, val_time: 0.445539
    Epoch: 06, Loss: 0.6206, auc: 0.7493, train_time: 11.510933, val_time: 0.445137
    Epoch: 07, Loss: 0.6160, auc: 0.7491, train_time: 11.684788, val_time: 0.297126
    Epoch: 08, Loss: 0.6176, auc: 0.7612, train_time: 11.519130, val_time: 0.306026
    Epoch: 09, Loss: 0.6303, auc: 0.7688, train_time: 11.692025, val_time: 0.327492
    Epoch: 10, Loss: 0.6187, auc: 0.7706, train_time: 11.634877, val_time: 0.337790
    Epoch: 11, Loss: 0.5952, auc: 0.7768, train_time: 11.609031, val_time: 0.310153
    Epoch: 12, Loss: 0.5875, auc: 0.7667, train_time: 11.909676, val_time: 0.292688
    Epoch: 13, Loss: 0.5945, auc: 0.7681, train_time: 11.806919, val_time: 0.295133
    Epoch: 14, Loss: 0.5876, auc: 0.7827, train_time: 11.788707, val_time: 0.338131
    Epoch: 15, Loss: 0.5818, auc: 0.7793, train_time: 11.589313, val_time: 0.307520
    Epoch: 16, Loss: 0.5770, auc: 0.7782, train_time: 11.764631, val_time: 0.302438
    Epoch: 17, Loss: 0.5698, auc: 0.7869, train_time: 11.454369, val_time: 0.413306
    Epoch: 18, Loss: 0.5636, auc: 0.7918, train_time: 11.743885, val_time: 0.409539
    Epoch: 19, Loss: 0.5648, auc: 0.7912, train_time: 11.727757, val_time: 0.298214
    Epoch: 20, Loss: 0.5618, auc: 0.7923, train_time: 11.660682, val_time: 0.327231
    Epoch: 21, Loss: 0.5530, auc: 0.7943, train_time: 11.615249, val_time: 0.286587
    Epoch: 22, Loss: 0.5508, auc: 0.7883, train_time: 11.253371, val_time: 0.331646
    Epoch: 23, Loss: 0.5518, auc: 0.7927, train_time: 11.484581, val_time: 0.331716
    Epoch: 24, Loss: 0.5418, auc: 0.7972, train_time: 11.528899, val_time: 0.308683
    Epoch: 25, Loss: 0.5456, auc: 0.7990, train_time: 11.929682, val_time: 0.310510
    Epoch: 26, Loss: 0.5350, auc: 0.7929, train_time: 11.604513, val_time: 0.297678
    Epoch: 27, Loss: 0.5394, auc: 0.7989, train_time: 11.453251, val_time: 0.292810
    Epoch: 28, Loss: 0.5320, auc: 0.8010, train_time: 11.624455, val_time: 0.309475
    Epoch: 29, Loss: 0.5338, auc: 0.8009, train_time: 11.762576, val_time: 0.294682
    Epoch: 30, Loss: 0.5297, auc: 0.7962, train_time: 11.660346, val_time: 0.292787
    Epoch: 31, Loss: 0.5274, auc: 0.8027, train_time: 11.862746, val_time: 0.291134
    Epoch: 32, Loss: 0.5272, auc: 0.8037, train_time: 11.743582, val_time: 0.398789
    Epoch: 33, Loss: 0.5226, auc: 0.8007, train_time: 11.650809, val_time: 0.296985
    Epoch: 34, Loss: 0.5226, auc: 0.7967, train_time: 11.597193, val_time: 0.321086
    Epoch: 35, Loss: 0.5177, auc: 0.8026, train_time: 11.647320, val_time: 0.304086
    Epoch: 36, Loss: 0.5195, auc: 0.8025, train_time: 11.715076, val_time: 0.287590
    Epoch: 37, Loss: 0.5158, auc: 0.7962, train_time: 11.835550, val_time: 0.282753
    Epoch: 38, Loss: 0.5152, auc: 0.7973, train_time: 11.643948, val_time: 0.397647
    Epoch: 39, Loss: 0.5096, auc: 0.8003, train_time: 11.644680, val_time: 0.329439
    Epoch: 40, Loss: 0.5094, auc: 0.8017, train_time: 11.574507, val_time: 0.300908
    Epoch: 41, Loss: 0.5082, auc: 0.7962, train_time: 11.725015, val_time: 0.329480
    Epoch: 42, Loss: 0.5032, auc: 0.8004, train_time: 11.562064, val_time: 0.303209
    Epoch: 43, Loss: 0.5043, auc: 0.7983, train_time: 11.420388, val_time: 0.310526
    Epoch: 44, Loss: 0.5006, auc: 0.7941, train_time: 11.590370, val_time: 0.290690
    Epoch: 45, Loss: 0.4993, auc: 0.7979, train_time: 11.677804, val_time: 0.282144
    Epoch: 46, Loss: 0.4959, auc: 0.7978, train_time: 11.377506, val_time: 0.420724
    Epoch: 47, Loss: 0.4935, auc: 0.7913, train_time: 12.186495, val_time: 0.289656
    Epoch: 48, Loss: 0.4935, auc: 0.7926, train_time: 11.643528, val_time: 0.398964
    Epoch: 49, Loss: 0.4906, auc: 0.7975, train_time: 11.857935, val_time: 0.353652
    Epoch: 50, Loss: 0.4884, auc: 0.7921, train_time: 11.917154, val_time: 0.295094
  ```
