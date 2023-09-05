# NASA 模型说明

## Model
    论文：[Regularizing graph neural networks via consistency-diversity graph augmentations]<https://ojs.aaai.org/index.php/AAAI/article/view/20307/20066>

```
    @inproceedings{bo2022regularizing,
    title={Regularizing graph neural networks via consistency-diversity graph augmentations},
    author={Bo, Deyu and Hu, BinBin and Wang, Xiao and Zhang, Zhiqiang and Shi, Chuan and Zhou, Jun},
    booktitle={Proceedings of the AAAI Conference on Artificial Intelligence},
    volume={36},
    number={4},
    pages={3913--3921},
    year={2022}
    }
```

## 说明

### 数据预处理
以 `Amazon Photo` 数据集为例子

* 数据下载：
    从 https://github.com/BUPT-GAMMA/NASA/tree/main/dataset 下载photo.npz文件,放到data_process/dataset/目录下
* 数据预处理与子图采样：
    运行submit.sh进行数据预处理和spark采样,得到每条样本的子图
    将得到的csv表 `graph_features.csv`，包含 'seed', 'graph_feature', 'node_id_list', 'label_list', 'flag_list' 三个字段，其中 'flag_list' 字段中以 'train', 'val', 'test' 标记训练集数据、验证集数据和测试集数据。然后，执行下述指令，转换flag_list的格式以便于后续处理：
```
sed -i 's/\btrain\b/0/g' graph_features.csv
sed -i 's/\tval/\t1/g' graph_features.csv
sed -i 's/test/2/g' graph_features.csv
```

### 模型运行
```
python nasa.py
```

## BenchMark
* 效果
  * NASA 未调参，300 epoch, Amazon Photo, F1 ~ 0.92 左右。

