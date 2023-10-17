# PaGNN Demo

> Paper: Inductive Link Prediction with Interactive Structure Learning on Attributed Graph
https://2021.ecmlpkdd.org/wp-content/uploads/2021/07/sub_635.pdf

### 数据下载：

从Facebook: https://docs.google.com/uc?export=download&id=12aJWAGCM4IvdGI2fiydDNyWzViEOLZH8&confirm=t 下载数据，
放到data_process/facebook/raw目录下，切换到data_process目录下执行python data_prepare.py生成点表、边表等

### 数据预处理与子图采样：

运行submit.sh进行数据预处理和spark采样,得到训练集测试集验证集
由于link模式的样本量巨大，用户需要100G内存的机器运行spark任务，用户需要修改[start_docker_with_image.sh](../../../../docker/start_docker_with_image.sh)给虚拟机分配100G内存，同时修改[run_spark_template.sh](../run_spark_template.sh)配置spark.executor.memory=90g和spark.driver.memory=90g。
对于缺少资源的用户,我们提供了采样好的图样本下载地址为 https://drive.google.com/drive/folders/11VphpI6rgvKzfElF9slp2D9qSJvT-big?usp=sharing ，用户可直接进行模型训练

### 模型训练

python pagnn_facebook.py

### Model:

PaGNN as a path aware model includes two main parts: broadcast and aggregation. The broadcast operation is to propagate
the information of source nodes to the subgraph, and target nodes will perceive which nodes are propagated to and model
the structural information of the subgraph during aggregation(i.e. paths, common neighbors).

# Benchmark

