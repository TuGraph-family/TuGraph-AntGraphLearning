# PaGNN Demo

> Paper: Inductive Link Prediction with Interactive Structure Learning on Attributed Graph
https://2021.ecmlpkdd.org/wp-content/uploads/2021/07/sub_635.pdf


### 数据下载：
从Facebook: https://docs.google.com/uc?export=download&id=12aJWAGCM4IvdGI2fiydDNyWzViEOLZH8&confirm=t 下载数据，
放到data_process/facebook/raw目录下，切换到data_process目录下执行python data_prepare.py生成点表、边表等
### 数据预处理与子图采样：
运行submit.sh进行数据预处理和spark采样,得到训练集测试集验证集
由于link类算法的样本数量过多，只能在分布式模式（比如yarn）运行，为了方便不想搭建yarn集群的用户，
我们提供了采样好的图样本下载地址为 https://drive.google.com/drive/folders/11VphpI6rgvKzfElF9slp2D9qSJvT-big?usp=sharing ，用户可直接进行模型训练
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
Epoch: 01, Loss: 0.1768, Val_AUC: 0.9854, Test_AUC: 0.9848, Final_AUC: 0.9848, train_time: 332.859940, val_time:
103.673040
Epoch: 02, Loss: 0.1208, Val_AUC: 0.9881, Test_AUC: 0.9871, Final_AUC: 0.9871, train_time: 332.166914, val_time:
103.813720
Epoch: 03, Loss: 0.1129, Val_AUC: 0.9887, Test_AUC: 0.9878, Final_AUC: 0.9878, train_time: 332.317503, val_time:
103.721821
Epoch: 04, Loss: 0.1081, Val_AUC: 0.9891, Test_AUC: 0.9882, Final_AUC: 0.9882, train_time: 331.528258, val_time:
103.604120
Epoch: 05, Loss: 0.1046, Val_AUC: 0.9892, Test_AUC: 0.9884, Final_AUC: 0.9884, train_time: 331.953364, val_time:
103.528278
Epoch: 06, Loss: 0.1013, Val_AUC: 0.9893, Test_AUC: 0.9888, Final_AUC: 0.9888, train_time: 331.546283, val_time:
103.724927
Epoch: 07, Loss: 0.0985, Val_AUC: 0.9894, Test_AUC: 0.9891, Final_AUC: 0.9891, train_time: 332.268523, val_time:
104.034015
Epoch: 08, Loss: 0.0957, Val_AUC: 0.9897, Test_AUC: 0.9892, Final_AUC: 0.9892, train_time: 331.685886, val_time:
103.373338
Epoch: 09, Loss: 0.0935, Val_AUC: 0.9899, Test_AUC: 0.9897, Final_AUC: 0.9897, train_time: 331.892707, val_time:
103.685648
Epoch: 10, Loss: 0.0915, Val_AUC: 0.9900, Test_AUC: 0.9898, Final_AUC: 0.9898, train_time: 331.743336, val_time:
103.700926
Epoch: 11, Loss: 0.0896, Val_AUC: 0.9900, Test_AUC: 0.9898, Final_AUC: 0.9898, train_time: 331.676935, val_time:
103.812883
Epoch: 12, Loss: 0.0878, Val_AUC: 0.9902, Test_AUC: 0.9901, Final_AUC: 0.9901, train_time: 332.450235, val_time:
103.902545
Epoch: 13, Loss: 0.0863, Val_AUC: 0.9905, Test_AUC: 0.9903, Final_AUC: 0.9903, train_time: 331.632258, val_time:
103.710113
Epoch: 14, Loss: 0.0849, Val_AUC: 0.9905, Test_AUC: 0.9902, Final_AUC: 0.9902, train_time: 332.213633, val_time:
103.801291
Epoch: 15, Loss: 0.0839, Val_AUC: 0.9905, Test_AUC: 0.9903, Final_AUC: 0.9903, train_time: 332.242129, val_time:
103.812406
Epoch: 16, Loss: 0.0824, Val_AUC: 0.9903, Test_AUC: 0.9901, Final_AUC: 0.9903, train_time: 331.864525, val_time:
103.792836
Epoch: 17, Loss: 0.0815, Val_AUC: 0.9900, Test_AUC: 0.9900, Final_AUC: 0.9903, train_time: 332.029171, val_time:
103.605284
Epoch: 18, Loss: 0.0805, Val_AUC: 0.9902, Test_AUC: 0.9901, Final_AUC: 0.9903, train_time: 331.698890, val_time:
103.606825
Epoch: 19, Loss: 0.0795, Val_AUC: 0.9901, Test_AUC: 0.9903, Final_AUC: 0.9903, train_time: 331.674817, val_time:
103.653768
Epoch: 20, Loss: 0.0784, Val_AUC: 0.9905, Test_AUC: 0.9903, Final_AUC: 0.9903, train_time: 332.562874, val_time:
103.597441
Epoch: 21, Loss: 0.0780, Val_AUC: 0.9906, Test_AUC: 0.9905, Final_AUC: 0.9905, train_time: 331.998443, val_time:
103.334342
Epoch: 22, Loss: 0.0771, Val_AUC: 0.9902, Test_AUC: 0.9902, Final_AUC: 0.9905, train_time: 332.537310, val_time:
103.472951
Epoch: 23, Loss: 0.0764, Val_AUC: 0.9901, Test_AUC: 0.9904, Final_AUC: 0.9905, train_time: 332.338633, val_time:
103.873981
Epoch: 24, Loss: 0.0753, Val_AUC: 0.9903, Test_AUC: 0.9905, Final_AUC: 0.9905, train_time: 331.526010, val_time:
103.919959
Epoch: 25, Loss: 0.0748, Val_AUC: 0.9903, Test_AUC: 0.9906, Final_AUC: 0.9905, train_time: 331.601013, val_time:
103.640856
```