* 效率  * NASA
    ```
    Epoch: 01, Loss: 0.7849, train_time: 2.409370, val_time: 0.387727	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.0901
    Epoch: 02, Loss: 1.9168, train_time: 0.463835, val_time: 0.409787	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.1148
    Epoch: 03, Loss: 1.3910, train_time: 0.461324, val_time: 0.389210	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.1148
    Epoch: 04, Loss: 1.3309, train_time: 0.450482, val_time: 0.384466	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.0390
    Epoch: 05, Loss: 1.0456, train_time: 0.454981, val_time: 0.381707	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.1190
    Epoch: 06, Loss: 0.9304, train_time: 0.449232, val_time: 0.385375	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.1196
    Epoch: 07, Loss: 0.7828, train_time: 0.462663, val_time: 0.385535	train_f1: 0.156250, val_f1: 0.1458, test_f1: 0.2350
    Epoch: 08, Loss: 0.7248, train_time: 0.450349, val_time: 0.388324	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2257
    Epoch: 09, Loss: 0.9742, train_time: 0.457985, val_time: 0.388683	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2257
    Epoch: 10, Loss: 0.9428, train_time: 0.446913, val_time: 0.387501	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2261
    Epoch: 11, Loss: 0.7487, train_time: 0.446201, val_time: 0.382103	train_f1: 0.225000, val_f1: 0.2250, test_f1: 0.4423
    Epoch: 12, Loss: 0.6091, train_time: 0.462892, val_time: 0.383698	train_f1: 0.131250, val_f1: 0.1250, test_f1: 0.2610
    Epoch: 13, Loss: 0.5671, train_time: 0.446771, val_time: 0.384869	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2608
    Epoch: 14, Loss: 0.5563, train_time: 0.443244, val_time: 0.380741	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2608
    Epoch: 15, Loss: 0.5612, train_time: 0.447747, val_time: 0.382180	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2611
    Epoch: 16, Loss: 0.5596, train_time: 0.446157, val_time: 0.382596	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2611
    Epoch: 17, Loss: 0.5488, train_time: 0.451101, val_time: 0.379544	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2608
    Epoch: 18, Loss: 0.5509, train_time: 0.441869, val_time: 0.376832	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2608
    Epoch: 19, Loss: 0.5207, train_time: 0.442937, val_time: 0.481601	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2610
    Epoch: 20, Loss: 0.5072, train_time: 0.558334, val_time: 0.377892	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2608
    Epoch: 21, Loss: 0.5019, train_time: 0.467034, val_time: 0.402186	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2608
    Epoch: 22, Loss: 0.4987, train_time: 0.457480, val_time: 0.390974	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2608
    Epoch: 23, Loss: 0.4962, train_time: 0.442622, val_time: 0.386423	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2612
    Epoch: 24, Loss: 0.4734, train_time: 0.462875, val_time: 0.384645	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2612
    Epoch: 25, Loss: 0.4778, train_time: 0.450900, val_time: 0.381567	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2611
    Epoch: 26, Loss: 0.4685, train_time: 0.449544, val_time: 0.380866	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2617
    Epoch: 27, Loss: 0.4658, train_time: 0.449051, val_time: 0.383625	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2621
    Epoch: 28, Loss: 0.4489, train_time: 0.450661, val_time: 0.387177	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2621
    Epoch: 29, Loss: 0.4529, train_time: 0.453003, val_time: 0.389676	train_f1: 0.125000, val_f1: 0.1250, test_f1: 0.2619
    Epoch: 30, Loss: 0.4527, train_time: 0.457725, val_time: 0.390348	train_f1: 0.137500, val_f1: 0.1292, test_f1: 0.2640
    Epoch: 31, Loss: 0.4319, train_time: 0.453672, val_time: 0.401276	train_f1: 0.137500, val_f1: 0.1333, test_f1: 0.2713
    Epoch: 32, Loss: 0.4339, train_time: 0.449958, val_time: 0.387022	train_f1: 0.156250, val_f1: 0.1458, test_f1: 0.2828
    Epoch: 33, Loss: 0.4262, train_time: 0.448586, val_time: 0.400396	train_f1: 0.150000, val_f1: 0.1333, test_f1: 0.2714
    Epoch: 34, Loss: 0.4131, train_time: 0.486054, val_time: 0.419609	train_f1: 0.143750, val_f1: 0.1292, test_f1: 0.2655
    Epoch: 35, Loss: 0.4172, train_time: 0.491612, val_time: 0.427876	train_f1: 0.156250, val_f1: 0.1417, test_f1: 0.2908
    Epoch: 36, Loss: 0.3963, train_time: 0.469271, val_time: 0.386648	train_f1: 0.181250, val_f1: 0.1542, test_f1: 0.2985
    Epoch: 37, Loss: 0.3875, train_time: 0.454999, val_time: 0.384635	train_f1: 0.206250, val_f1: 0.1542, test_f1: 0.3023
    Epoch: 38, Loss: 0.3801, train_time: 0.459137, val_time: 0.384290	train_f1: 0.212500, val_f1: 0.1542, test_f1: 0.3050
    Epoch: 39, Loss: 0.3738, train_time: 0.566178, val_time: 0.411133	train_f1: 0.250000, val_f1: 0.1667, test_f1: 0.3199
    Epoch: 40, Loss: 0.3738, train_time: 0.458307, val_time: 0.374921	train_f1: 0.287500, val_f1: 0.2000, test_f1: 0.3567
    Epoch: 41, Loss: 0.3691, train_time: 0.453641, val_time: 0.378376	train_f1: 0.350000, val_f1: 0.2833, test_f1: 0.3851
    Epoch: 42, Loss: 0.3576, train_time: 0.457642, val_time: 0.379780	train_f1: 0.412500, val_f1: 0.3417, test_f1: 0.4098
    Epoch: 43, Loss: 0.3432, train_time: 0.455808, val_time: 0.383137	train_f1: 0.468750, val_f1: 0.3833, test_f1: 0.4527
    Epoch: 44, Loss: 0.3314, train_time: 0.455915, val_time: 0.382015	train_f1: 0.481250, val_f1: 0.4333, test_f1: 0.4888
    Epoch: 45, Loss: 0.3308, train_time: 0.458668, val_time: 0.377257	train_f1: 0.487500, val_f1: 0.4292, test_f1: 0.4946
    Epoch: 46, Loss: 0.3190, train_time: 0.458364, val_time: 0.376608	train_f1: 0.487500, val_f1: 0.4167, test_f1: 0.4810
    Epoch: 47, Loss: 0.3213, train_time: 0.453923, val_time: 0.382437	train_f1: 0.493750, val_f1: 0.4292, test_f1: 0.4899
    Epoch: 48, Loss: 0.3125, train_time: 0.456032, val_time: 0.376015	train_f1: 0.512500, val_f1: 0.4500, test_f1: 0.5046
    Epoch: 49, Loss: 0.3140, train_time: 0.458869, val_time: 0.374605	train_f1: 0.506250, val_f1: 0.4542, test_f1: 0.5074
    Epoch: 50, Loss: 0.3123, train_time: 0.454955, val_time: 0.375020	train_f1: 0.562500, val_f1: 0.4875, test_f1: 0.5378
    Epoch: 51, Loss: 0.2811, train_time: 0.454685, val_time: 0.380710	train_f1: 0.606250, val_f1: 0.5292, test_f1: 0.5572
    Epoch: 52, Loss: 0.2915, train_time: 0.463989, val_time: 0.375225	train_f1: 0.612500, val_f1: 0.5542, test_f1: 0.5694
    Epoch: 53, Loss: 0.2807, train_time: 0.451378, val_time: 0.371463	train_f1: 0.618750, val_f1: 0.5625, test_f1: 0.5750
    Epoch: 54, Loss: 0.2847, train_time: 0.451757, val_time: 0.375171	train_f1: 0.631250, val_f1: 0.5792, test_f1: 0.5892
    Epoch: 55, Loss: 0.2789, train_time: 0.454146, val_time: 0.379980	train_f1: 0.650000, val_f1: 0.5833, test_f1: 0.6065
    Epoch: 56, Loss: 0.2717, train_time: 0.454165, val_time: 0.373986	train_f1: 0.643750, val_f1: 0.5875, test_f1: 0.6319
    Epoch: 57, Loss: 0.2691, train_time: 0.454717, val_time: 0.373815	train_f1: 0.650000, val_f1: 0.5833, test_f1: 0.6317
    Epoch: 58, Loss: 0.2592, train_time: 0.500981, val_time: 0.374715	train_f1: 0.650000, val_f1: 0.5875, test_f1: 0.6319
    Epoch: 59, Loss: 0.2554, train_time: 0.453532, val_time: 0.374376	train_f1: 0.650000, val_f1: 0.6167, test_f1: 0.6542
    Epoch: 60, Loss: 0.2652, train_time: 0.452625, val_time: 0.375079	train_f1: 0.668750, val_f1: 0.6250, test_f1: 0.6801
    Epoch: 61, Loss: 0.2515, train_time: 0.459745, val_time: 0.379440	train_f1: 0.693750, val_f1: 0.6583, test_f1: 0.7295
    Epoch: 62, Loss: 0.2585, train_time: 0.454022, val_time: 0.375827	train_f1: 0.712500, val_f1: 0.6833, test_f1: 0.7543
    Epoch: 63, Loss: 0.2483, train_time: 0.451904, val_time: 0.379126	train_f1: 0.725000, val_f1: 0.7042, test_f1: 0.7677
    Epoch: 64, Loss: 0.2423, train_time: 0.456659, val_time: 0.382272	train_f1: 0.718750, val_f1: 0.7000, test_f1: 0.7599
    Epoch: 65, Loss: 0.2576, train_time: 0.456727, val_time: 0.375665	train_f1: 0.718750, val_f1: 0.6917, test_f1: 0.7514
    Epoch: 66, Loss: 0.2429, train_time: 0.455046, val_time: 0.383324	train_f1: 0.725000, val_f1: 0.7125, test_f1: 0.7646
    Epoch: 67, Loss: 0.2394, train_time: 0.456770, val_time: 0.377453	train_f1: 0.756250, val_f1: 0.7292, test_f1: 0.7897
    Epoch: 68, Loss: 0.2421, train_time: 0.451637, val_time: 0.371791	train_f1: 0.731250, val_f1: 0.7208, test_f1: 0.7839
    Epoch: 69, Loss: 0.2441, train_time: 0.459214, val_time: 0.409385	train_f1: 0.712500, val_f1: 0.6917, test_f1: 0.7636
    Epoch: 70, Loss: 0.2277, train_time: 0.456779, val_time: 0.374898	train_f1: 0.700000, val_f1: 0.6833, test_f1: 0.7563
    Epoch: 71, Loss: 0.2288, train_time: 0.455686, val_time: 0.382077	train_f1: 0.712500, val_f1: 0.7125, test_f1: 0.7637
    Epoch: 72, Loss: 0.2276, train_time: 0.451888, val_time: 0.373402	train_f1: 0.731250, val_f1: 0.7333, test_f1: 0.7731
    Epoch: 73, Loss: 0.2343, train_time: 0.450120, val_time: 0.378067	train_f1: 0.737500, val_f1: 0.7250, test_f1: 0.7873
    Epoch: 74, Loss: 0.2235, train_time: 0.454213, val_time: 0.376312	train_f1: 0.737500, val_f1: 0.7333, test_f1: 0.7937
    Epoch: 75, Loss: 0.2164, train_time: 0.452079, val_time: 0.383587	train_f1: 0.731250, val_f1: 0.7250, test_f1: 0.7879
    Epoch: 76, Loss: 0.2290, train_time: 0.457238, val_time: 0.390302	train_f1: 0.737500, val_f1: 0.7417, test_f1: 0.7950
    Epoch: 77, Loss: 0.2215, train_time: 0.482541, val_time: 0.384275	train_f1: 0.743750, val_f1: 0.7458, test_f1: 0.7990
    Epoch: 78, Loss: 0.2212, train_time: 0.460082, val_time: 0.388165	train_f1: 0.750000, val_f1: 0.7500, test_f1: 0.7990
    Epoch: 79, Loss: 0.2171, train_time: 0.452800, val_time: 0.394711	train_f1: 0.743750, val_f1: 0.7375, test_f1: 0.7917
    Epoch: 80, Loss: 0.2178, train_time: 0.459813, val_time: 0.405177	train_f1: 0.743750, val_f1: 0.7292, test_f1: 0.7949
    Epoch: 81, Loss: 0.2080, train_time: 0.466736, val_time: 0.394859	train_f1: 0.750000, val_f1: 0.7375, test_f1: 0.7994
    Epoch: 82, Loss: 0.2187, train_time: 0.454182, val_time: 0.410645	train_f1: 0.731250, val_f1: 0.7458, test_f1: 0.8021
    Epoch: 83, Loss: 0.2125, train_time: 0.499220, val_time: 0.414889	train_f1: 0.731250, val_f1: 0.7292, test_f1: 0.7863
    Epoch: 84, Loss: 0.2119, train_time: 0.451876, val_time: 0.395117	train_f1: 0.725000, val_f1: 0.7208, test_f1: 0.7814
    Epoch: 85, Loss: 0.2101, train_time: 0.490785, val_time: 0.434433	train_f1: 0.743750, val_f1: 0.7375, test_f1: 0.7927
    Epoch: 86, Loss: 0.2152, train_time: 0.489830, val_time: 0.401396	train_f1: 0.787500, val_f1: 0.7583, test_f1: 0.8164
    Epoch: 87, Loss: 0.2115, train_time: 0.483145, val_time: 0.403199	train_f1: 0.781250, val_f1: 0.7833, test_f1: 0.8313
    Epoch: 88, Loss: 0.2150, train_time: 0.458329, val_time: 0.393530	train_f1: 0.756250, val_f1: 0.7542, test_f1: 0.8097
    Epoch: 89, Loss: 0.2079, train_time: 0.468917, val_time: 0.403194	train_f1: 0.743750, val_f1: 0.7333, test_f1: 0.7920
    Epoch: 90, Loss: 0.2136, train_time: 0.453062, val_time: 0.392831	train_f1: 0.800000, val_f1: 0.7792, test_f1: 0.8324
    Epoch: 91, Loss: 0.2048, train_time: 0.461432, val_time: 0.387603	train_f1: 0.850000, val_f1: 0.8042, test_f1: 0.8506
    Epoch: 92, Loss: 0.2057, train_time: 0.453992, val_time: 0.397924	train_f1: 0.856250, val_f1: 0.8000, test_f1: 0.8519
    Epoch: 93, Loss: 0.1995, train_time: 0.443586, val_time: 0.384226	train_f1: 0.818750, val_f1: 0.7875, test_f1: 0.8392
    Epoch: 94, Loss: 0.2098, train_time: 0.439300, val_time: 0.389837	train_f1: 0.781250, val_f1: 0.7708, test_f1: 0.8237
    Epoch: 95, Loss: 0.1979, train_time: 0.472037, val_time: 0.393535	train_f1: 0.775000, val_f1: 0.7708, test_f1: 0.8236
    Epoch: 96, Loss: 0.1976, train_time: 0.458223, val_time: 0.402284	train_f1: 0.781250, val_f1: 0.7750, test_f1: 0.8331
    Epoch: 97, Loss: 0.1968, train_time: 0.470268, val_time: 0.398442	train_f1: 0.800000, val_f1: 0.7958, test_f1: 0.8361
    Epoch: 98, Loss: 0.1975, train_time: 0.445167, val_time: 0.391435	train_f1: 0.806250, val_f1: 0.8000, test_f1: 0.8359
    Epoch: 99, Loss: 0.1983, train_time: 0.458230, val_time: 0.385353	train_f1: 0.806250, val_f1: 0.7958, test_f1: 0.8346
    Epoch: 100, Loss: 0.1988, train_time: 0.450563, val_time: 0.386776	train_f1: 0.806250, val_f1: 0.7917, test_f1: 0.8421
    Epoch: 101, Loss: 0.2096, train_time: 0.455066, val_time: 0.392220	train_f1: 0.831250, val_f1: 0.8042, test_f1: 0.8541
    Epoch: 102, Loss: 0.2018, train_time: 0.443387, val_time: 0.394153	train_f1: 0.837500, val_f1: 0.8042, test_f1: 0.8643
    Epoch: 103, Loss: 0.2016, train_time: 0.439255, val_time: 0.387634	train_f1: 0.843750, val_f1: 0.8208, test_f1: 0.8666
    Epoch: 104, Loss: 0.1948, train_time: 0.451777, val_time: 0.386899	train_f1: 0.856250, val_f1: 0.8458, test_f1: 0.8719
    Epoch: 105, Loss: 0.1925, train_time: 0.449302, val_time: 0.389077	train_f1: 0.862500, val_f1: 0.8500, test_f1: 0.8752
    Epoch: 106, Loss: 0.1956, train_time: 0.456920, val_time: 0.393086	train_f1: 0.881250, val_f1: 0.8542, test_f1: 0.8782
    Epoch: 107, Loss: 0.1946, train_time: 0.445179, val_time: 0.389073	train_f1: 0.887500, val_f1: 0.8583, test_f1: 0.8834
    Epoch: 108, Loss: 0.1937, train_time: 0.449708, val_time: 0.388252	train_f1: 0.862500, val_f1: 0.8375, test_f1: 0.8806
    Epoch: 109, Loss: 0.1966, train_time: 0.453591, val_time: 0.392260	train_f1: 0.875000, val_f1: 0.8333, test_f1: 0.8818
    Epoch: 110, Loss: 0.1943, train_time: 0.447050, val_time: 0.393864	train_f1: 0.862500, val_f1: 0.8292, test_f1: 0.8812
    Epoch: 111, Loss: 0.1980, train_time: 0.450165, val_time: 0.394744	train_f1: 0.868750, val_f1: 0.8458, test_f1: 0.8815
    Epoch: 112, Loss: 0.1938, train_time: 0.451525, val_time: 0.395107	train_f1: 0.887500, val_f1: 0.8625, test_f1: 0.8861
    Epoch: 113, Loss: 0.1863, train_time: 0.452203, val_time: 0.397329	train_f1: 0.887500, val_f1: 0.8667, test_f1: 0.8899
    Epoch: 114, Loss: 0.1842, train_time: 0.447603, val_time: 0.393829	train_f1: 0.881250, val_f1: 0.8708, test_f1: 0.8921
    Epoch: 115, Loss: 0.1914, train_time: 0.445139, val_time: 0.392588	train_f1: 0.881250, val_f1: 0.8708, test_f1: 0.8945
    Epoch: 116, Loss: 0.1858, train_time: 0.449721, val_time: 0.394229	train_f1: 0.881250, val_f1: 0.8792, test_f1: 0.8997
    Epoch: 117, Loss: 0.1931, train_time: 0.457180, val_time: 0.387968	train_f1: 0.881250, val_f1: 0.8792, test_f1: 0.9025
    Epoch: 118, Loss: 0.1994, train_time: 0.448175, val_time: 0.396640	train_f1: 0.887500, val_f1: 0.8750, test_f1: 0.9043
    Epoch: 119, Loss: 0.1914, train_time: 0.454013, val_time: 0.405102	train_f1: 0.900000, val_f1: 0.8917, test_f1: 0.9058
    Epoch: 120, Loss: 0.1842, train_time: 0.453593, val_time: 0.393414	train_f1: 0.893750, val_f1: 0.9042, test_f1: 0.9101
    Epoch: 121, Loss: 0.1943, train_time: 0.444421, val_time: 0.392165	train_f1: 0.887500, val_f1: 0.9042, test_f1: 0.9106
    Epoch: 122, Loss: 0.1925, train_time: 0.454191, val_time: 0.390331	train_f1: 0.900000, val_f1: 0.8958, test_f1: 0.9072
    Epoch: 123, Loss: 0.1995, train_time: 0.454651, val_time: 0.390887	train_f1: 0.900000, val_f1: 0.9000, test_f1: 0.9052
    Epoch: 124, Loss: 0.1862, train_time: 0.458681, val_time: 0.404071	train_f1: 0.906250, val_f1: 0.9083, test_f1: 0.9079
    Epoch: 125, Loss: 0.1874, train_time: 0.450586, val_time: 0.394936	train_f1: 0.893750, val_f1: 0.8958, test_f1: 0.9090
    Epoch: 126, Loss: 0.1870, train_time: 0.453429, val_time: 0.391595	train_f1: 0.881250, val_f1: 0.9000, test_f1: 0.9086
    Epoch: 127, Loss: 0.1860, train_time: 0.448238, val_time: 0.393557	train_f1: 0.881250, val_f1: 0.8875, test_f1: 0.9063
    Epoch: 128, Loss: 0.1895, train_time: 0.456745, val_time: 0.393320	train_f1: 0.887500, val_f1: 0.8917, test_f1: 0.9077
    Epoch: 129, Loss: 0.1901, train_time: 0.442648, val_time: 0.390761	train_f1: 0.893750, val_f1: 0.8958, test_f1: 0.9062
    Epoch: 130, Loss: 0.1825, train_time: 0.458853, val_time: 0.408519	train_f1: 0.900000, val_f1: 0.9083, test_f1: 0.9094
    Epoch: 131, Loss: 0.1732, train_time: 0.448754, val_time: 0.391863	train_f1: 0.906250, val_f1: 0.9042, test_f1: 0.9123
    Epoch: 132, Loss: 0.1930, train_time: 0.453109, val_time: 0.391353	train_f1: 0.900000, val_f1: 0.9125, test_f1: 0.9109
    Epoch: 133, Loss: 0.1775, train_time: 0.441247, val_time: 0.392522	train_f1: 0.900000, val_f1: 0.9083, test_f1: 0.9099
    Epoch: 134, Loss: 0.1748, train_time: 0.443449, val_time: 0.385535	train_f1: 0.887500, val_f1: 0.9083, test_f1: 0.9098
    Epoch: 135, Loss: 0.1792, train_time: 0.442838, val_time: 0.385317	train_f1: 0.893750, val_f1: 0.9042, test_f1: 0.9084
    Epoch: 136, Loss: 0.1859, train_time: 0.442256, val_time: 0.385644	train_f1: 0.881250, val_f1: 0.9000, test_f1: 0.9055
    Epoch: 137, Loss: 0.1817, train_time: 0.460867, val_time: 0.385437	train_f1: 0.893750, val_f1: 0.9042, test_f1: 0.9077
    Epoch: 138, Loss: 0.1713, train_time: 0.445950, val_time: 0.384954	train_f1: 0.887500, val_f1: 0.9125, test_f1: 0.9139
    Epoch: 139, Loss: 0.1827, train_time: 0.440833, val_time: 0.388276	train_f1: 0.900000, val_f1: 0.9167, test_f1: 0.9152
    Epoch: 140, Loss: 0.1790, train_time: 0.453825, val_time: 0.390597	train_f1: 0.906250, val_f1: 0.9208, test_f1: 0.9139
    Epoch: 141, Loss: 0.1812, train_time: 0.457757, val_time: 0.384416	train_f1: 0.906250, val_f1: 0.9208, test_f1: 0.9124
    Epoch: 142, Loss: 0.1765, train_time: 0.441343, val_time: 0.397051	train_f1: 0.900000, val_f1: 0.9208, test_f1: 0.9117
    Epoch: 143, Loss: 0.1782, train_time: 0.441256, val_time: 0.388266	train_f1: 0.912500, val_f1: 0.9208, test_f1: 0.9138
    Epoch: 144, Loss: 0.1815, train_time: 0.443241, val_time: 0.390542	train_f1: 0.906250, val_f1: 0.9167, test_f1: 0.9114
    Epoch: 145, Loss: 0.1761, train_time: 0.440742, val_time: 0.393586	train_f1: 0.893750, val_f1: 0.9167, test_f1: 0.9112
    Epoch: 146, Loss: 0.1690, train_time: 0.463384, val_time: 0.391230	train_f1: 0.887500, val_f1: 0.9125, test_f1: 0.9117
    Epoch: 147, Loss: 0.1746, train_time: 0.444694, val_time: 0.383637	train_f1: 0.887500, val_f1: 0.9083, test_f1: 0.9138
    Epoch: 148, Loss: 0.1698, train_time: 0.447705, val_time: 0.386101	train_f1: 0.906250, val_f1: 0.9250, test_f1: 0.9113
    Epoch: 149, Loss: 0.1705, train_time: 0.458272, val_time: 0.383065	train_f1: 0.906250, val_f1: 0.9250, test_f1: 0.9120
    Epoch: 150, Loss: 0.1747, train_time: 0.452806, val_time: 0.383158	train_f1: 0.912500, val_f1: 0.9083, test_f1: 0.9119
    Epoch: 151, Loss: 0.1856, train_time: 0.442148, val_time: 0.391732	train_f1: 0.912500, val_f1: 0.9208, test_f1: 0.9127
    Epoch: 152, Loss: 0.1704, train_time: 0.460733, val_time: 0.391192	train_f1: 0.900000, val_f1: 0.9250, test_f1: 0.9128
    Epoch: 153, Loss: 0.1810, train_time: 0.463810, val_time: 0.395869	train_f1: 0.906250, val_f1: 0.9125, test_f1: 0.9163
    Epoch: 154, Loss: 0.1779, train_time: 0.452375, val_time: 0.392212	train_f1: 0.900000, val_f1: 0.9042, test_f1: 0.9150
    Epoch: 155, Loss: 0.1780, train_time: 0.449106, val_time: 0.397114	train_f1: 0.900000, val_f1: 0.9167, test_f1: 0.9094
    Epoch: 156, Loss: 0.1748, train_time: 0.456670, val_time: 0.393993	train_f1: 0.887500, val_f1: 0.9083, test_f1: 0.9011
    Epoch: 157, Loss: 0.1814, train_time: 0.458913, val_time: 0.390159	train_f1: 0.900000, val_f1: 0.9208, test_f1: 0.9074
    Epoch: 158, Loss: 0.1699, train_time: 0.459164, val_time: 0.391982	train_f1: 0.912500, val_f1: 0.9208, test_f1: 0.9146
    Epoch: 159, Loss: 0.1733, train_time: 0.464545, val_time: 0.391483	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9168
    Epoch: 160, Loss: 0.1764, train_time: 0.446652, val_time: 0.384727	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9119
    Epoch: 161, Loss: 0.1807, train_time: 0.461709, val_time: 0.404569	train_f1: 0.900000, val_f1: 0.9250, test_f1: 0.9097
    Epoch: 162, Loss: 0.1779, train_time: 0.457925, val_time: 0.394702	train_f1: 0.912500, val_f1: 0.9250, test_f1: 0.9153
    Epoch: 163, Loss: 0.1716, train_time: 0.461708, val_time: 0.395214	train_f1: 0.906250, val_f1: 0.9250, test_f1: 0.9164
    Epoch: 164, Loss: 0.1807, train_time: 0.451459, val_time: 0.390888	train_f1: 0.893750, val_f1: 0.9208, test_f1: 0.9103
    Epoch: 165, Loss: 0.1651, train_time: 0.453680, val_time: 0.393294	train_f1: 0.887500, val_f1: 0.9042, test_f1: 0.8989
    Epoch: 166, Loss: 0.1736, train_time: 0.454021, val_time: 0.388737	train_f1: 0.900000, val_f1: 0.9125, test_f1: 0.9025
    Epoch: 167, Loss: 0.1806, train_time: 0.448348, val_time: 0.386748	train_f1: 0.900000, val_f1: 0.9208, test_f1: 0.9149
    Epoch: 168, Loss: 0.1704, train_time: 0.441684, val_time: 0.385814	train_f1: 0.918750, val_f1: 0.9167, test_f1: 0.9157
    Epoch: 169, Loss: 0.1709, train_time: 0.474505, val_time: 0.394620	train_f1: 0.918750, val_f1: 0.9250, test_f1: 0.9114
    Epoch: 170, Loss: 0.1702, train_time: 0.457869, val_time: 0.399448	train_f1: 0.912500, val_f1: 0.9167, test_f1: 0.9054
    Epoch: 171, Loss: 0.1792, train_time: 0.469363, val_time: 0.391629	train_f1: 0.906250, val_f1: 0.9167, test_f1: 0.9131
    Epoch: 172, Loss: 0.1697, train_time: 0.466157, val_time: 0.391788	train_f1: 0.900000, val_f1: 0.9292, test_f1: 0.9167
    Epoch: 173, Loss: 0.1699, train_time: 0.447252, val_time: 0.400941	train_f1: 0.900000, val_f1: 0.9208, test_f1: 0.9172
    Epoch: 174, Loss: 0.1690, train_time: 0.456203, val_time: 0.390292	train_f1: 0.906250, val_f1: 0.9208, test_f1: 0.9141
    Epoch: 175, Loss: 0.1658, train_time: 0.463908, val_time: 0.394521	train_f1: 0.906250, val_f1: 0.9208, test_f1: 0.9117
    Epoch: 176, Loss: 0.1634, train_time: 0.461511, val_time: 0.399145	train_f1: 0.906250, val_f1: 0.9250, test_f1: 0.9130
    Epoch: 177, Loss: 0.1730, train_time: 0.467495, val_time: 0.399415	train_f1: 0.918750, val_f1: 0.9333, test_f1: 0.9156
    Epoch: 178, Loss: 0.1731, train_time: 0.452674, val_time: 0.387287	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9148
    Epoch: 179, Loss: 0.1635, train_time: 0.443344, val_time: 0.389657	train_f1: 0.912500, val_f1: 0.9250, test_f1: 0.9163
    Epoch: 180, Loss: 0.1631, train_time: 0.458120, val_time: 0.392209	train_f1: 0.912500, val_f1: 0.9250, test_f1: 0.9160
    Epoch: 181, Loss: 0.1677, train_time: 0.458378, val_time: 0.406659	train_f1: 0.925000, val_f1: 0.9250, test_f1: 0.9181
    Epoch: 182, Loss: 0.1754, train_time: 0.471653, val_time: 0.396353	train_f1: 0.918750, val_f1: 0.9333, test_f1: 0.9178
    Epoch: 183, Loss: 0.1710, train_time: 0.471277, val_time: 0.400120	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9127
    Epoch: 184, Loss: 0.1673, train_time: 0.460885, val_time: 0.393499	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9135
    Epoch: 185, Loss: 0.1688, train_time: 0.461851, val_time: 0.394523	train_f1: 0.918750, val_f1: 0.9250, test_f1: 0.9152
    Epoch: 186, Loss: 0.1677, train_time: 0.454201, val_time: 0.398099	train_f1: 0.925000, val_f1: 0.9292, test_f1: 0.9175
    Epoch: 187, Loss: 0.1689, train_time: 0.464801, val_time: 0.388531	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9181
    Epoch: 188, Loss: 0.1758, train_time: 0.450999, val_time: 0.400836	train_f1: 0.912500, val_f1: 0.9292, test_f1: 0.9128
    Epoch: 189, Loss: 0.1644, train_time: 0.458503, val_time: 0.401884	train_f1: 0.912500, val_f1: 0.9250, test_f1: 0.9130
    Epoch: 190, Loss: 0.1640, train_time: 0.461937, val_time: 0.391222	train_f1: 0.912500, val_f1: 0.9208, test_f1: 0.9146
    Epoch: 191, Loss: 0.1650, train_time: 0.447526, val_time: 0.402985	train_f1: 0.918750, val_f1: 0.9250, test_f1: 0.9160
    Epoch: 192, Loss: 0.1739, train_time: 0.453922, val_time: 0.397233	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9167
    Epoch: 193, Loss: 0.1608, train_time: 0.455353, val_time: 0.389747	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9172
    Epoch: 194, Loss: 0.1708, train_time: 0.451057, val_time: 0.390553	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9186
    Epoch: 195, Loss: 0.1651, train_time: 0.461481, val_time: 0.405649	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9178
    Epoch: 196, Loss: 0.1746, train_time: 0.474096, val_time: 0.398049	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9142
    Epoch: 197, Loss: 0.1765, train_time: 0.463416, val_time: 0.396694	train_f1: 0.918750, val_f1: 0.9208, test_f1: 0.9109
    Epoch: 198, Loss: 0.1627, train_time: 0.466215, val_time: 0.409122	train_f1: 0.918750, val_f1: 0.9250, test_f1: 0.9143
    Epoch: 199, Loss: 0.1808, train_time: 0.461198, val_time: 0.399740	train_f1: 0.925000, val_f1: 0.9250, test_f1: 0.9177
    Epoch: 200, Loss: 0.1640, train_time: 0.447127, val_time: 0.395700	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9175
    Epoch: 201, Loss: 0.1711, train_time: 0.462343, val_time: 0.392432	train_f1: 0.925000, val_f1: 0.9292, test_f1: 0.9179
    Epoch: 202, Loss: 0.1652, train_time: 0.448992, val_time: 0.395874	train_f1: 0.912500, val_f1: 0.9208, test_f1: 0.9156
    Epoch: 203, Loss: 0.1673, train_time: 0.457323, val_time: 0.402517	train_f1: 0.918750, val_f1: 0.9333, test_f1: 0.9114
    Epoch: 204, Loss: 0.1728, train_time: 0.475780, val_time: 0.397789	train_f1: 0.912500, val_f1: 0.9292, test_f1: 0.9108
    Epoch: 205, Loss: 0.1666, train_time: 0.577320, val_time: 0.396043	train_f1: 0.925000, val_f1: 0.9250, test_f1: 0.9153
    Epoch: 206, Loss: 0.1694, train_time: 0.448983, val_time: 0.394768	train_f1: 0.925000, val_f1: 0.9167, test_f1: 0.9159
    Epoch: 207, Loss: 0.1700, train_time: 0.457281, val_time: 0.398448	train_f1: 0.925000, val_f1: 0.9292, test_f1: 0.9161
    Epoch: 208, Loss: 0.1704, train_time: 0.460470, val_time: 0.397538	train_f1: 0.918750, val_f1: 0.9250, test_f1: 0.9087
    Epoch: 209, Loss: 0.1659, train_time: 0.445487, val_time: 0.409231	train_f1: 0.918750, val_f1: 0.9250, test_f1: 0.9121
    Epoch: 210, Loss: 0.1623, train_time: 0.458501, val_time: 0.438757	train_f1: 0.918750, val_f1: 0.9208, test_f1: 0.9167
    Epoch: 211, Loss: 0.1649, train_time: 0.468280, val_time: 0.401017	train_f1: 0.918750, val_f1: 0.9208, test_f1: 0.9164
    Epoch: 212, Loss: 0.1664, train_time: 0.463500, val_time: 0.397718	train_f1: 0.918750, val_f1: 0.9292, test_f1: 0.9123
    Epoch: 213, Loss: 0.1639, train_time: 0.465236, val_time: 0.397989	train_f1: 0.918750, val_f1: 0.9333, test_f1: 0.9123
    Epoch: 214, Loss: 0.1687, train_time: 0.449840, val_time: 0.397202	train_f1: 0.918750, val_f1: 0.9333, test_f1: 0.9181
    Epoch: 215, Loss: 0.1653, train_time: 0.452932, val_time: 0.396798	train_f1: 0.943750, val_f1: 0.9167, test_f1: 0.9150
    Epoch: 216, Loss: 0.1697, train_time: 0.458518, val_time: 0.392985	train_f1: 0.925000, val_f1: 0.9208, test_f1: 0.9135
    Epoch: 217, Loss: 0.1606, train_time: 0.461026, val_time: 0.395124	train_f1: 0.918750, val_f1: 0.9167, test_f1: 0.9073
    Epoch: 218, Loss: 0.1631, train_time: 0.467454, val_time: 0.393200	train_f1: 0.925000, val_f1: 0.9292, test_f1: 0.9120
    Epoch: 219, Loss: 0.1690, train_time: 0.466931, val_time: 0.401497	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9188
    Epoch: 220, Loss: 0.1655, train_time: 0.465267, val_time: 0.390967	train_f1: 0.931250, val_f1: 0.9250, test_f1: 0.9159
    Epoch: 221, Loss: 0.1717, train_time: 0.461511, val_time: 0.402631	train_f1: 0.918750, val_f1: 0.9208, test_f1: 0.9132
    Epoch: 222, Loss: 0.1703, train_time: 0.463001, val_time: 0.410113	train_f1: 0.925000, val_f1: 0.9208, test_f1: 0.9121
    Epoch: 223, Loss: 0.1641, train_time: 0.464444, val_time: 0.408762	train_f1: 0.925000, val_f1: 0.9167, test_f1: 0.9116
    Epoch: 224, Loss: 0.1609, train_time: 0.480095, val_time: 0.390661	train_f1: 0.925000, val_f1: 0.9250, test_f1: 0.9148
    Epoch: 225, Loss: 0.1594, train_time: 0.471180, val_time: 0.403784	train_f1: 0.912500, val_f1: 0.9208, test_f1: 0.9130
    Epoch: 226, Loss: 0.1701, train_time: 0.457036, val_time: 0.391104	train_f1: 0.925000, val_f1: 0.9208, test_f1: 0.9159
    Epoch: 227, Loss: 0.1686, train_time: 0.457494, val_time: 0.394581	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9199
    Epoch: 228, Loss: 0.1579, train_time: 0.455626, val_time: 0.399623	train_f1: 0.925000, val_f1: 0.9167, test_f1: 0.9172
    Epoch: 229, Loss: 0.1706, train_time: 0.450535, val_time: 0.388104	train_f1: 0.918750, val_f1: 0.9375, test_f1: 0.9130
    Epoch: 230, Loss: 0.1676, train_time: 0.463952, val_time: 0.401608	train_f1: 0.912500, val_f1: 0.9250, test_f1: 0.9117
    Epoch: 231, Loss: 0.1706, train_time: 0.464486, val_time: 0.399394	train_f1: 0.925000, val_f1: 0.9208, test_f1: 0.9166
    Epoch: 232, Loss: 0.1583, train_time: 0.469163, val_time: 0.407946	train_f1: 0.937500, val_f1: 0.9292, test_f1: 0.9186
    Epoch: 233, Loss: 0.1569, train_time: 0.477242, val_time: 0.397354	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9167
    Epoch: 234, Loss: 0.1691, train_time: 0.472784, val_time: 0.401857	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9119
    Epoch: 235, Loss: 0.1595, train_time: 0.479847, val_time: 0.411346	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9156
    Epoch: 236, Loss: 0.1631, train_time: 0.489919, val_time: 0.397272	train_f1: 0.925000, val_f1: 0.9208, test_f1: 0.9171
    Epoch: 237, Loss: 0.1681, train_time: 0.465526, val_time: 0.405837	train_f1: 0.925000, val_f1: 0.9125, test_f1: 0.9159
    Epoch: 238, Loss: 0.1655, train_time: 0.457426, val_time: 0.401923	train_f1: 0.925000, val_f1: 0.9292, test_f1: 0.9166
    Epoch: 239, Loss: 0.1615, train_time: 0.460302, val_time: 0.412083	train_f1: 0.925000, val_f1: 0.9375, test_f1: 0.9141
    Epoch: 240, Loss: 0.1624, train_time: 0.455020, val_time: 0.402286	train_f1: 0.925000, val_f1: 0.9167, test_f1: 0.9174
    Epoch: 241, Loss: 0.1631, train_time: 0.460418, val_time: 0.406516	train_f1: 0.931250, val_f1: 0.9250, test_f1: 0.9094
    Epoch: 242, Loss: 0.1582, train_time: 0.470588, val_time: 0.397048	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9137
    Epoch: 243, Loss: 0.1585, train_time: 0.454023, val_time: 0.401365	train_f1: 0.937500, val_f1: 0.9333, test_f1: 0.9203
    Epoch: 244, Loss: 0.1588, train_time: 0.457538, val_time: 0.421644	train_f1: 0.925000, val_f1: 0.9375, test_f1: 0.9203
    Epoch: 245, Loss: 0.1707, train_time: 0.455922, val_time: 0.404543	train_f1: 0.925000, val_f1: 0.9417, test_f1: 0.9153
    Epoch: 246, Loss: 0.1666, train_time: 0.461825, val_time: 0.403364	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9123
    Epoch: 247, Loss: 0.1643, train_time: 0.452025, val_time: 0.400922	train_f1: 0.931250, val_f1: 0.9292, test_f1: 0.9159
    Epoch: 248, Loss: 0.1585, train_time: 0.455410, val_time: 0.401873	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9179
    Epoch: 249, Loss: 0.1560, train_time: 0.467158, val_time: 0.397890	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9168
    Epoch: 250, Loss: 0.1592, train_time: 0.456920, val_time: 0.396781	train_f1: 0.937500, val_f1: 0.9333, test_f1: 0.9143
    Epoch: 251, Loss: 0.1583, train_time: 0.462412, val_time: 0.400638	train_f1: 0.937500, val_f1: 0.9292, test_f1: 0.9110
    Epoch: 252, Loss: 0.1582, train_time: 0.454033, val_time: 0.415506	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9120
    Epoch: 253, Loss: 0.1674, train_time: 0.450033, val_time: 0.406660	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9186
    Epoch: 254, Loss: 0.1605, train_time: 0.457658, val_time: 0.418767	train_f1: 0.937500, val_f1: 0.9250, test_f1: 0.9193
    Epoch: 255, Loss: 0.1581, train_time: 0.459408, val_time: 0.395032	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9182
    Epoch: 256, Loss: 0.1552, train_time: 0.461952, val_time: 0.395385	train_f1: 0.931250, val_f1: 0.9250, test_f1: 0.9160
    Epoch: 257, Loss: 0.1520, train_time: 0.466273, val_time: 0.402267	train_f1: 0.931250, val_f1: 0.9250, test_f1: 0.9149
    Epoch: 258, Loss: 0.1639, train_time: 0.472842, val_time: 0.402324	train_f1: 0.925000, val_f1: 0.9292, test_f1: 0.9143
    Epoch: 259, Loss: 0.1655, train_time: 0.467757, val_time: 0.405136	train_f1: 0.937500, val_f1: 0.9250, test_f1: 0.9154
    Epoch: 260, Loss: 0.1515, train_time: 0.452410, val_time: 0.391822	train_f1: 0.937500, val_f1: 0.9292, test_f1: 0.9167
    Epoch: 261, Loss: 0.1589, train_time: 0.461282, val_time: 0.389266	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9167
    Epoch: 262, Loss: 0.1640, train_time: 0.451252, val_time: 0.392045	train_f1: 0.925000, val_f1: 0.9250, test_f1: 0.9116
    Epoch: 263, Loss: 0.1592, train_time: 0.463023, val_time: 0.391295	train_f1: 0.918750, val_f1: 0.9208, test_f1: 0.9117
    Epoch: 264, Loss: 0.1638, train_time: 0.463214, val_time: 0.395226	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9186
    Epoch: 265, Loss: 0.1581, train_time: 0.471109, val_time: 0.393808	train_f1: 0.937500, val_f1: 0.9292, test_f1: 0.9175
    Epoch: 266, Loss: 0.1588, train_time: 0.467802, val_time: 0.396646	train_f1: 0.931250, val_f1: 0.9292, test_f1: 0.9139
    Epoch: 267, Loss: 0.1590, train_time: 0.459547, val_time: 0.391811	train_f1: 0.937500, val_f1: 0.9208, test_f1: 0.9108
    Epoch: 268, Loss: 0.1716, train_time: 0.459391, val_time: 0.396645	train_f1: 0.931250, val_f1: 0.9208, test_f1: 0.9130
    Epoch: 269, Loss: 0.1615, train_time: 0.468447, val_time: 0.389689	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9163
    Epoch: 270, Loss: 0.1581, train_time: 0.472106, val_time: 0.399589	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9193
    Epoch: 271, Loss: 0.1576, train_time: 0.471457, val_time: 0.399265	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9183
    Epoch: 272, Loss: 0.1539, train_time: 0.454661, val_time: 0.392248	train_f1: 0.937500, val_f1: 0.9333, test_f1: 0.9142
    Epoch: 273, Loss: 0.1513, train_time: 0.451976, val_time: 0.389326	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9149
    Epoch: 274, Loss: 0.1585, train_time: 0.451466, val_time: 0.386282	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9181
    Epoch: 275, Loss: 0.1636, train_time: 0.443387, val_time: 0.386783	train_f1: 0.937500, val_f1: 0.9292, test_f1: 0.9201
    Epoch: 276, Loss: 0.1533, train_time: 0.451974, val_time: 0.389416	train_f1: 0.925000, val_f1: 0.9333, test_f1: 0.9131
    Epoch: 277, Loss: 0.1577, train_time: 0.461919, val_time: 0.387502	train_f1: 0.925000, val_f1: 0.9375, test_f1: 0.9127
    Epoch: 278, Loss: 0.1615, train_time: 0.438472, val_time: 0.393333	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9159
    Epoch: 279, Loss: 0.1615, train_time: 0.444278, val_time: 0.390064	train_f1: 0.937500, val_f1: 0.9333, test_f1: 0.9156
    Epoch: 280, Loss: 0.1543, train_time: 0.458565, val_time: 0.420598	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9135
    Epoch: 281, Loss: 0.1566, train_time: 0.452781, val_time: 0.396424	train_f1: 0.931250, val_f1: 0.9292, test_f1: 0.9099
    Epoch: 282, Loss: 0.1554, train_time: 0.448304, val_time: 0.395016	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9124
    Epoch: 283, Loss: 0.1640, train_time: 0.458192, val_time: 0.390087	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9156
    Epoch: 284, Loss: 0.1553, train_time: 0.440214, val_time: 0.383308	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9167
    Epoch: 285, Loss: 0.1521, train_time: 0.448102, val_time: 0.397962	train_f1: 0.931250, val_f1: 0.9417, test_f1: 0.9164
    Epoch: 286, Loss: 0.1460, train_time: 0.453427, val_time: 0.399234	train_f1: 0.931250, val_f1: 0.9417, test_f1: 0.9132
    Epoch: 287, Loss: 0.1506, train_time: 0.446965, val_time: 0.386775	train_f1: 0.931250, val_f1: 0.9208, test_f1: 0.9138
    Epoch: 288, Loss: 0.1559, train_time: 0.443628, val_time: 0.397555	train_f1: 0.937500, val_f1: 0.9292, test_f1: 0.9139
    Epoch: 289, Loss: 0.1589, train_time: 0.466519, val_time: 0.389501	train_f1: 0.931250, val_f1: 0.9333, test_f1: 0.9149
    Epoch: 290, Loss: 0.1534, train_time: 0.441172, val_time: 0.388616	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9174
    Epoch: 291, Loss: 0.1547, train_time: 0.447603, val_time: 0.392236	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9171
    Epoch: 292, Loss: 0.1516, train_time: 0.445864, val_time: 0.393170	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9168
    Epoch: 293, Loss: 0.1585, train_time: 0.438612, val_time: 0.390646	train_f1: 0.943750, val_f1: 0.9292, test_f1: 0.9138
    Epoch: 294, Loss: 0.1536, train_time: 0.445481, val_time: 0.400595	train_f1: 0.943750, val_f1: 0.9333, test_f1: 0.9120
    Epoch: 295, Loss: 0.1552, train_time: 0.468782, val_time: 0.403011	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9142
    Epoch: 296, Loss: 0.1539, train_time: 0.456322, val_time: 0.387627	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9183
    Epoch: 297, Loss: 0.1518, train_time: 0.464343, val_time: 0.404478	train_f1: 0.931250, val_f1: 0.9375, test_f1: 0.9175
    Epoch: 298, Loss: 0.1562, train_time: 0.451021, val_time: 0.390593	train_f1: 0.937500, val_f1: 0.9333, test_f1: 0.9097
    Epoch: 299, Loss: 0.1632, train_time: 0.462334, val_time: 0.386922	train_f1: 0.937500, val_f1: 0.9375, test_f1: 0.9182
    Epoch: 300, Loss: 0.1505, train_time: 0.447994, val_time: 0.404172	train_f1: 0.943750, val_f1: 0.9250, test_f1: 0.9174
```