```
Facebook AUC~99.0%
Train 75%, Val 5%, Test 20%, embedding_size = 32, n_hops=2
Logs:
Epoch: 01, Loss: 0.6008, Val_AUC: 0.9758, Test_AUC: 0.9721, Final_AUC: 0.9721, train_time: 54.455296, val_time: 15.442602
Epoch: 02, Loss: 0.1598, Val_AUC: 0.9820, Test_AUC: 0.9799, Final_AUC: 0.9799, train_time: 53.828139, val_time: 15.416461
Epoch: 03, Loss: 0.1327, Val_AUC: 0.9842, Test_AUC: 0.9825, Final_AUC: 0.9825, train_time: 53.804774, val_time: 15.365456
Epoch: 04, Loss: 0.1275, Val_AUC: 0.9854, Test_AUC: 0.9840, Final_AUC: 0.9840, train_time: 53.720645, val_time: 15.473708
Epoch: 05, Loss: 0.1242, Val_AUC: 0.9861, Test_AUC: 0.9850, Final_AUC: 0.9850, train_time: 53.892405, val_time: 15.420357
Epoch: 06, Loss: 0.1212, Val_AUC: 0.9869, Test_AUC: 0.9859, Final_AUC: 0.9859, train_time: 53.875890, val_time: 15.412267
Epoch: 07, Loss: 0.1176, Val_AUC: 0.9878, Test_AUC: 0.9867, Final_AUC: 0.9867, train_time: 54.016955, val_time: 15.453273
Epoch: 08, Loss: 0.1143, Val_AUC: 0.9889, Test_AUC: 0.9875, Final_AUC: 0.9875, train_time: 54.120776, val_time: 15.488316
Epoch: 09, Loss: 0.1115, Val_AUC: 0.9898, Test_AUC: 0.9882, Final_AUC: 0.9882, train_time: 53.996052, val_time: 15.438718
Epoch: 10, Loss: 0.1081, Val_AUC: 0.9902, Test_AUC: 0.9887, Final_AUC: 0.9887, train_time: 54.110107, val_time: 15.469001
Epoch: 11, Loss: 0.1051, Val_AUC: 0.9904, Test_AUC: 0.9891, Final_AUC: 0.9891, train_time: 53.891554, val_time: 15.411663
Epoch: 12, Loss: 0.1028, Val_AUC: 0.9905, Test_AUC: 0.9894, Final_AUC: 0.9894, train_time: 54.181941, val_time: 15.566764
Epoch: 13, Loss: 0.1004, Val_AUC: 0.9904, Test_AUC: 0.9897, Final_AUC: 0.9894, train_time: 54.082941, val_time: 15.499990
Epoch: 14, Loss: 0.0995, Val_AUC: 0.9904, Test_AUC: 0.9899, Final_AUC: 0.9894, train_time: 54.066227, val_time: 15.449839
Epoch: 15, Loss: 0.0971, Val_AUC: 0.9903, Test_AUC: 0.9900, Final_AUC: 0.9894, train_time: 54.071145, val_time: 15.400676
Epoch: 16, Loss: 0.0959, Val_AUC: 0.9903, Test_AUC: 0.9902, Final_AUC: 0.9894, train_time: 53.951882, val_time: 15.406671
Epoch: 17, Loss: 0.0949, Val_AUC: 0.9903, Test_AUC: 0.9903, Final_AUC: 0.9894, train_time: 54.140441, val_time: 15.415422
Epoch: 18, Loss: 0.0934, Val_AUC: 0.9904, Test_AUC: 0.9904, Final_AUC: 0.9894, train_time: 54.069685, val_time: 15.417865
Epoch: 19, Loss: 0.0930, Val_AUC: 0.9903, Test_AUC: 0.9905, Final_AUC: 0.9894, train_time: 53.853523, val_time: 15.468146
Epoch: 20, Loss: 0.0919, Val_AUC: 0.9903, Test_AUC: 0.9906, Final_AUC: 0.9894, train_time: 54.061703, val_time: 15.407650
Epoch: 21, Loss: 0.0909, Val_AUC: 0.9903, Test_AUC: 0.9906, Final_AUC: 0.9894, train_time: 54.112284, val_time: 15.446209
Epoch: 22, Loss: 0.0905, Val_AUC: 0.9905, Test_AUC: 0.9907, Final_AUC: 0.9894, train_time: 53.932761, val_time: 15.416759
Epoch: 23, Loss: 0.0900, Val_AUC: 0.9903, Test_AUC: 0.9907, Final_AUC: 0.9894, train_time: 54.074390, val_time: 15.418561
Epoch: 24, Loss: 0.0887, Val_AUC: 0.9904, Test_AUC: 0.9908, Final_AUC: 0.9894, train_time: 54.047718, val_time: 15.405108
Epoch: 25, Loss: 0.0881, Val_AUC: 0.9904, Test_AUC: 0.9908, Final_AUC: 0.9894, train_time: 54.033102, val_time: 15.422883
Epoch: 26, Loss: 0.0878, Val_AUC: 0.9904, Test_AUC: 0.9909, Final_AUC: 0.9894, train_time: 54.084948, val_time: 15.377341
Epoch: 27, Loss: 0.0875, Val_AUC: 0.9904, Test_AUC: 0.9908, Final_AUC: 0.9894, train_time: 54.099622, val_time: 15.481835
Epoch: 28, Loss: 0.0869, Val_AUC: 0.9905, Test_AUC: 0.9909, Final_AUC: 0.9894, train_time: 53.966958, val_time: 15.428368
Epoch: 29, Loss: 0.0863, Val_AUC: 0.9905, Test_AUC: 0.9909, Final_AUC: 0.9909, train_time: 53.927024, val_time: 15.490171
Epoch: 30, Loss: 0.0862, Val_AUC: 0.9905, Test_AUC: 0.9909, Final_AUC: 0.9909, train_time: 54.140018, val_time: 15.314639
Epoch: 31, Loss: 0.0854, Val_AUC: 0.9905, Test_AUC: 0.9910, Final_AUC: 0.9909, train_time: 53.918612, val_time: 15.324767
Epoch: 32, Loss: 0.0852, Val_AUC: 0.9905, Test_AUC: 0.9910, Final_AUC: 0.9909, train_time: 53.889411, val_time: 15.434077
Epoch: 33, Loss: 0.0845, Val_AUC: 0.9905, Test_AUC: 0.9910, Final_AUC: 0.9909, train_time: 53.829149, val_time: 15.400292
Epoch: 34, Loss: 0.0839, Val_AUC: 0.9905, Test_AUC: 0.9910, Final_AUC: 0.9909, train_time: 53.841129, val_time: 15.455966
Epoch: 35, Loss: 0.0839, Val_AUC: 0.9904, Test_AUC: 0.9909, Final_AUC: 0.9909, train_time: 53.862776, val_time: 15.447779
Epoch: 36, Loss: 0.0834, Val_AUC: 0.9907, Test_AUC: 0.9910, Final_AUC: 0.9910, train_time: 57.199176, val_time: 15.437118
Epoch: 37, Loss: 0.0833, Val_AUC: 0.9907, Test_AUC: 0.9911, Final_AUC: 0.9910, train_time: 53.787138, val_time: 15.438943
Epoch: 38, Loss: 0.0828, Val_AUC: 0.9906, Test_AUC: 0.9910, Final_AUC: 0.9910, train_time: 54.064451, val_time: 15.332812
Epoch: 39, Loss: 0.0820, Val_AUC: 0.9906, Test_AUC: 0.9910, Final_AUC: 0.9910, train_time: 54.037931, val_time: 15.449846
Epoch: 40, Loss: 0.0828, Val_AUC: 0.9906, Test_AUC: 0.9910, Final_AUC: 0.9910, train_time: 53.983699, val_time: 15.404096
Epoch: 41, Loss: 0.0815, Val_AUC: 0.9906, Test_AUC: 0.9911, Final_AUC: 0.9910, train_time: 53.945185, val_time: 15.474371
Epoch: 42, Loss: 0.0809, Val_AUC: 0.9906, Test_AUC: 0.9911, Final_AUC: 0.9910, train_time: 53.962729, val_time: 15.515708
Epoch: 43, Loss: 0.0807, Val_AUC: 0.9907, Test_AUC: 0.9911, Final_AUC: 0.9910, train_time: 54.179840, val_time: 15.359091
Epoch: 44, Loss: 0.0810, Val_AUC: 0.9907, Test_AUC: 0.9911, Final_AUC: 0.9910, train_time: 53.889727, val_time: 15.378546
Epoch: 45, Loss: 0.0805, Val_AUC: 0.9907, Test_AUC: 0.9911, Final_AUC: 0.9910, train_time: 53.974539, val_time: 15.396711
Epoch: 46, Loss: 0.0795, Val_AUC: 0.9905, Test_AUC: 0.9910, Final_AUC: 0.9910, train_time: 54.024551, val_time: 15.427157
```
