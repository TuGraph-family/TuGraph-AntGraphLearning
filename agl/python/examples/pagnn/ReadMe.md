# PaGNN Demo
> Paper: Inductive Link Prediction with Interactive Structure Learning on Attributed Graph
            https://2021.ecmlpkdd.org/wp-content/uploads/2021/07/sub_635.pdf
            
### Model:
PaGNN as a path aware model includes two main parts: broadcast and aggregation. The broadcast operation is to propagate the information of source nodes to the subgraph, and target nodes will perceive which nodes are propagated to and model the structural information of the subgraph during aggregation(i.e. paths, common neighbors).
  
  

# Benchmark
Facebook   AUC~98.6%
	Train 75%, Val 5%, Test 20%,  embedding_size = 32, n_hops=2
Logs:
  Epoch: 01, Loss: 0.2874, Val_AUC: 0.9037, Test_AUC: 0.8959, Final_AUC: 0.8959, train_time: 380.436493, val_time: 107.387108
  Epoch: 02, Loss: 0.2565, Val_AUC: 0.9167, Test_AUC: 0.9113, Final_AUC: 0.9113, train_time: 379.830383, val_time: 105.699690
  Epoch: 03, Loss: 0.2324, Val_AUC: 0.9776, Test_AUC: 0.9748, Final_AUC: 0.9748, train_time: 378.269327, val_time: 107.465273
  Epoch: 04, Loss: 0.1484, Val_AUC: 0.9839, Test_AUC: 0.9821, Final_AUC: 0.9821, train_time: 383.874315, val_time: 106.121735
  Epoch: 05, Loss: 0.1359, Val_AUC: 0.9810, Test_AUC: 0.9791, Final_AUC: 0.9821, train_time: 376.305634, val_time: 106.381106
  Epoch: 06, Loss: 0.1330, Val_AUC: 0.9822, Test_AUC: 0.9832, Final_AUC: 0.9821, train_time: 376.704582, val_time: 106.967821
  Epoch: 07, Loss: 0.1311, Val_AUC: 0.9817, Test_AUC: 0.9809, Final_AUC: 0.9821, train_time: 376.480518, val_time: 105.960921
  Epoch: 08, Loss: 0.1232, Val_AUC: 0.9847, Test_AUC: 0.9852, Final_AUC: 0.9852, train_time: 378.202595, val_time: 106.333275
  Epoch: 09, Loss: 0.1165, Val_AUC: 0.9831, Test_AUC: 0.9830, Final_AUC: 0.9852, train_time: 377.144849, val_time: 108.410433
  Epoch: 10, Loss: 0.1247, Val_AUC: 0.9837, Test_AUC: 0.9847, Final_AUC: 0.9852, train_time: 384.330160, val_time: 108.350580
  Epoch: 11, Loss: 0.1131, Val_AUC: 0.9842, Test_AUC: 0.9847, Final_AUC: 0.9852, train_time: 385.734705, val_time: 107.750543
  Epoch: 12, Loss: 0.1154, Val_AUC: 0.9837, Test_AUC: 0.9832, Final_AUC: 0.9852, train_time: 383.579289, val_time: 107.981150
  Epoch: 13, Loss: 0.1112, Val_AUC: 0.9855, Test_AUC: 0.9854, Final_AUC: 0.9854, train_time: 382.799133, val_time: 107.509377
  Epoch: 14, Loss: 0.1101, Val_AUC: 0.9849, Test_AUC: 0.9848, Final_AUC: 0.9854, train_time: 382.382064, val_time: 108.161631
  Epoch: 15, Loss: 0.1086, Val_AUC: 0.9845, Test_AUC: 0.9845, Final_AUC: 0.9854, train_time: 383.639369, val_time: 108.838336
  Epoch: 16, Loss: 0.1061, Val_AUC: 0.9856, Test_AUC: 0.9857, Final_AUC: 0.9857, train_time: 383.415629, val_time: 108.189606
  Epoch: 17, Loss: 0.1036, Val_AUC: 0.9856, Test_AUC: 0.9862, Final_AUC: 0.9862, train_time: 381.920808, val_time: 109.029660
  Epoch: 18, Loss: 0.1017, Val_AUC: 0.9846, Test_AUC: 0.9859, Final_AUC: 0.9862, train_time: 385.027288, val_time: 108.582406
  Epoch: 19, Loss: 0.1000, Val_AUC: 0.9854, Test_AUC: 0.9857, Final_AUC: 0.9862, train_time: 383.419828, val_time: 107.937082
  Epoch: 20, Loss: 0.0993, Val_AUC: 0.9856, Test_AUC: 0.9858, Final_AUC: 0.9862, train_time: 382.923361, val_time: 108.012105
  Epoch: 21, Loss: 0.0975, Val_AUC: 0.9864, Test_AUC: 0.9866, Final_AUC: 0.9866, train_time: 380.999595, val_time: 105.748604
  Epoch: 22, Loss: 0.0969, Val_AUC: 0.9863, Test_AUC: 0.9865, Final_AUC: 0.9866, train_time: 378.358583, val_time: 106.693759
  Epoch: 23, Loss: 0.0949, Val_AUC: 0.9861, Test_AUC: 0.9865, Final_AUC: 0.9866, train_time: 385.467453, val_time: 108.357811
  Epoch: 24, Loss: 0.0972, Val_AUC: 0.9853, Test_AUC: 0.9855, Final_AUC: 0.9866, train_time: 384.483368, val_time: 108.824938
  Epoch: 25, Loss: 0.0967, Val_AUC: 0.9854, Test_AUC: 0.9854, Final_AUC: 0.9866, train_time: 384.026031, val_time: 108.521998



