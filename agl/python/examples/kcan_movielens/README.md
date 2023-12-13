# KCAN 模型说明
## Model
    论文：[Conditional graph attention networks for distilling and refining knowledge graphs in recommendation] <https://arxiv.org/abs/2111.02100>

```
@inproceedings{tu2021conditional,
  title={Conditional graph attention networks for distilling and refining knowledge graphs in recommendation},
  author={Tu, Ke and Cui, Peng and Wang, Daixin and Zhang, Zhiqiang and Zhou, Jun and Qi, Yuan and Zhu, Wenwu},
  booktitle={Proceedings of the 30th ACM International Conference on Information \& Knowledge Management},
  pages={1834--1843},
  year={2021}
}
```

## 说明
> Warning: 模型部分实现和论文中略有不同，原文是知识图谱表征学习和kcan交替训练，这里只有kcan的训练。同时开源数据只有正边没有负边，所以负样本是随机采样的导致数据集中负样本和原论文不一样，因此效果并不能完全对齐论文。

### 数据下载：
从 https://drive.google.com/drive/folders/12_mU1jt7ntuWEMQ-bogF0cLQjFJijnab?usp=sharing 下载数据文件,把图数据文件node_table.csv,link_table.csv,edge_table.csv放在data_process/目录下。

### 数据预处理
以movielens为例子

首先我们要把原始数据压缩成子图(pb string)的形式，使用如下data_process/submit.sh的命令。

由于link模式的样本量巨大，用户需要100G内存的机器运行spark任务，用户需要修改[start_docker_with_image.sh](../../../../docker/start_docker_with_image.sh)给虚拟机分配100G内存，同时修改[run_spark_template.sh](../run_spark_template.sh)配置spark.executor.memory=90g和spark.driver.memory=90g。
对于缺少资源的用户，可以从上面的链接中下载预先采样的子图数据part-subgraph_kcan_train_test.csv，放在data_process/output_graph_feature目录下

```
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
```
主要包含以下几个文件：
- input_edge
    - 包含node1_id,node2_id,edge_id三个字段
- input_edge_feature
    - 包含edge_id，edge_feature字段
- input_label
    - 包含node_id,seed,label,train_flag四个字段
        - 原始movielens数据中不包含负边，这里表中需要提前采样好负边。
        - 其中对于每条训练/测试样本，在这个文件产生2条样本，node_id分别是节点1和节点2的id，seed是边id。
- input_node_feature
    - 包含node_id,node_feature两个字段

```
cd data_process
python split_graph_features.py
```
运行上面的脚本，将output_graph_feature目录下的子图根据train_flag划分为subgraph_kcan_movielens_train.txt和subgraph_kcan_movielens_test.txt文件给下游训练。

### 模型运行
```
python kcan_subgraph_adj.py
```

## BenchMark
* 效果
  * kcan 未调参，100 epoch, AUC ~ 0.9 左右 (原论文 0.907)

