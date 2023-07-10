# Geniepath Demo 说明
## Model
    论文：[GeniePath: Graph Neural Networks with Adaptive Receptive Paths](https://arxiv.org/abs/1802.00910)
    实现参考：[1] <https://github.com/shuowang-ai/GeniePath-pytorch>
            [2] <https://github.com/pyg-team/pytorch_geometric/blob/master/examples/geniepath.py>

## 说明
这个样例中包含三种 geniepath 的模式
* geniepath_subgraph_adj.py 
  * 每条样本（pb string）包含一个ppi的小 和 多个 root节点。 label字段按照 root 节点在样本中的排布顺序进行排布。
  * dataloader 输出的是 TorchSubGraphBatchData 对象，无论有多少跳，只用一个adj进行信息传递
* geniepath_subgraph_ego.py
  * 样本和前面的一致
  * dataloader 输出的是 TorchEgoBatchData 对象。随着模型迭代进行，逐渐缩小adj的范围，减少计算量
* geniepath_ego.py
  * 样本和前面的有较大差别。每条样本只包含一个root节点，以及这个root节点出发的若干跳邻居。是为了适应工业级大图的方案。
  * dataloader 输出的是 TorchEgoBatchData 对象。随着模型迭代进行，逐渐缩小adj的范围，减少计算量

## BenchMark
* 效果
  * geniepath_subgraph_adj.py (默认 geniepath-lazy)未调参，未加l2. 100 epoch ~ 0.965 左右
  * geniepath_subgraph_ego.py  (默认 geniepath-lazy)未调参，未加l2. 100 epoch ~ 0.965 左右; 200 epoch ~ 0.972
  * geniepath_ego.py  2-hop neighbor limit 50，50。 ~ 0.95 （速度比较慢，一个epoch 22s+）
* 效率：
  * geniepath_adj.py
  
    Epoch: 01, Loss: 0.5855, micro_f1: 0.4329, train_time: 3.610520, val_time: 0.396839 <br>
    Epoch: 02, Loss: 0.5349, micro_f1: 0.4927, train_time: 0.576704, val_time: 0.281550 <br>
    Epoch: 03, Loss: 0.5200, micro_f1: 0.4948, train_time: 0.570703, val_time: 0.264501 <br>
    Epoch: 04, Loss: 0.5127, micro_f1: 0.5230, train_time: 0.588028, val_time: 0.245312 <br>
    Epoch: 05, Loss: 0.5012, micro_f1: 0.5110, train_time: 0.563286, val_time: 0.251925 <br>
    Epoch: 06, Loss: 0.4906, micro_f1: 0.5249, train_time: 0.575678, val_time: 0.240704 <br>
    Epoch: 07, Loss: 0.4767, micro_f1: 0.5475, train_time: 0.568387, val_time: 0.245497 <br>
    Epoch: 08, Loss: 0.4614, micro_f1: 0.5623, train_time: 0.564679, val_time: 0.247245 <br>
    Epoch: 09, Loss: 0.4441, micro_f1: 0.5971, train_time: 0.559996, val_time: 0.248082 <br>
    Epoch: 10, Loss: 0.4236, micro_f1: 0.6162, train_time: 0.579896, val_time: 0.233580 <br>
    Epoch: 11, Loss: 0.4051, micro_f1: 0.6375, train_time: 0.569947, val_time: 0.239823 <br>
    Epoch: 12, Loss: 0.3845, micro_f1: 0.6708, train_time: 0.573948, val_time: 0.242186 <br>
    Epoch: 13, Loss: 0.3629, micro_f1: 0.7081, train_time: 0.585297, val_time: 0.260506 <br>
    Epoch: 14, Loss: 0.3394, micro_f1: 0.7297, train_time: 0.568208, val_time: 0.269986 <br>
    Epoch: 15, Loss: 0.3182, micro_f1: 0.7565, train_time: 0.564630, val_time: 0.245954 <br>
    Epoch: 16, Loss: 0.2977, micro_f1: 0.7790, train_time: 0.592652, val_time: 0.263107 <br>
    Epoch: 17, Loss: 0.2792, micro_f1: 0.7916, train_time: 0.566979, val_time: 0.262001 <br>
    Epoch: 18, Loss: 0.2637, micro_f1: 0.8120, train_time: 0.588144, val_time: 0.268683 <br>
    Epoch: 19, Loss: 0.2464, micro_f1: 0.8303, train_time: 0.584625, val_time: 0.269440 <br>
    Epoch: 20, Loss: 0.2291, micro_f1: 0.8437, train_time: 0.570239, val_time: 0.270055 <br>
    Epoch: 21, Loss: 0.2165, micro_f1: 0.8544, train_time: 0.567580, val_time: 0.253984 <br>
    Epoch: 22, Loss: 0.2053, micro_f1: 0.8606, train_time: 0.583632, val_time: 0.256629 <br>
    Epoch: 23, Loss: 0.1965, micro_f1: 0.8642, train_time: 0.574087, val_time: 0.273745 <br>
    Epoch: 24, Loss: 0.1914, micro_f1: 0.8577, train_time: 0.566030, val_time: 0.286784 <br>
    Epoch: 25, Loss: 0.1885, micro_f1: 0.8763, train_time: 0.578397, val_time: 0.268414 <br>
    Epoch: 26, Loss: 0.1793, micro_f1: 0.8813, train_time: 0.576509, val_time: 0.251609 <br>
    Epoch: 27, Loss: 0.1669, micro_f1: 0.8865, train_time: 0.573591, val_time: 0.247459 <br>
    Epoch: 28, Loss: 0.1560, micro_f1: 0.9016, train_time: 0.568200, val_time: 0.267399 <br>
    Epoch: 29, Loss: 0.1464, micro_f1: 0.9083, train_time: 0.578524, val_time: 0.272283 <br>
    Epoch: 30, Loss: 0.1365, micro_f1: 0.9138, train_time: 0.572161, val_time: 0.272783 <br>
    Epoch: 31, Loss: 0.1291, micro_f1: 0.9191, train_time: 0.566223, val_time: 0.250995 <br>
    Epoch: 32, Loss: 0.1224, micro_f1: 0.9249, train_time: 0.565926, val_time: 0.266821 <br>
    Epoch: 33, Loss: 0.1154, micro_f1: 0.9286, train_time: 0.563264, val_time: 0.254327 <br>
    Epoch: 34, Loss: 0.1105, micro_f1: 0.9296, train_time: 0.591050, val_time: 0.266217 <br>
    Epoch: 35, Loss: 0.1075, micro_f1: 0.9329, train_time: 0.571937, val_time: 0.260360 <br>
    Epoch: 36, Loss: 0.1047, micro_f1: 0.9339, train_time: 0.579550, val_time: 0.249697 <br>
    Epoch: 37, Loss: 0.1020, micro_f1: 0.9359, train_time: 0.576907, val_time: 0.270153 <br>
    Epoch: 38, Loss: 0.1024, micro_f1: 0.9351, train_time: 0.572445, val_time: 0.279754 <br>
    Epoch: 39, Loss: 0.0987, micro_f1: 0.9366, train_time: 0.582170, val_time: 0.273054 <br>
    Epoch: 40, Loss: 0.0966, micro_f1: 0.9398, train_time: 0.580741, val_time: 0.262090 <br>
    Epoch: 41, Loss: 0.0928, micro_f1: 0.9406, train_time: 0.586068, val_time: 0.259312 <br>
    Epoch: 42, Loss: 0.0894, micro_f1: 0.9421, train_time: 0.582386, val_time: 0.253131 <br>
    Epoch: 43, Loss: 0.0873, micro_f1: 0.9443, train_time: 0.579330, val_time: 0.254032 <br>
    Epoch: 44, Loss: 0.0833, micro_f1: 0.9463, train_time: 0.575542, val_time: 0.262460 <br>
    Epoch: 45, Loss: 0.0802, micro_f1: 0.9480, train_time: 0.588365, val_time: 0.249366 <br>
    Epoch: 46, Loss: 0.0799, micro_f1: 0.9452, train_time: 0.575234, val_time: 0.253607 <br>
    Epoch: 47, Loss: 0.0805, micro_f1: 0.9472, train_time: 0.563869, val_time: 0.268178 <br>
    Epoch: 48, Loss: 0.0776, micro_f1: 0.9506, train_time: 0.561639, val_time: 0.263775 <br>
    Epoch: 49, Loss: 0.0735, micro_f1: 0.9520, train_time: 0.588326, val_time: 0.278221 <br>
    Epoch: 50, Loss: 0.0704, micro_f1: 0.9539, train_time: 0.567135, val_time: 0.268161 <br>
    Epoch: 51, Loss: 0.0687, micro_f1: 0.9529, train_time: 0.580380, val_time: 0.267800 <br>
    Epoch: 52, Loss: 0.0678, micro_f1: 0.9545, train_time: 0.567255, val_time: 0.257294 <br>
    Epoch: 53, Loss: 0.0665, micro_f1: 0.9554, train_time: 0.573325, val_time: 0.269401 <br>
    Epoch: 54, Loss: 0.0661, micro_f1: 0.9529, train_time: 0.592860, val_time: 0.267921 <br>
    Epoch: 55, Loss: 0.0738, micro_f1: 0.9490, train_time: 0.562889, val_time: 0.262562 <br>
    Epoch: 56, Loss: 0.0700, micro_f1: 0.9529, train_time: 0.588530, val_time: 0.277734 <br>
    Epoch: 57, Loss: 0.0664, micro_f1: 0.9555, train_time: 0.567806, val_time: 0.271892 <br>
    Epoch: 58, Loss: 0.0635, micro_f1: 0.9563, train_time: 0.574261, val_time: 0.291518 <br>
    Epoch: 59, Loss: 0.0615, micro_f1: 0.9577, train_time: 0.582142, val_time: 0.268996 <br>
    Epoch: 60, Loss: 0.0598, micro_f1: 0.9566, train_time: 0.588323, val_time: 0.254313 <br>
    Epoch: 61, Loss: 0.0612, micro_f1: 0.9551, train_time: 0.574833, val_time: 0.256837 <br>
    Epoch: 62, Loss: 0.0602, micro_f1: 0.9573, train_time: 0.564108, val_time: 0.258551 <br>
    Epoch: 63, Loss: 0.0597, micro_f1: 0.9578, train_time: 0.566397, val_time: 0.258235 <br>
    Epoch: 64, Loss: 0.0591, micro_f1: 0.9590, train_time: 0.571116, val_time: 0.263476 <br>
    Epoch: 65, Loss: 0.0569, micro_f1: 0.9594, train_time: 0.567667, val_time: 0.264978 <br>
    Epoch: 66, Loss: 0.0548, micro_f1: 0.9601, train_time: 0.577469, val_time: 0.255571 <br>
    Epoch: 67, Loss: 0.0529, micro_f1: 0.9604, train_time: 0.589913, val_time: 0.258149 <br>
    Epoch: 68, Loss: 0.0522, micro_f1: 0.9607, train_time: 0.583751, val_time: 0.253471 <br>
    Epoch: 69, Loss: 0.0522, micro_f1: 0.9619, train_time: 0.564875, val_time: 0.262786 <br>
    Epoch: 70, Loss: 0.0526, micro_f1: 0.9605, train_time: 0.579280, val_time: 0.272728 <br>
    Epoch: 71, Loss: 0.0530, micro_f1: 0.9607, train_time: 0.586517, val_time: 0.256806 <br>
    Epoch: 72, Loss: 0.0532, micro_f1: 0.9605, train_time: 0.556420, val_time: 0.271943 <br>
    Epoch: 73, Loss: 0.0528, micro_f1: 0.9600, train_time: 0.565175, val_time: 0.262735 <br>
    Epoch: 74, Loss: 0.0527, micro_f1: 0.9582, train_time: 0.560437, val_time: 0.263096 <br>
    Epoch: 75, Loss: 0.0564, micro_f1: 0.9581, train_time: 0.577128, val_time: 0.267349 <br>
    Epoch: 76, Loss: 0.0544, micro_f1: 0.9602, train_time: 0.593988, val_time: 0.256018 <br>
    Epoch: 77, Loss: 0.0544, micro_f1: 0.9599, train_time: 0.571122, val_time: 0.273547 <br>
    Epoch: 78, Loss: 0.0561, micro_f1: 0.9580, train_time: 0.580809, val_time: 0.258527 <br>
    Epoch: 79, Loss: 0.0529, micro_f1: 0.9594, train_time: 0.585353, val_time: 0.263625 <br>
    Epoch: 80, Loss: 0.0507, micro_f1: 0.9614, train_time: 0.558811, val_time: 0.256316 <br>
    Epoch: 81, Loss: 0.0497, micro_f1: 0.9614, train_time: 0.564778, val_time: 0.254335 <br>
    Epoch: 82, Loss: 0.0486, micro_f1: 0.9622, train_time: 0.580032, val_time: 0.250083 <br>
    Epoch: 83, Loss: 0.0476, micro_f1: 0.9622, train_time: 0.554755, val_time: 0.272314 <br>
    Epoch: 84, Loss: 0.0485, micro_f1: 0.9620, train_time: 0.566637, val_time: 0.263297 <br>
    Epoch: 85, Loss: 0.0476, micro_f1: 0.9627, train_time: 0.586891, val_time: 0.257271 <br>
    Epoch: 86, Loss: 0.0479, micro_f1: 0.9612, train_time: 0.573189, val_time: 0.265699 <br>
    Epoch: 87, Loss: 0.0480, micro_f1: 0.9624, train_time: 0.578523, val_time: 0.257610 <br>
    Epoch: 88, Loss: 0.0465, micro_f1: 0.9627, train_time: 0.573776, val_time: 0.251871 <br>
    Epoch: 89, Loss: 0.0452, micro_f1: 0.9642, train_time: 0.563104, val_time: 0.250803 <br>
    Epoch: 90, Loss: 0.0438, micro_f1: 0.9640, train_time: 0.572833, val_time: 0.249560 <br>
    Epoch: 91, Loss: 0.0454, micro_f1: 0.9630, train_time: 0.572923, val_time: 0.266466 <br>
    Epoch: 92, Loss: 0.0467, micro_f1: 0.9632, train_time: 0.567222, val_time: 0.256081 <br>
    Epoch: 93, Loss: 0.0470, micro_f1: 0.9621, train_time: 0.586775, val_time: 0.248709 <br>
    Epoch: 94, Loss: 0.0449, micro_f1: 0.9648, train_time: 0.570102, val_time: 0.260963 <br>
    Epoch: 95, Loss: 0.0429, micro_f1: 0.9650, train_time: 0.564752, val_time: 0.257342 <br>
    Epoch: 96, Loss: 0.0413, micro_f1: 0.9648, train_time: 0.568025, val_time: 0.258286 <br>
    Epoch: 97, Loss: 0.0411, micro_f1: 0.9650, train_time: 0.571661, val_time: 0.263260 <br>
    Epoch: 98, Loss: 0.0417, micro_f1: 0.9651, train_time: 0.579259, val_time: 0.270057 <br>
    Epoch: 99, Loss: 0.0424, micro_f1: 0.9639, train_time: 0.600746, val_time: 0.260680 <br>
    Epoch: 100, Loss: 0.0454, micro_f1: 0.9637, train_time: 0.598195, val_time: 0.250657 <br>
  
  * new_geniepath_subgraph_ego.py  <br>
    Epoch: 01, Loss: 0.5846, micro_f1: 0.4377, train_time:3.5385, val_time: 0.4400 <br>
    Epoch: 02, Loss: 0.5344, micro_f1: 0.4771, train_time:0.6348, val_time: 0.3261 <br>
    Epoch: 03, Loss: 0.5204, micro_f1: 0.4927, train_time:0.6524, val_time: 0.3275 <br>
    Epoch: 04, Loss: 0.5106, micro_f1: 0.5091, train_time:0.6419, val_time: 0.3163 <br>
    Epoch: 05, Loss: 0.4971, micro_f1: 0.5032, train_time:0.6282, val_time: 0.3106 <br>
    Epoch: 06, Loss: 0.4870, micro_f1: 0.5177, train_time:0.6462, val_time: 0.3199 <br>
    Epoch: 07, Loss: 0.4738, micro_f1: 0.5575, train_time:0.6348, val_time: 0.3323 <br>
    Epoch: 08, Loss: 0.4541, micro_f1: 0.5802, train_time:0.6316, val_time: 0.3253 <br>
    Epoch: 09, Loss: 0.4352, micro_f1: 0.5963, train_time:0.6825, val_time: 0.3083 <br>
    Epoch: 10, Loss: 0.4163, micro_f1: 0.6187, train_time:0.6281, val_time: 0.3075 <br>
    Epoch: 11, Loss: 0.3951, micro_f1: 0.6531, train_time:0.6409, val_time: 0.3141 <br>
    Epoch: 12, Loss: 0.3724, micro_f1: 0.6969, train_time:0.6399, val_time: 0.3251 <br>
    Epoch: 13, Loss: 0.3487, micro_f1: 0.7230, train_time:0.6291, val_time: 0.3452 <br>
    Epoch: 14, Loss: 0.3274, micro_f1: 0.7411, train_time:0.6295, val_time: 0.3448 <br>
    Epoch: 15, Loss: 0.3082, micro_f1: 0.7696, train_time:0.6430, val_time: 0.3357 <br>
    Epoch: 16, Loss: 0.2887, micro_f1: 0.7880, train_time:0.6428, val_time: 0.3334 <br>
    Epoch: 17, Loss: 0.2697, micro_f1: 0.8060, train_time:0.6359, val_time: 0.3373 <br>
    Epoch: 18, Loss: 0.2523, micro_f1: 0.8253, train_time:0.6368, val_time: 0.3472 <br>
    Epoch: 19, Loss: 0.2361, micro_f1: 0.8372, train_time:0.6306, val_time: 0.3250 <br>
    Epoch: 20, Loss: 0.2210, micro_f1: 0.8520, train_time:0.6417, val_time: 0.3223 <br>
    Epoch: 21, Loss: 0.2091, micro_f1: 0.8606, train_time:0.6496, val_time: 0.3312 <br>
    Epoch: 22, Loss: 0.1979, micro_f1: 0.8663, train_time:0.6422, val_time: 0.3301 <br>
    Epoch: 23, Loss: 0.1885, micro_f1: 0.8804, train_time:0.6403, val_time: 0.3230 <br>
    Epoch: 24, Loss: 0.1760, micro_f1: 0.8880, train_time:0.6426, val_time: 0.3217 <br>
    Epoch: 25, Loss: 0.1693, micro_f1: 0.8928, train_time:0.6285, val_time: 0.3251 <br>
    Epoch: 26, Loss: 0.1665, micro_f1: 0.8922, train_time:0.6449, val_time: 0.3243 <br>
    Epoch: 27, Loss: 0.1596, micro_f1: 0.8885, train_time:0.6456, val_time: 0.3303 <br>
    Epoch: 28, Loss: 0.1547, micro_f1: 0.9028, train_time:0.6694, val_time: 0.3255 <br>
    Epoch: 29, Loss: 0.1466, micro_f1: 0.9043, train_time:0.6457, val_time: 0.3205 <br>
    Epoch: 30, Loss: 0.1397, micro_f1: 0.9136, train_time:0.6483, val_time: 0.3297 <br>
    Epoch: 31, Loss: 0.1331, micro_f1: 0.9178, train_time:0.6557, val_time: 0.3380 <br>
    Epoch: 32, Loss: 0.1300, micro_f1: 0.9185, train_time:0.6409, val_time: 0.3250 <br>
    Epoch: 33, Loss: 0.1278, micro_f1: 0.9192, train_time:0.6428, val_time: 0.3390 <br>
    Epoch: 34, Loss: 0.1217, micro_f1: 0.9240, train_time:0.6231, val_time: 0.3363 <br>
    Epoch: 35, Loss: 0.1155, micro_f1: 0.9293, train_time:0.6648, val_time: 0.3308 <br>
    Epoch: 36, Loss: 0.1097, micro_f1: 0.9322, train_time:0.6241, val_time: 0.3221 <br>
    Epoch: 37, Loss: 0.1058, micro_f1: 0.9352, train_time:0.6456, val_time: 0.3328 <br>
    Epoch: 38, Loss: 0.1090, micro_f1: 0.9326, train_time:0.6451, val_time: 0.3218 <br>
    Epoch: 39, Loss: 0.1035, micro_f1: 0.9383, train_time:0.6315, val_time: 0.3236 <br>
    Epoch: 40, Loss: 0.0975, micro_f1: 0.9413, train_time:0.6400, val_time: 0.3233 <br>
    Epoch: 41, Loss: 0.0932, micro_f1: 0.9437, train_time:0.6409, val_time: 0.3411 <br>
    Epoch: 42, Loss: 0.0888, micro_f1: 0.9462, train_time:0.6504, val_time: 0.3251 <br>
    Epoch: 43, Loss: 0.0855, micro_f1: 0.9483, train_time:0.6294, val_time: 0.3344 <br>
    Epoch: 44, Loss: 0.0822, micro_f1: 0.9499, train_time:0.6569, val_time: 0.3238 <br>
    Epoch: 45, Loss: 0.0793, micro_f1: 0.9517, train_time:0.6479, val_time: 0.3357 <br>
    Epoch: 46, Loss: 0.0789, micro_f1: 0.9507, train_time:0.6569, val_time: 0.3719 <br>
    Epoch: 47, Loss: 0.0778, micro_f1: 0.9520, train_time:0.6716, val_time: 0.3372 <br>
    Epoch: 48, Loss: 0.0759, micro_f1: 0.9530, train_time:0.6380, val_time: 0.3363 <br>
    Epoch: 49, Loss: 0.0739, micro_f1: 0.9541, train_time:0.6573, val_time: 0.3311 <br>
    Epoch: 50, Loss: 0.0726, micro_f1: 0.9542, train_time:0.6352, val_time: 0.3274 <br>
    Epoch: 51, Loss: 0.0714, micro_f1: 0.9550, train_time:0.6549, val_time: 0.3253 <br>
    Epoch: 52, Loss: 0.0694, micro_f1: 0.9561, train_time:0.6410, val_time: 0.3381 <br>
    Epoch: 53, Loss: 0.0674, micro_f1: 0.9562, train_time:0.6381, val_time: 0.3164 <br>
    Epoch: 54, Loss: 0.0677, micro_f1: 0.9552, train_time:0.6321, val_time: 0.3197 <br>
    Epoch: 55, Loss: 0.0702, micro_f1: 0.9537, train_time:0.6356, val_time: 0.3411 <br>
    Epoch: 56, Loss: 0.0689, micro_f1: 0.9550, train_time:0.6477, val_time: 0.3185 <br>
    Epoch: 57, Loss: 0.0686, micro_f1: 0.9555, train_time:0.6379, val_time: 0.3157 <br>
    Epoch: 58, Loss: 0.0673, micro_f1: 0.9562, train_time:0.6942, val_time: 0.3629 <br>
    Epoch: 59, Loss: 0.0656, micro_f1: 0.9571, train_time:0.6484, val_time: 0.3490 <br>
    Epoch: 60, Loss: 0.0640, micro_f1: 0.9575, train_time:0.6510, val_time: 0.3527 <br>
    Epoch: 61, Loss: 0.0630, micro_f1: 0.9582, train_time:0.6469, val_time: 0.3270 <br>
    Epoch: 62, Loss: 0.0621, micro_f1: 0.9590, train_time:0.6359, val_time: 0.3290 <br>
    Epoch: 63, Loss: 0.0611, micro_f1: 0.9587, train_time:0.6565, val_time: 0.3540 <br>
    Epoch: 64, Loss: 0.0612, micro_f1: 0.9596, train_time:0.6271, val_time: 0.3443 <br>
    Epoch: 65, Loss: 0.0597, micro_f1: 0.9605, train_time:0.6421, val_time: 0.3379 <br>
    Epoch: 66, Loss: 0.0581, micro_f1: 0.9605, train_time:0.6333, val_time: 0.3232 <br>
    Epoch: 67, Loss: 0.0580, micro_f1: 0.9610, train_time:0.6442, val_time: 0.3224 <br>
    Epoch: 68, Loss: 0.0577, micro_f1: 0.9606, train_time:0.6302, val_time: 0.3336 <br>
    Epoch: 69, Loss: 0.0627, micro_f1: 0.9544, train_time:0.6713, val_time: 0.3357 <br>
    Epoch: 70, Loss: 0.0633, micro_f1: 0.9595, train_time:0.6702, val_time: 0.3396 <br>
    Epoch: 71, Loss: 0.0592, micro_f1: 0.9604, train_time:0.6657, val_time: 0.3508 <br>
    Epoch: 72, Loss: 0.0562, micro_f1: 0.9616, train_time:0.6433, val_time: 0.3205 <br>
    Epoch: 73, Loss: 0.0539, micro_f1: 0.9623, train_time:0.6510, val_time: 0.3215 <br>
    Epoch: 74, Loss: 0.0527, micro_f1: 0.9628, train_time:0.6459, val_time: 0.3251 <br>
    Epoch: 75, Loss: 0.0521, micro_f1: 0.9629, train_time:0.6399, val_time: 0.3315 <br>
    Epoch: 76, Loss: 0.0521, micro_f1: 0.9634, train_time:0.6471, val_time: 0.3320 <br>
    Epoch: 77, Loss: 0.0522, micro_f1: 0.9626, train_time:0.6246, val_time: 0.3230 <br>
    Epoch: 78, Loss: 0.0513, micro_f1: 0.9631, train_time:0.6407, val_time: 0.3286 <br>
    Epoch: 79, Loss: 0.0515, micro_f1: 0.9644, train_time:0.6330, val_time: 0.3324 <br>
    Epoch: 80, Loss: 0.0510, micro_f1: 0.9637, train_time:0.6337, val_time: 0.3231 <br>
    Epoch: 81, Loss: 0.0517, micro_f1: 0.9631, train_time:0.6778, val_time: 0.3214 <br>
    Epoch: 82, Loss: 0.0523, micro_f1: 0.9630, train_time:0.6502, val_time: 0.3179 <br>
    Epoch: 83, Loss: 0.0521, micro_f1: 0.9634, train_time:0.6475, val_time: 0.3348 <br>
    Epoch: 84, Loss: 0.0495, micro_f1: 0.9645, train_time:0.6606, val_time: 0.3179 <br>
    Epoch: 85, Loss: 0.0488, micro_f1: 0.9640, train_time:0.6491, val_time: 0.3308 <br>
    Epoch: 86, Loss: 0.0504, micro_f1: 0.9628, train_time:0.6613, val_time: 0.3243 <br>
    Epoch: 87, Loss: 0.0509, micro_f1: 0.9621, train_time:0.6380, val_time: 0.3247 <br>
    Epoch: 88, Loss: 0.0496, micro_f1: 0.9626, train_time:0.6375, val_time: 0.3207 <br>
    Epoch: 89, Loss: 0.0498, micro_f1: 0.9620, train_time:0.6620, val_time: 0.3287 <br>
    Epoch: 90, Loss: 0.0533, micro_f1: 0.9562, train_time:0.6414, val_time: 0.3392 <br>
    Epoch: 91, Loss: 0.0613, micro_f1: 0.9582, train_time:0.6646, val_time: 0.3503 <br>
    Epoch: 92, Loss: 0.0578, micro_f1: 0.9602, train_time:0.6608, val_time: 0.3305 <br>
    Epoch: 93, Loss: 0.0532, micro_f1: 0.9624, train_time:0.6625, val_time: 0.3254 <br>
    Epoch: 94, Loss: 0.0502, micro_f1: 0.9631, train_time:0.6472, val_time: 0.3447 <br>
    Epoch: 95, Loss: 0.0494, micro_f1: 0.9633, train_time:0.6459, val_time: 0.3349 <br>
    Epoch: 96, Loss: 0.0489, micro_f1: 0.9636, train_time:0.6616, val_time: 0.3328 <br>
    Epoch: 97, Loss: 0.0481, micro_f1: 0.9645, train_time:0.6416, val_time: 0.3278 <br>
    Epoch: 98, Loss: 0.0469, micro_f1: 0.9646, train_time:0.6615, val_time: 0.3304 <br>
    Epoch: 99, Loss: 0.0830, micro_f1: 0.9439, train_time:0.6633, val_time: 0.3528 <br>
    Epoch: 100, Loss: 0.0769, micro_f1: 0.9534, train_time:0.6615, val_time: 0.3223 <br>
  * baseline: pyg 实现 <br>
    Epoch: 001, Loss: 0.5599, Val: 0.4894, Test: 0.4910, train_time:1.1019, val_time:0.2715, test_time:0.2180 <br>
    Epoch: 002, Loss: 0.5212, Val: 0.5229, Test: 0.5273, train_time:0.8187, val_time:0.2555, test_time:0.2158 <br>
    Epoch: 003, Loss: 0.4995, Val: 0.5330, Test: 0.5382, train_time:0.7585, val_time:0.2517, test_time:0.2240 <br>
    Epoch: 004, Loss: 0.4713, Val: 0.5454, Test: 0.5559, train_time:0.7695, val_time:0.2481, test_time:0.2201 <br>
    Epoch: 005, Loss: 0.4335, Val: 0.6091, Test: 0.6230, train_time:0.8903, val_time:0.2590, test_time:0.2403 <br>
    Epoch: 006, Loss: 0.3954, Val: 0.6738, Test: 0.6889, train_time:0.8326, val_time:0.2709, test_time:0.2426 <br>
    Epoch: 007, Loss: 0.3407, Val: 0.7292, Test: 0.7476, train_time:0.8284, val_time:0.2778, test_time:0.2445 <br>
    Epoch: 008, Loss: 0.2976, Val: 0.7629, Test: 0.7825, train_time:0.8169, val_time:0.2631, test_time:0.2306 <br>
    Epoch: 009, Loss: 0.2628, Val: 0.7995, Test: 0.8188, train_time:0.8147, val_time:0.2989, test_time:0.2316 <br>
    Epoch: 010, Loss: 0.2433, Val: 0.8045, Test: 0.8264, train_time:0.7705, val_time:0.2613, test_time:0.2322 <br>
    Epoch: 011, Loss: 0.2172, Val: 0.8254, Test: 0.8456, train_time:0.8756, val_time:0.2681, test_time:0.2287 <br>
    Epoch: 012, Loss: 0.1987, Val: 0.8416, Test: 0.8626, train_time:0.7717, val_time:0.2588, test_time:0.2239 <br>
    Epoch: 013, Loss: 0.1855, Val: 0.8522, Test: 0.8723, train_time:0.8198, val_time:0.3065, test_time:0.2384 <br>
    Epoch: 014, Loss: 0.1677, Val: 0.8670, Test: 0.8893, train_time:0.8172, val_time:0.3038, test_time:0.2336 <br>
    Epoch: 015, Loss: 0.1451, Val: 0.8863, Test: 0.9061, train_time:0.8113, val_time:0.2876, test_time:0.2403 <br>
    Epoch: 016, Loss: 0.1310, Val: 0.8984, Test: 0.9176, train_time:0.8096, val_time:0.2625, test_time:0.2663 <br>
    Epoch: 017, Loss: 0.1202, Val: 0.8921, Test: 0.9129, train_time:0.7687, val_time:0.2609, test_time:0.2517 <br>
    Epoch: 018, Loss: 0.1313, Val: 0.8783, Test: 0.8967, train_time:0.7757, val_time:0.2976, test_time:0.2385 <br>
    Epoch: 019, Loss: 0.1439, Val: 0.8854, Test: 0.9073, train_time:0.8450, val_time:0.2681, test_time:0.2451 <br>
    Epoch: 020, Loss: 0.1325, Val: 0.8868, Test: 0.9065, train_time:0.8598, val_time:0.2655, test_time:0.2338 <br>
    Epoch: 021, Loss: 0.1232, Val: 0.8995, Test: 0.9173, train_time:0.7906, val_time:0.2600, test_time:0.2341 <br>
    Epoch: 022, Loss: 0.1189, Val: 0.9058, Test: 0.9244, train_time:0.7871, val_time:0.2626, test_time:0.2334 <br>
    Epoch: 023, Loss: 0.1259, Val: 0.8915, Test: 0.9083, train_time:0.7983, val_time:0.2586, test_time:0.2295 <br>
    Epoch: 024, Loss: 0.1195, Val: 0.8982, Test: 0.9166, train_time:0.8242, val_time:0.2706, test_time:0.2469 <br>
    Epoch: 025, Loss: 0.1145, Val: 0.9049, Test: 0.9233, train_time:0.8945, val_time:0.2641, test_time:0.2326 <br>
    Epoch: 026, Loss: 0.1024, Val: 0.9178, Test: 0.9335, train_time:0.7958, val_time:0.2602, test_time:0.2281 <br>
    Epoch: 027, Loss: 0.0918, Val: 0.9167, Test: 0.9324, train_time:0.8116, val_time:0.2771, test_time:0.2525 <br>
    Epoch: 028, Loss: 0.0953, Val: 0.9200, Test: 0.9373, train_time:0.8455, val_time:0.2887, test_time:0.2444 <br>
    Epoch: 029, Loss: 0.0998, Val: 0.9055, Test: 0.9258, train_time:0.8552, val_time:0.3002, test_time:0.2519 <br>
    Epoch: 030, Loss: 0.1005, Val: 0.9126, Test: 0.9303, train_time:0.7948, val_time:0.2597, test_time:0.2289 <br>
    Epoch: 031, Loss: 0.0850, Val: 0.9260, Test: 0.9408, train_time:0.8004, val_time:0.2826, test_time:0.2305 <br>
    Epoch: 032, Loss: 0.0822, Val: 0.9228, Test: 0.9412, train_time:0.8790, val_time:0.2625, test_time:0.2387 <br>
    Epoch: 033, Loss: 0.0856, Val: 0.9182, Test: 0.9350, train_time:0.8468, val_time:0.2819, test_time:0.2420 <br>
    Epoch: 034, Loss: 0.0908, Val: 0.9193, Test: 0.9360, train_time:0.7867, val_time:0.2587, test_time:0.2268 <br>
    Epoch: 035, Loss: 0.0888, Val: 0.9275, Test: 0.9436, train_time:0.7670, val_time:0.2601, test_time:0.2303 <br>
    Epoch: 036, Loss: 0.0784, Val: 0.9311, Test: 0.9459, train_time:0.7572, val_time:0.2763, test_time:0.2441 <br>
    Epoch: 037, Loss: 0.0725, Val: 0.9325, Test: 0.9483, train_time:0.8589, val_time:0.2832, test_time:0.2440 <br>
    Epoch: 038, Loss: 0.0684, Val: 0.9371, Test: 0.9526, train_time:0.8181, val_time:0.2665, test_time:0.2615 <br>
    Epoch: 039, Loss: 0.0655, Val: 0.9301, Test: 0.9440, train_time:0.7637, val_time:0.2628, test_time:0.2298 <br>
    Epoch: 040, Loss: 0.0926, Val: 0.9132, Test: 0.9286, train_time:0.7741, val_time:0.2732, test_time:0.2290 <br>
    Epoch: 041, Loss: 0.1030, Val: 0.9078, Test: 0.9247, train_time:0.8021, val_time:0.2809, test_time:0.2421 <br>
    Epoch: 042, Loss: 0.0956, Val: 0.9244, Test: 0.9399, train_time:0.7995, val_time:0.2608, test_time:0.2339 <br>
    Epoch: 043, Loss: 0.0876, Val: 0.9307, Test: 0.9469, train_time:0.7981, val_time:0.2689, test_time:0.2367 <br>
    Epoch: 044, Loss: 0.0712, Val: 0.9364, Test: 0.9513, train_time:0.7977, val_time:0.2625, test_time:0.2279 <br>
    Epoch: 045, Loss: 0.0750, Val: 0.9307, Test: 0.9440, train_time:0.7926, val_time:0.2732, test_time:0.2445 <br>
    Epoch: 046, Loss: 0.0780, Val: 0.9291, Test: 0.9451, train_time:0.8602, val_time:0.2620, test_time:0.2355 <br>
    Epoch: 047, Loss: 0.0850, Val: 0.9159, Test: 0.9332, train_time:0.7880, val_time:0.3018, test_time:0.2607 <br>
    Epoch: 048, Loss: 0.0942, Val: 0.9181, Test: 0.9336, train_time:0.9193, val_time:0.2761, test_time:0.2398 <br>
    Epoch: 049, Loss: 0.0953, Val: 0.9186, Test: 0.9339, train_time:0.8398, val_time:0.2624, test_time:0.2374 <br>
    Epoch: 050, Loss: 0.1016, Val: 0.9048, Test: 0.9216, train_time:0.8109, val_time:0.2614, test_time:0.2440 <br>
    Epoch: 051, Loss: 0.1001, Val: 0.9144, Test: 0.9318, train_time:0.8316, val_time:0.2851, test_time:0.2448 <br>
    Epoch: 052, Loss: 0.0869, Val: 0.9223, Test: 0.9391, train_time:0.8125, val_time:0.2641, test_time:0.2300 <br>
    Epoch: 053, Loss: 0.0910, Val: 0.9172, Test: 0.9350, train_time:0.7778, val_time:0.2664, test_time:0.2575 <br>
    Epoch: 054, Loss: 0.1033, Val: 0.9089, Test: 0.9257, train_time:0.8271, val_time:0.2594, test_time:0.2408 <br>
    Epoch: 055, Loss: 0.1000, Val: 0.9071, Test: 0.9254, train_time:0.8394, val_time:0.2623, test_time:0.2349 <br>
    Epoch: 056, Loss: 0.0986, Val: 0.9120, Test: 0.9318, train_time:0.8475, val_time:0.2919, test_time:0.2380 <br>
    Epoch: 057, Loss: 0.0957, Val: 0.9011, Test: 0.9182, train_time:0.7930, val_time:0.2581, test_time:0.2294 <br>
    Epoch: 058, Loss: 0.1148, Val: 0.9035, Test: 0.9228, train_time:0.7915, val_time:0.2616, test_time:0.2400 <br>
    Epoch: 059, Loss: 0.0984, Val: 0.9083, Test: 0.9283, train_time:0.8261, val_time:0.2677, test_time:0.2348 <br>
    Epoch: 060, Loss: 0.1072, Val: 0.9044, Test: 0.9219, train_time:0.9410, val_time:0.2975, test_time:0.2518 <br>
    Epoch: 061, Loss: 0.1166, Val: 0.8958, Test: 0.9134, train_time:0.8665, val_time:0.2631, test_time:0.2276 <br>
    Epoch: 062, Loss: 0.1172, Val: 0.9103, Test: 0.9274, train_time:0.7864, val_time:0.2581, test_time:0.2245 <br>
    Epoch: 063, Loss: 0.1145, Val: 0.8958, Test: 0.9150, train_time:0.7899, val_time:0.2584, test_time:0.2326 <br>
    Epoch: 064, Loss: 0.1197, Val: 0.8938, Test: 0.9112, train_time:0.8798, val_time:0.2614, test_time:0.2456 <br>
    Epoch: 065, Loss: 0.1164, Val: 0.9110, Test: 0.9283, train_time:0.7995, val_time:0.2640, test_time:0.2323 <br>
    Epoch: 066, Loss: 0.0990, Val: 0.9201, Test: 0.9348, train_time:0.7754, val_time:0.2605, test_time:0.2315 <br>
    Epoch: 067, Loss: 0.0993, Val: 0.9103, Test: 0.9290, train_time:0.7814, val_time:0.2595, test_time:0.2383 <br>
    Epoch: 068, Loss: 0.1014, Val: 0.9120, Test: 0.9299, train_time:0.7816, val_time:0.2644, test_time:0.2371 <br>
    Epoch: 069, Loss: 0.1163, Val: 0.8843, Test: 0.9030, train_time:0.8301, val_time:0.2752, test_time:0.2656 <br>
    Epoch: 070, Loss: 0.1229, Val: 0.9011, Test: 0.9199, train_time:0.9174, val_time:0.3154, test_time:0.2390 <br>
    Epoch: 071, Loss: 0.1181, Val: 0.8902, Test: 0.9053, train_time:0.8772, val_time:0.2680, test_time:0.2498 <br>
    Epoch: 072, Loss: 0.1381, Val: 0.8813, Test: 0.8987, train_time:0.8017, val_time:0.2596, test_time:0.2465 <br>
    Epoch: 073, Loss: 0.1407, Val: 0.8801, Test: 0.9022, train_time:0.9542, val_time:0.2676, test_time:0.2317 <br>
    Epoch: 074, Loss: 0.1231, Val: 0.8968, Test: 0.9169, train_time:0.8178, val_time:0.2950, test_time:0.2432 <br>
    Epoch: 075, Loss: 0.1049, Val: 0.9103, Test: 0.9283, train_time:0.8771, val_time:0.2666, test_time:0.2336 <br>
    Epoch: 076, Loss: 0.1140, Val: 0.8987, Test: 0.9151, train_time:0.7841, val_time:0.2632, test_time:0.2487 <br>
    Epoch: 077, Loss: 0.1262, Val: 0.8933, Test: 0.9113, train_time:0.8768, val_time:0.2919, test_time:0.2476 <br>
    Epoch: 078, Loss: 0.1251, Val: 0.8787, Test: 0.8995, train_time:0.8940, val_time:0.2940, test_time:0.2731 <br>
    Epoch: 079, Loss: 0.1433, Val: 0.8826, Test: 0.9008, train_time:0.8530, val_time:0.2662, test_time:0.2277 <br>
    Epoch: 080, Loss: 0.1290, Val: 0.8996, Test: 0.9179, train_time:0.7695, val_time:0.2645, test_time:0.2284 <br>
    Epoch: 081, Loss: 0.1161, Val: 0.8985, Test: 0.9142, train_time:0.7879, val_time:0.2606, test_time:0.2325 <br>
    Epoch: 082, Loss: 0.1299, Val: 0.8637, Test: 0.8824, train_time:0.7887, val_time:0.2700, test_time:0.2449 <br>
    Epoch: 083, Loss: 0.1593, Val: 0.8762, Test: 0.8943, train_time:0.8642, val_time:0.2821, test_time:0.2344 <br>
    Epoch: 084, Loss: 0.1360, Val: 0.8761, Test: 0.8940, train_time:0.8347, val_time:0.2670, test_time:0.2400 <br>
    Epoch: 085, Loss: 0.1408, Val: 0.8847, Test: 0.9043, train_time:0.7968, val_time:0.2746, test_time:0.2400 <br>
    Epoch: 086, Loss: 0.1286, Val: 0.8874, Test: 0.9063, train_time:0.8007, val_time:0.3054, test_time:0.2527 <br>
    Epoch: 087, Loss: 0.1340, Val: 0.8814, Test: 0.9012, train_time:0.8326, val_time:0.2699, test_time:0.2443 <br>
    Epoch: 088, Loss: 0.1267, Val: 0.8906, Test: 0.9096, train_time:0.7923, val_time:0.2634, test_time:0.2280 <br>
    Epoch: 089, Loss: 0.1161, Val: 0.8877, Test: 0.9043, train_time:0.7590, val_time:0.2576, test_time:0.2268 <br>
    Epoch: 090, Loss: 0.1179, Val: 0.8997, Test: 0.9164, train_time:0.8663, val_time:0.2887, test_time:0.2782 <br>
    Epoch: 091, Loss: 0.1156, Val: 0.8767, Test: 0.8999, train_time:0.8980, val_time:0.2705, test_time:0.2466 <br>
    Epoch: 092, Loss: 0.1470, Val: 0.8554, Test: 0.8782, train_time:0.8096, val_time:0.2613, test_time:0.2283 <br>
    Epoch: 093, Loss: 0.1636, Val: 0.8563, Test: 0.8762, train_time:0.7880, val_time:0.2621, test_time:0.2390 <br>
    Epoch: 094, Loss: 0.1546, Val: 0.8628, Test: 0.8837, train_time:0.8532, val_time:0.2987, test_time:0.2650 <br>
    Epoch: 095, Loss: 0.1429, Val: 0.8537, Test: 0.8754, train_time:0.8654, val_time:0.2621, test_time:0.2534 <br>
    Epoch: 096, Loss: 0.1422, Val: 0.8697, Test: 0.8923, train_time:1.0101, val_time:0.2849, test_time:0.2361 <br>
    Epoch: 097, Loss: 0.1322, Val: 0.8747, Test: 0.8963, train_time:0.8134, val_time:0.2664, test_time:0.2270 <br>
    Epoch: 098, Loss: 0.1276, Val: 0.8848, Test: 0.9053, train_time:0.7759, val_time:0.2574, test_time:0.2303 <br>
    Epoch: 099, Loss: 0.1196, Val: 0.8821, Test: 0.9023, train_time:0.8370, val_time:0.2633, test_time:0.2331 <br>
    Epoch: 100, Loss: 0.1239, Val: 0.8871, Test: 0.9071, train_time:0.8368, val_time:0.2618, test_time:0.2275 <br>
