import numpy as np


def read_lastfm():
    edges = []
    with open("./data/user_artists.dat", "r") as f:
        f.readline()
        for line in f:
            l = line.strip().split()
            edges.append([int(l[0]), int(l[1])])
    edges = np.array(edges)
    print(
        "edge num: {}, distinct user num: {}, distinct item num: {}".format(
            len(edges), len(set(edges[:, 0])), len(set(edges[:, 1]))
        )
    )

    social_relations = []
    with open("./data/user_friends.dat", "r") as f:
        f.readline()
        for line in f:
            l = line.strip().split()
            social_relations.append([int(l[0]), int(l[1])])
    social_relations = np.array(social_relations)
    print(
        "social_relations num: {}, distinct user num: {}".format(
            len(social_relations),
            len(set(social_relations[:, 0]) | set(social_relations[:, 1])),
        )
    )
    return edges, social_relations


def negative_sampling(user_nbrs, edges, item_num):
    neg_edges = []
    for u, i in edges:
        j = np.random.randint(1, item_num)
        while j in user_nbrs[u]:
            j = np.random.randint(1, item_num)
        neg_edges.append([u, j])
    neg_edges = np.array(neg_edges)
    return neg_edges


if __name__ == "__main__":
    edges, social_relations = read_lastfm()
    np.random.shuffle(edges)
    user_num, item_num = max(edges[:, 0]) + 1, max(edges[:, 1]) + 1
    print(user_num, item_num)
    user_nbrs = {}
    for u, i in edges:
        if u not in user_nbrs:
            user_nbrs[u] = []
        user_nbrs[u].append(i)
    ratio = int(0.8 * len(edges))
    train_edges, test_edges = edges[:ratio], edges[ratio:]

    with open("./data/agl_gzoo_bmdata_ssr_lastfm_open_source_node_table.csv", "w") as f:
        print("node_id,node_feature", file=f)
        for i in range(user_num + item_num):
            print(i, i, file=f, sep=",")
    with open("./data/agl_gzoo_bmdata_ssr_lastfm_open_source_edge_table.csv", "w") as f:
        print("node1_id,node2_id,edge_id", file=f)
        for u, i in train_edges:
            i += user_num
            print(u, i, "{}_{}".format(u, i), file=f, sep=",")
            print(i, u, "{}_{}".format(i, u), file=f, sep=",")
        for u1, u2 in social_relations:
            print(u1, u2, "{}_{}".format(u1, u2), file=f, sep=",")
            print(u2, u1, "{}_{}".format(u2, u1), file=f, sep=",")

    train_neg_edges = negative_sampling(user_nbrs, train_edges, item_num)
    test_neg_edges = negative_sampling(user_nbrs, test_edges, item_num)
    with open("./data/agl_gzoo_bmdata_ssr_lastfm_open_source_node_label.csv", "w") as f:
        print("node1_id,node2_id,seed,label,train_flag", file=f)
        for u, i in train_edges:
            i += user_num
            print(u, i, "{}_{}".format(u, i), 1, 1, file=f, sep=",")
        for u, i in train_neg_edges:
            i += user_num
            print(u, i, "{}_{}".format(u, i), 0, 1, file=f, sep=",")
        for u, i in test_edges:
            i += user_num
            print(u, i, "{}_{}".format(u, i), 1, 0, file=f, sep=",")
        for u, i in test_neg_edges:
            i += user_num
            print(u, i, "{}_{}".format(u, i), 0, 0, file=f, sep=",")