Pubmed   AUC~94.1%
 	Train 75%, Val 5%, Test 20%,  embedding_size = 32, n_hops=2
Logs:
  Epoch: 01, Loss: 0.4376, Val_AUC: 0.8985, Test_AUC: 0.9038, Final_AUC: 0.9038, train_time: 325.235858, val_time: 84.538931
  Epoch: 02, Loss: 0.3512, Val_AUC: 0.9222, Test_AUC: 0.9291, Final_AUC: 0.9291, train_time: 322.038818, val_time: 84.642969
  Epoch: 03, Loss: 0.3311, Val_AUC: 0.9246, Test_AUC: 0.9314, Final_AUC: 0.9314, train_time: 323.316217, val_time: 84.442774
  Epoch: 04, Loss: 0.3263, Val_AUC: 0.9153, Test_AUC: 0.9223, Final_AUC: 0.9314, train_time: 324.821595, val_time: 84.640268
  Epoch: 05, Loss: 0.3343, Val_AUC: 0.9252, Test_AUC: 0.9321, Final_AUC: 0.9321, train_time: 323.989801, val_time: 84.509204
  Epoch: 06, Loss: 0.3374, Val_AUC: 0.8716, Test_AUC: 0.8790, Final_AUC: 0.9321, train_time: 322.228783, val_time: 84.002366
  Epoch: 07, Loss: 0.4274, Val_AUC: 0.8930, Test_AUC: 0.8998, Final_AUC: 0.9321, train_time: 319.993209, val_time: 85.039291
  Epoch: 08, Loss: 0.4441, Val_AUC: 0.8545, Test_AUC: 0.8609, Final_AUC: 0.9321, train_time: 325.361055, val_time: 85.668372
  Epoch: 09, Loss: 0.3778, Val_AUC: 0.9176, Test_AUC: 0.9239, Final_AUC: 0.9321, train_time: 324.677065, val_time: 84.033798
  Epoch: 10, Loss: 0.3366, Val_AUC: 0.9248, Test_AUC: 0.9306, Final_AUC: 0.9321, train_time: 323.139526, val_time: 84.098302
  Epoch: 11, Loss: 0.3240, Val_AUC: 0.9235, Test_AUC: 0.9295, Final_AUC: 0.9321, train_time: 325.894223, val_time: 84.128854
  Epoch: 12, Loss: 0.3202, Val_AUC: 0.9297, Test_AUC: 0.9348, Final_AUC: 0.9348, train_time: 325.761913, val_time: 84.220473
  Epoch: 13, Loss: 0.3115, Val_AUC: 0.9306, Test_AUC: 0.9360, Final_AUC: 0.9360, train_time: 326.451049, val_time: 84.056654
  Epoch: 14, Loss: 0.3060, Val_AUC: 0.9325, Test_AUC: 0.9373, Final_AUC: 0.9373, train_time: 324.388285, val_time: 84.209103
  Epoch: 15, Loss: 0.3040, Val_AUC: 0.9294, Test_AUC: 0.9344, Final_AUC: 0.9373, train_time: 323.140053, val_time: 85.050226
  Epoch: 16, Loss: 0.3008, Val_AUC: 0.9326, Test_AUC: 0.9378, Final_AUC: 0.9378, train_time: 321.854683, val_time: 84.415050
  Epoch: 17, Loss: 0.3008, Val_AUC: 0.9327, Test_AUC: 0.9381, Final_AUC: 0.9381, train_time: 321.498435, val_time: 85.207391
  Epoch: 18, Loss: 0.2982, Val_AUC: 0.9349, Test_AUC: 0.9397, Final_AUC: 0.9397, train_time: 322.666984, val_time: 84.518749
  Epoch: 19, Loss: 0.2922, Val_AUC: 0.9363, Test_AUC: 0.9411, Final_AUC: 0.9411, train_time: 321.937428, val_time: 84.985604
  Epoch: 20, Loss: 0.2913, Val_AUC: 0.9344, Test_AUC: 0.9400, Final_AUC: 0.9411, train_time: 321.992191, val_time: 84.927943
  Epoch: 21, Loss: 0.2935, Val_AUC: 0.9261, Test_AUC: 0.9313, Final_AUC: 0.9411, train_time: 323.596506, val_time: 84.102577
  Epoch: 22, Loss: 0.3043, Val_AUC: 0.9315, Test_AUC: 0.9367, Final_AUC: 0.9411, train_time: 321.777333, val_time: 84.588558
  Epoch: 23, Loss: 0.3044, Val_AUC: 0.9314, Test_AUC: 0.9355, Final_AUC: 0.9411, train_time: 322.780236, val_time: 85.008025
  Epoch: 24, Loss: 0.3070, Val_AUC: 0.9297, Test_AUC: 0.9340, Final_AUC: 0.9411, train_time: 322.570970, val_time: 83.989334
  Epoch: 25, Loss: 0.2948, Val_AUC: 0.9332, Test_AUC: 0.9366, Final_AUC: 0.9411, train_time: 322.835144, val_time: 84.995032