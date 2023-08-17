import torch
import numpy as np


def load_data(filename, filter_label=False):
    res = []
    with open(filename, 'r') as f:
        f.readline()
        for line in f:
            l = line.strip().split(',')
            if filter_label and l[3] == '0':
                continue
            res.append([int(l[0]), int(l[1])])
    res = np.array(res)
    return res

user_num = 2101
item_num = 18746
node_num = user_num+item_num
neg_sample_num = 100

user_embed = torch.load('result/model2_user_embed.pt').cpu().detach().numpy()
item_embed = torch.load('result/model2_item_embed.pt').cpu().detach().numpy()
test_data = load_data('./data/subgraph_ssr_lastfm_test.csv', filter_label=True)
edge_table = load_data('./data/agl_gzoo_bmdata_ssr_lastfm_open_source_edge_table.csv')
print(user_embed.shape, item_embed.shape, test_data.shape, edge_table.shape)

edge_dict = {}
for u, i in edge_table:
    if u not in edge_dict:
        edge_dict[u] = set()
    edge_dict[u].add(i)

test_dict = {}
for u, i in test_data:
    if u not in test_dict:
        test_dict[u] = set()
    test_dict[u].add(i)

prec_15, recall_15 = [], []
n = 0
for u, items in test_dict.items():
    embed_u = user_embed[u]
    embed_items = item_embed[[i-user_num for i in items]]
    pos_scores = np.sum(embed_u*embed_items, -1)
    neg_index = []
    for _ in range(neg_sample_num):
        j = np.random.randint(user_num, node_num)
        if j in edge_dict[u] or j in items:
            continue
        neg_index.append(j)
    neg_index = np.array(neg_index)
    neg_embed = item_embed[neg_index-user_num]
    neg_scores = np.sum(embed_u*neg_embed, -1)
    scores = np.concatenate([pos_scores, neg_scores])
    hits = len(set(np.argsort(scores)[::-1][:15]).intersection(set(range(len(pos_scores)))))
    prec = hits*1.0/15
    recall = hits*1.0/len(pos_scores)
    prec_15.append(prec)
    recall_15.append(recall)
    n += 1
    if n % 100 == 0:
        print(n, np.mean(prec_15), np.mean(recall_15))
prec_15 = np.mean(prec_15)
recall_15 = np.mean(recall_15)
print('prec@15: {:.4f}, recall@15: {:.4f}'.format(prec_15, recall_15))
