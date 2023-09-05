# HeGNN 说明
## Model
    论文：[Cash-out User Detection based on Attributed Heterogeneous Information Network with a Hierarchical Attention Mechanism](https://dl.acm.org/doi/pdf/10.1609/aaai.v33i01.3301946)
    [Loan Default Analysis with Multiplex Graph Learning](https://dl.acm.org/doi/10.1145/3340531.3412724)




## BenchMark

### 数据

下载数据 https://drive.google.com/drive/folders/1koV0rGhj-UXrEMOCZezK1tnwC6zb69uB?usp=sharing ，将node.csv,edge.csv,label.csv文件拷贝到data_process目录下

### 数据预处理与子图采样：
    运行submit.sh进行spark采样,得到训练集测试集验证集
### 效果
python model_hegnn.py

Epoch: 01, Loss: 0.6549, val_micro_f1: 0.3533, test_micro_f1: 0.3812, time_cost:10.1865
(Epoch: 01, best_val_micro_f1: 0.3533, best_test_micro_f1: 0.3812)  <br>
Epoch: 02, Loss: 0.5937, val_micro_f1: 0.8100, test_micro_f1: 0.8640, time_cost:7.7812
(Epoch: 02, best_val_micro_f1: 0.8100, best_test_micro_f1: 0.8640)  <br>
Epoch: 03, Loss: 0.4410, val_micro_f1: 0.8800, test_micro_f1: 0.9087, time_cost:7.8255
(Epoch: 03, best_val_micro_f1: 0.8800, best_test_micro_f1: 0.9087)  <br>
Epoch: 04, Loss: 0.2698, val_micro_f1: 0.8833, test_micro_f1: 0.9134, time_cost:7.9378
(Epoch: 04, best_val_micro_f1: 0.8833, best_test_micro_f1: 0.9134)  <br>
Epoch: 05, Loss: 0.1560, val_micro_f1: 0.8733, test_micro_f1: 0.9181, time_cost:7.8614
(Epoch: 04, best_val_micro_f1: 0.8833, best_test_micro_f1: 0.9134)  <br>
Epoch: 06, Loss: 0.0930, val_micro_f1: 0.8867, test_micro_f1: 0.9181, time_cost:7.7332
(Epoch: 06, best_val_micro_f1: 0.8867, best_test_micro_f1: 0.9181)  <br>
Epoch: 07, Loss: 0.0616, val_micro_f1: 0.8900, test_micro_f1: 0.9144, time_cost:7.7568
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 08, Loss: 0.0374, val_micro_f1: 0.8900, test_micro_f1: 0.9139, time_cost:8.2536
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 09, Loss: 0.0252, val_micro_f1: 0.8733, test_micro_f1: 0.9101, time_cost:7.9447
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 10, Loss: 0.0165, val_micro_f1: 0.8767, test_micro_f1: 0.9115, time_cost:8.0749
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 11, Loss: 0.0139, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:7.9099
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 12, Loss: 0.0110, val_micro_f1: 0.8867, test_micro_f1: 0.9158, time_cost:7.8200
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 13, Loss: 0.0095, val_micro_f1: 0.8733, test_micro_f1: 0.9068, time_cost:7.7258
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 14, Loss: 0.0063, val_micro_f1: 0.8800, test_micro_f1: 0.9087, time_cost:7.6581
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 15, Loss: 0.0048, val_micro_f1: 0.8833, test_micro_f1: 0.9129, time_cost:7.8837
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 16, Loss: 0.0044, val_micro_f1: 0.8867, test_micro_f1: 0.9120, time_cost:7.6761
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 17, Loss: 0.0040, val_micro_f1: 0.8833, test_micro_f1: 0.9129, time_cost:7.8965
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 18, Loss: 0.0036, val_micro_f1: 0.8833, test_micro_f1: 0.9106, time_cost:7.5159
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 19, Loss: 0.0034, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:7.6573
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 20, Loss: 0.0031, val_micro_f1: 0.8800, test_micro_f1: 0.9115, time_cost:7.5441
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 21, Loss: 0.0029, val_micro_f1: 0.8833, test_micro_f1: 0.9106, time_cost:7.8138
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 22, Loss: 0.0028, val_micro_f1: 0.8800, test_micro_f1: 0.9106, time_cost:7.7327
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 23, Loss: 0.0026, val_micro_f1: 0.8833, test_micro_f1: 0.9101, time_cost:7.9395
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 24, Loss: 0.0025, val_micro_f1: 0.8833, test_micro_f1: 0.9101, time_cost:8.1016
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 25, Loss: 0.0023, val_micro_f1: 0.8833, test_micro_f1: 0.9106, time_cost:7.8681
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 26, Loss: 0.0022, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:8.1047
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 27, Loss: 0.0021, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:7.9321
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 28, Loss: 0.0020, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:8.0467
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 29, Loss: 0.0019, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:7.9494
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 30, Loss: 0.0018, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:7.8126
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 31, Loss: 0.0018, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:7.6112
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 32, Loss: 0.0017, val_micro_f1: 0.8833, test_micro_f1: 0.9111, time_cost:7.8890
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 33, Loss: 0.0016, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:7.8514
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 34, Loss: 0.0016, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:7.7725
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 35, Loss: 0.0015, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:8.1664
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 36, Loss: 0.0014, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:8.0800
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 37, Loss: 0.0014, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:8.2466
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 38, Loss: 0.0013, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:7.8962
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 39, Loss: 0.0013, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:8.1028
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 40, Loss: 0.0013, val_micro_f1: 0.8833, test_micro_f1: 0.9120, time_cost:7.9443
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 41, Loss: 0.0012, val_micro_f1: 0.8833, test_micro_f1: 0.9115, time_cost:7.8737
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 42, Loss: 0.0012, val_micro_f1: 0.8833, test_micro_f1: 0.9120, time_cost:7.7033
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 43, Loss: 0.0011, val_micro_f1: 0.8833, test_micro_f1: 0.9120, time_cost:7.7072
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 44, Loss: 0.0011, val_micro_f1: 0.8833, test_micro_f1: 0.9120, time_cost:7.6971
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 45, Loss: 0.0011, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:7.7901
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 46, Loss: 0.0010, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:7.7611
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 47, Loss: 0.0010, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:7.7674
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 48, Loss: 0.0010, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:8.1013
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 49, Loss: 0.0009, val_micro_f1: 0.8800, test_micro_f1: 0.9125, time_cost:8.4407
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 50, Loss: 0.0009, val_micro_f1: 0.8800, test_micro_f1: 0.9125, time_cost:8.0549
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 51, Loss: 0.0009, val_micro_f1: 0.8800, test_micro_f1: 0.9125, time_cost:8.0030
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 52, Loss: 0.0009, val_micro_f1: 0.8800, test_micro_f1: 0.9125, time_cost:7.8666
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 53, Loss: 0.0009, val_micro_f1: 0.8800, test_micro_f1: 0.9125, time_cost:8.1222
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 54, Loss: 0.0008, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:7.8804
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 55, Loss: 0.0008, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:7.9207
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 56, Loss: 0.0008, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:7.6285
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 57, Loss: 0.0008, val_micro_f1: 0.8800, test_micro_f1: 0.9120, time_cost:7.7935
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 58, Loss: 0.0007, val_micro_f1: 0.8800, test_micro_f1: 0.9115, time_cost:7.6493
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 59, Loss: 0.0007, val_micro_f1: 0.8800, test_micro_f1: 0.9111, time_cost:7.7028
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 60, Loss: 0.0007, val_micro_f1: 0.8800, test_micro_f1: 0.9111, time_cost:8.0117
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 61, Loss: 0.0007, val_micro_f1: 0.8800, test_micro_f1: 0.9115, time_cost:7.7716
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 62, Loss: 0.0007, val_micro_f1: 0.8800, test_micro_f1: 0.9106, time_cost:7.6274
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 63, Loss: 0.0007, val_micro_f1: 0.8800, test_micro_f1: 0.9106, time_cost:7.4590
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 64, Loss: 0.0007, val_micro_f1: 0.8800, test_micro_f1: 0.9106, time_cost:7.7638
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 65, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9106, time_cost:7.7291
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 66, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.7218
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 67, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.8032
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 68, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.6212
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 69, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.7812
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 70, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.7904
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 71, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9096, time_cost:7.8707
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 72, Loss: 0.0006, val_micro_f1: 0.8800, test_micro_f1: 0.9096, time_cost:7.7529
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 73, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9096, time_cost:7.8480
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 74, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.7984
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 75, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.6998
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 76, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.7612
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 77, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.8385
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 78, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.8834
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 79, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.8384
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 80, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.8723
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 81, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9101, time_cost:7.8812
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 82, Loss: 0.0005, val_micro_f1: 0.8800, test_micro_f1: 0.9096, time_cost:7.9375
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 83, Loss: 0.0004, val_micro_f1: 0.8800, test_micro_f1: 0.9092, time_cost:7.7673
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 84, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9092, time_cost:7.7935
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 85, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9092, time_cost:7.7314
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 86, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9092, time_cost:7.9969
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 87, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9092, time_cost:7.7097
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 88, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9092, time_cost:7.9380
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 89, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9092, time_cost:7.7121
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 90, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9092, time_cost:7.8339
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 91, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9087, time_cost:7.9747
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 92, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9082, time_cost:7.8892
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 93, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9078, time_cost:7.8809
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 94, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9078, time_cost:7.8685
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 95, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9078, time_cost:7.6439
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 96, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9078, time_cost:7.7733
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 97, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9078, time_cost:7.9232
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 98, Loss: 0.0004, val_micro_f1: 0.8767, test_micro_f1: 0.9078, time_cost:7.4196
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 99, Loss: 0.0003, val_micro_f1: 0.8767, test_micro_f1: 0.9068, time_cost:7.4690
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
Epoch: 100, Loss: 0.0003, val_micro_f1: 0.8767, test_micro_f1: 0.9068, time_cost:7.9536
(Epoch: 07, best_val_micro_f1: 0.8900, best_test_micro_f1: 0.9144)  <br>
sucess
