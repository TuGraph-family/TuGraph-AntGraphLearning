import numpy as np
import scipy.sparse as sp
from scipy.linalg import fractional_matrix_power, inv, expm
import torch
import random

def load_npz_to_sparse_graph(file_name):

    with np.load('./dataset/' + file_name + '.npz', allow_pickle=True) as loader:
        loader = dict(loader)
        adj_matrix = sp.csr_matrix((loader['adj_data'], loader['adj_indices'], loader['adj_indptr']),
                                   shape=loader['adj_shape'])

        if 'attr_data' in loader:
            attr_matrix = sp.csr_matrix((loader['attr_data'], loader['attr_indices'], loader['attr_indptr']), shape=loader['attr_shape'])
        elif 'attr_matrix' in loader:
            attr_matrix = loader['attr_matrix']
        else:
            attr_matrix = None

        if 'labels_data' in loader:
            labels = sp.csr_matrix((loader['labels_data'], loader['labels_indices'], loader['labels_indptr']), shape=loader['labels_shape'])
        elif 'labels' in loader:
            labels = loader['labels']
        else:
            labels = None

        adj = adj_matrix.todense()
        col, row = np.where(adj>0)

        feat = attr_matrix.todense()
        num_class = len(set(labels))

        # graph = dgl.graph((col, row), num_nodes=adj.shape[0])
        # graph.ndata['feat'] = torch.FloatTensor(feat)
        # graph.ndata['label'] = torch.LongTensor(labels)

        with open(file_name + '_edge.csv', 'w') as outfile:
            outfile.write("node1_id,node2_id,edge_id\n")
            edge_set = set()
            for s, t in zip(col, row):
                edge_set.add(str(s) + ',' + str(t) + ',' + str(s) + '_' + str(t))
                edge_set.add(str(t) + ',' + str(s) + ',' + str(t) + '_' + str(s))

            outfile.write('\n'.join(list(edge_set)))

        print(feat.shape)
        with open(file_name + '_node.csv', 'w') as outfile:
            outfile.write("node_id,node_feature\n")
            m, n = feat.shape
            # print(m, n, type(feat))
            for i in range(m):
                outfile.write(str(i) + ',')
                atts = []
                for j in range(n):
                    # print(i,j, feat[i,j])
                    if feat[i,j] != 0:
                        atts.append(str(j) + ':' + str(feat[i,j]))
                outfile.write(' '.join(atts) + '\n')

        label = list(labels)
        # nclass = len(set(labels))
        # class_ind = [[] for i in range(nclass)]
        # for ind, lab in enumerate(label):
        #     class_ind[lab].append(ind)
        # # train = 20; # val = 30
        train_inds = [
            [416, 1199, 1342, 1986, 2085, 2167, 2858, 3071, 3696, 4119, 4299, 4929, 5956, 6254, 6394, 6456, 7066, 7089, 7138, 7256],
            [373, 548, 738, 805, 1783, 1859, 2616, 2731, 2764, 2836, 3777, 5014, 5273, 6583, 7063, 7120, 7154, 7516, 7545, 7634],
            [247, 541, 1306, 1685, 2132, 3057, 3167, 3820, 4099, 4965, 5319, 5425, 5426, 5848, 5906, 6220, 6320, 6840, 7248, 7368],
            [584, 1091, 1726, 1802, 1984, 2068, 3232, 3589, 3592, 3757, 3822, 4519, 4811, 4990, 5168, 5957, 6610, 6684, 7240, 7558],
            [198, 311, 393, 1253, 1377, 2045, 3073, 3382, 4141, 4187, 4407, 4637, 5156, 5299, 5346, 5696, 6447, 6827, 7035, 7330],
            [54, 142, 409, 504, 730, 797, 999, 1804, 1868, 2329, 2850, 3903, 4351, 4497, 4613, 5870, 6533, 6718, 7355, 7500],
            [450, 688, 963, 1652, 1697, 1944, 2755, 3027, 3081, 3178, 3898, 4071, 4082, 4941, 5220, 6076, 6185, 7166, 7511, 7557],
            [113, 194, 341, 410, 905, 1355, 1686, 2487, 2793, 2935, 2958, 3115, 3202, 3203, 3334, 4894, 5132, 5313, 6798, 7179]
        ]

        val_inds = [
            [140, 406, 437, 798, 1071, 1207, 1208, 1257, 1352, 2934, 3110, 3215, 3517, 3537, 3556, 3679, 3769, 4222,
             4927, 4998, 5149, 5590, 6285, 6415, 6708, 6742, 6820, 6977, 7301, 7327],
            [643, 673, 713, 1273, 1395, 2052, 2161, 2353, 2411, 2962, 2976, 3181, 3781, 4232, 4341, 4521, 4631, 4661,
             4760, 4831, 5041, 5043, 5596, 5634, 5715, 5789, 6392, 6861, 7140, 7232],
            [57, 467, 605, 926, 1213, 1389, 1471, 1616, 1630, 1709, 2050, 2192, 2975, 3065, 3489, 3529, 4741, 4960,
             4992, 5028, 5588, 5662, 5668, 6018, 6091, 6525, 6734, 7013, 7086, 7188],
            [677, 1046, 1448, 1682, 1760, 2322, 2655, 2682, 2884, 2977, 3270, 3317, 3913, 4707, 5078, 5264, 5589, 5941,
             6172, 6224, 6437, 6458, 6580, 6672, 6729, 6750, 6856, 7047, 7054, 7518],
            [226, 1205, 1406, 1707, 1962, 2071, 2415, 2738, 2905, 2999, 3419, 3473, 3579, 3643, 3834, 3889, 4330, 4436,
             4716, 4745, 4879, 5289, 5433, 5657, 6269, 6578, 6777, 6844, 7184, 7453],
            [172, 617, 700, 938, 1410, 1523, 1828, 1923, 2343, 3082, 3084, 3560, 3963, 4146, 4197, 4618, 4655, 4684,
             4763, 4789, 4974, 5169, 5361, 5627, 5759, 5883, 6002, 6344, 6524, 7404],
            [14, 128, 192, 220, 293, 318, 344, 408, 538, 943, 1169, 1665, 1892, 3008, 3148, 3206, 3446, 3648, 4157,
             4176, 5016, 5084, 5443, 5552, 5721, 5886, 6342, 6811, 7174, 7495],
            [523, 760, 852, 1753, 1826, 2470, 2513, 2558, 2704, 2798, 3012, 3078, 3213, 3268, 3527, 3545, 3618, 3650,
             4090, 4562, 4830, 5068, 5219, 5334, 5716, 6230, 6486, 6741, 6952, 7336]
        ]

        with open(file_name + '_label.csv', 'w') as outfile:
            outfile.write("node_id,seed,label,train_flag\n")
            for ind, lab in enumerate(label):
                if ind in train_inds[lab]:
                    outfile.write(str(ind) + ',' + "whole graph" + ',' + str(lab) + ',train\n')
                elif ind in val_inds[lab]:
                    outfile.write(str(ind) + ',' + "whole graph" + ',' + str(lab) + ',val\n')
                else:
                    outfile.write(str(ind) + ',' + "whole graph" + ',' + str(lab) + ',test\n')

            # print(num_train, num_val, num_test, num_train + num_val + num_test, len(label))


load_npz_to_sparse_graph('photo')