* 效率  * kcan
```
    Epoch: 01, Loss: 0.4570, auc: 0.8826, best_auc: 0.8826, train_time: 154.442953, val_time: 30.473222
    Epoch: 02, Loss: 0.4234, auc: 0.8841, best_auc: 0.8841, train_time: 151.677146, val_time: 29.973186
    Epoch: 03, Loss: 0.4215, auc: 0.8842, best_auc: 0.8842, train_time: 153.905838, val_time: 32.188418
    Epoch: 04, Loss: 0.4204, auc: 0.8845, best_auc: 0.8845, train_time: 153.808938, val_time: 30.013850
    Epoch: 05, Loss: 0.4196, auc: 0.8847, best_auc: 0.8847, train_time: 155.965477, val_time: 30.001599
    Epoch: 06, Loss: 0.4190, auc: 0.8847, best_auc: 0.8847, train_time: 158.372542, val_time: 29.704480
    Epoch: 07, Loss: 0.4182, auc: 0.8849, best_auc: 0.8849, train_time: 159.528338, val_time: 29.556624
    Epoch: 08, Loss: 0.4174, auc: 0.8851, best_auc: 0.8851, train_time: 157.751968, val_time: 30.219867
    Epoch: 09, Loss: 0.4165, auc: 0.8851, best_auc: 0.8851, train_time: 158.100552, val_time: 30.410751
    Epoch: 10, Loss: 0.4155, auc: 0.8854, best_auc: 0.8854, train_time: 158.980367, val_time: 29.147344
    Epoch: 11, Loss: 0.4144, auc: 0.8856, best_auc: 0.8856, train_time: 160.779557, val_time: 29.913888
    Epoch: 12, Loss: 0.4130, auc: 0.8860, best_auc: 0.8860, train_time: 161.443003, val_time: 29.205204
    Epoch: 13, Loss: 0.4117, auc: 0.8863, best_auc: 0.8863, train_time: 162.105299, val_time: 29.815239
    Epoch: 14, Loss: 0.4099, auc: 0.8873, best_auc: 0.8873, train_time: 161.528955, val_time: 30.976201
    Epoch: 15, Loss: 0.4078, auc: 0.8881, best_auc: 0.8881, train_time: 161.879133, val_time: 30.218559
    Epoch: 16, Loss: 0.4055, auc: 0.8895, best_auc: 0.8895, train_time: 164.364600, val_time: 29.192315
    Epoch: 17, Loss: 0.4031, auc: 0.8905, best_auc: 0.8905, train_time: 165.300766, val_time: 29.872147
    Epoch: 18, Loss: 0.4004, auc: 0.8916, best_auc: 0.8916, train_time: 164.017409, val_time: 29.453771
    Epoch: 19, Loss: 0.3978, auc: 0.8926, best_auc: 0.8926, train_time: 163.567369, val_time: 29.415726
    Epoch: 20, Loss: 0.3950, auc: 0.8940, best_auc: 0.8940, train_time: 163.352856, val_time: 29.771705
    Epoch: 21, Loss: 0.3923, auc: 0.8946, best_auc: 0.8946, train_time: 162.627353, val_time: 31.271033
    Epoch: 22, Loss: 0.3899, auc: 0.8952, best_auc: 0.8952, train_time: 164.815891, val_time: 29.754660
    Epoch: 23, Loss: 0.3876, auc: 0.8959, best_auc: 0.8959, train_time: 165.358544, val_time: 29.504207
    Epoch: 24, Loss: 0.3853, auc: 0.8966, best_auc: 0.8966, train_time: 165.513969, val_time: 29.464947
    Epoch: 25, Loss: 0.3835, auc: 0.8972, best_auc: 0.8972, train_time: 165.814146, val_time: 30.295558
    Epoch: 26, Loss: 0.3818, auc: 0.8975, best_auc: 0.8975, train_time: 167.436155, val_time: 29.895903
    Epoch: 27, Loss: 0.3802, auc: 0.8979, best_auc: 0.8979, train_time: 166.109663, val_time: 30.857482
    Epoch: 28, Loss: 0.3787, auc: 0.8983, best_auc: 0.8983, train_time: 165.310546, val_time: 30.359713
    Epoch: 29, Loss: 0.3774, auc: 0.8982, best_auc: 0.8983, train_time: 166.830977, val_time: 29.719225
    Epoch: 30, Loss: 0.3760, auc: 0.8986, best_auc: 0.8986, train_time: 167.059395, val_time: 29.527563
    Epoch: 31, Loss: 0.3748, auc: 0.8989, best_auc: 0.8989, train_time: 166.586335, val_time: 29.707431
    Epoch: 32, Loss: 0.3737, auc: 0.8993, best_auc: 0.8993, train_time: 166.552711, val_time: 29.625352
    Epoch: 33, Loss: 0.3726, auc: 0.8992, best_auc: 0.8993, train_time: 166.088135, val_time: 29.702411
    Epoch: 34, Loss: 0.3717, auc: 0.8997, best_auc: 0.8997, train_time: 165.995186, val_time: 30.493399
    Epoch: 35, Loss: 0.3707, auc: 0.8996, best_auc: 0.8997, train_time: 165.152117, val_time: 30.073559
    Epoch: 36, Loss: 0.3698, auc: 0.8998, best_auc: 0.8998, train_time: 169.682799, val_time: 29.615795
    Epoch: 37, Loss: 0.3690, auc: 0.8996, best_auc: 0.8998, train_time: 170.536779, val_time: 29.416178
    Epoch: 38, Loss: 0.3680, auc: 0.8999, best_auc: 0.8999, train_time: 168.058197, val_time: 29.377550
    Epoch: 39, Loss: 0.3672, auc: 0.8997, best_auc: 0.8999, train_time: 169.529619, val_time: 29.130233
    Epoch: 40, Loss: 0.3662, auc: 0.8999, best_auc: 0.8999, train_time: 168.847006, val_time: 29.983481
    Epoch: 41, Loss: 0.3655, auc: 0.9001, best_auc: 0.9001, train_time: 169.295993, val_time: 29.978450
    Epoch: 42, Loss: 0.3649, auc: 0.9001, best_auc: 0.9001, train_time: 167.329376, val_time: 31.966698
    Epoch: 43, Loss: 0.3639, auc: 0.9001, best_auc: 0.9001, train_time: 168.396654, val_time: 29.749203
    Epoch: 44, Loss: 0.3633, auc: 0.9002, best_auc: 0.9002, train_time: 170.975862, val_time: 29.569853
    Epoch: 45, Loss: 0.3624, auc: 0.9004, best_auc: 0.9004, train_time: 169.205427, val_time: 29.729446
    Epoch: 46, Loss: 0.3616, auc: 0.8997, best_auc: 0.9004, train_time: 171.019204, val_time: 29.698842
    Epoch: 47, Loss: 0.3609, auc: 0.9000, best_auc: 0.9004, train_time: 171.976654, val_time: 29.372388
    Epoch: 48, Loss: 0.3601, auc: 0.9002, best_auc: 0.9004, train_time: 169.632170, val_time: 29.680647
    Epoch: 49, Loss: 0.3594, auc: 0.9001, best_auc: 0.9004, train_time: 168.520956, val_time: 30.555877
    Epoch: 50, Loss: 0.3589, auc: 0.9007, best_auc: 0.9007, train_time: 168.436056, val_time: 30.636245
    Epoch: 51, Loss: 0.3581, auc: 0.9003, best_auc: 0.9007, train_time: 169.124647, val_time: 29.666503
    Epoch: 52, Loss: 0.3574, auc: 0.9000, best_auc: 0.9007, train_time: 169.989911, val_time: 29.669656
    Epoch: 53, Loss: 0.3567, auc: 0.9004, best_auc: 0.9007, train_time: 168.654058, val_time: 29.784604
    Epoch: 54, Loss: 0.3558, auc: 0.9004, best_auc: 0.9007, train_time: 169.397768, val_time: 29.590395
    Epoch: 55, Loss: 0.3552, auc: 0.9003, best_auc: 0.9007, train_time: 170.765589, val_time: 29.993585
    Epoch: 56, Loss: 0.3547, auc: 0.9004, best_auc: 0.9007, train_time: 169.974314, val_time: 29.841710
    Epoch: 57, Loss: 0.3539, auc: 0.9000, best_auc: 0.9007, train_time: 168.883928, val_time: 31.988360
    Epoch: 58, Loss: 0.3532, auc: 0.9001, best_auc: 0.9007, train_time: 168.391557, val_time: 29.544885
    Epoch: 59, Loss: 0.3526, auc: 0.8996, best_auc: 0.9007, train_time: 170.150459, val_time: 29.458804
    Epoch: 60, Loss: 0.3521, auc: 0.8998, best_auc: 0.9007, train_time: 169.494603, val_time: 29.313368
    Epoch: 61, Loss: 0.3513, auc: 0.9007, best_auc: 0.9007, train_time: 169.777705, val_time: 29.245758
    Epoch: 62, Loss: 0.3507, auc: 0.9004, best_auc: 0.9007, train_time: 169.737658, val_time: 29.461710
    Epoch: 63, Loss: 0.3499, auc: 0.9005, best_auc: 0.9007, train_time: 169.630755, val_time: 29.535067
    Epoch: 64, Loss: 0.3494, auc: 0.9004, best_auc: 0.9007, train_time: 169.853990, val_time: 30.675977
    Epoch: 65, Loss: 0.3486, auc: 0.9006, best_auc: 0.9007, train_time: 169.045542, val_time: 30.878547
    Epoch: 66, Loss: 0.3482, auc: 0.9000, best_auc: 0.9007, train_time: 170.849150, val_time: 29.744911
    Epoch: 67, Loss: 0.3474, auc: 0.9001, best_auc: 0.9007, train_time: 169.918812, val_time: 29.106062
    Epoch: 68, Loss: 0.3468, auc: 0.9003, best_auc: 0.9007, train_time: 170.794599, val_time: 29.251811
    Epoch: 69, Loss: 0.3460, auc: 0.8986, best_auc: 0.9007, train_time: 170.607898, val_time: 29.939337
    Epoch: 70, Loss: 0.3456, auc: 0.8996, best_auc: 0.9007, train_time: 170.584409, val_time: 29.585327
    Epoch: 71, Loss: 0.3451, auc: 0.8998, best_auc: 0.9007, train_time: 170.640183, val_time: 29.433573
    Epoch: 72, Loss: 0.3442, auc: 0.8997, best_auc: 0.9007, train_time: 168.514910, val_time: 31.591115
    Epoch: 73, Loss: 0.3438, auc: 0.9004, best_auc: 0.9007, train_time: 167.757844, val_time: 29.518397
    Epoch: 74, Loss: 0.3430, auc: 0.9000, best_auc: 0.9007, train_time: 170.319546, val_time: 29.533094
    Epoch: 75, Loss: 0.3424, auc: 0.8997, best_auc: 0.9007, train_time: 171.458460, val_time: 29.682495
    Epoch: 76, Loss: 0.3417, auc: 0.8996, best_auc: 0.9007, train_time: 171.808766, val_time: 29.077152
    Epoch: 77, Loss: 0.3411, auc: 0.9001, best_auc: 0.9007, train_time: 170.652000, val_time: 29.648805
    Epoch: 78, Loss: 0.3407, auc: 0.8996, best_auc: 0.9007, train_time: 171.098794, val_time: 29.345326
    Epoch: 79, Loss: 0.3399, auc: 0.8996, best_auc: 0.9007, train_time: 170.595881, val_time: 29.329460
    Epoch: 80, Loss: 0.3395, auc: 0.8995, best_auc: 0.9007, train_time: 170.721350, val_time: 31.648123
    Epoch: 81, Loss: 0.3386, auc: 0.9000, best_auc: 0.9007, train_time: 169.058813, val_time: 29.176433
    Epoch: 82, Loss: 0.3381, auc: 0.8996, best_auc: 0.9007, train_time: 171.634301, val_time: 29.578259
    Epoch: 83, Loss: 0.3374, auc: 0.8994, best_auc: 0.9007, train_time: 159.482861, val_time: 26.916368
    Epoch: 84, Loss: 0.3368, auc: 0.8992, best_auc: 0.9007, train_time: 150.208770, val_time: 27.287211
    Epoch: 85, Loss: 0.3361, auc: 0.8984, best_auc: 0.9007, train_time: 149.879132, val_time: 27.009389
    Epoch: 86, Loss: 0.3355, auc: 0.8998, best_auc: 0.9007, train_time: 150.108264, val_time: 27.434287
    Epoch: 87, Loss: 0.3350, auc: 0.8993, best_auc: 0.9007, train_time: 150.009462, val_time: 27.217271
    Epoch: 88, Loss: 0.3345, auc: 0.8993, best_auc: 0.9007, train_time: 150.062833, val_time: 27.456728
    Epoch: 89, Loss: 0.3335, auc: 0.8993, best_auc: 0.9007, train_time: 150.436222, val_time: 27.493321
    Epoch: 90, Loss: 0.3331, auc: 0.8995, best_auc: 0.9007, train_time: 150.305276, val_time: 26.651490
    Epoch: 91, Loss: 0.3323, auc: 0.8995, best_auc: 0.9007, train_time: 150.413634, val_time: 27.071467
    Epoch: 92, Loss: 0.3316, auc: 0.8993, best_auc: 0.9007, train_time: 150.272645, val_time: 26.760915
    Epoch: 93, Loss: 0.3312, auc: 0.8988, best_auc: 0.9007, train_time: 149.787601, val_time: 27.458558
    Epoch: 94, Loss: 0.3305, auc: 0.8991, best_auc: 0.9007, train_time: 150.544199, val_time: 27.191698
    Epoch: 95, Loss: 0.3301, auc: 0.8995, best_auc: 0.9007, train_time: 150.170177, val_time: 26.889699
    Epoch: 96, Loss: 0.3293, auc: 0.8983, best_auc: 0.9007, train_time: 149.967859, val_time: 26.839030
    Epoch: 97, Loss: 0.3286, auc: 0.8990, best_auc: 0.9007, train_time: 150.130142, val_time: 27.058787
    Epoch: 98, Loss: 0.3282, auc: 0.8986, best_auc: 0.9007, train_time: 150.111433, val_time: 27.377137
    Epoch: 99, Loss: 0.3275, auc: 0.8986, best_auc: 0.9007, train_time: 150.115727, val_time: 27.462723
    Epoch: 100, Loss: 0.3270, auc: 0.8991, best_auc: 0.9007, train_time: 149.993774, val_time: 27.180979
```
