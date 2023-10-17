from collections import defaultdict
import heapq
import math
from tqdm import tqdm
from numba import jit


class Metric(object):
    def __init__(self):
        pass

    @staticmethod
    def hits(origin, res):
        hit_count = {}
        for user in origin:
            items = list(origin[user].keys())
            predicted = [item[0] for item in res[user]]
            hit_count[user] = len(set(items).intersection(set(predicted)))
        return hit_count

    @staticmethod
    def hit_ratio(origin, hits):
        """
        Note: This type of hit ratio calculates the fraction:
         (# retrieved interactions in the test set / #all the interactions in the test set)
        """
        total_num = 0
        for user in origin:
            items = list(origin[user].keys())
            total_num += len(items)
        hit_num = 0
        for user in hits:
            hit_num += hits[user]
        return hit_num / total_num

    @staticmethod
    def precision(hits, N):
        prec = sum([hits[user] for user in hits])
        return prec / (len(hits) * N)

    @staticmethod
    def recall(hits, origin):
        recall_list = [hits[user] / len(origin[user]) for user in hits]
        recall = sum(recall_list) / len(recall_list)
        return recall

    @staticmethod
    def F1(prec, recall):
        if (prec + recall) != 0:
            return 2 * prec * recall / (prec + recall)
        else:
            return 0

    @staticmethod
    def MAE(res):
        error = 0
        count = 0
        for entry in res:
            error += abs(entry[2] - entry[3])
            count += 1
        if count == 0:
            return error
        return error / count

    @staticmethod
    def RMSE(res):
        error = 0
        count = 0
        for entry in res:
            error += (entry[2] - entry[3]) ** 2
            count += 1
        if count == 0:
            return error
        return math.sqrt(error / count)

    @staticmethod
    def NDCG(origin, res, N):
        sum_NDCG = 0
        for user in res:
            DCG = 0
            IDCG = 0
            # 1 = related, 0 = unrelated
            for n, item in enumerate(res[user]):
                if item[0] in origin[user]:
                    DCG += 1.0 / math.log(n + 2, 2)
            for n, item in enumerate(list(origin[user].keys())[:N]):
                IDCG += 1.0 / math.log(n + 2, 2)
            sum_NDCG += DCG / IDCG
        return sum_NDCG / len(res)


def ranking_evaluation(origin, rec_list, N):
    predicted = {}
    for user in rec_list:
        predicted[user] = rec_list[user][:N]
    if len(origin) != len(predicted):
        print("The Lengths of test set and predicted set do not match!")
        exit(-1)
    if len(origin) == 0:
        return {
            "Hit_Ratio": -1,
            "Precision": -1,
            "Recall": -1,
            "NDCG": -1,
        }

    measure = {}
    hits = Metric.hits(origin, predicted)
    hr = Metric.hit_ratio(origin, hits)
    prec = Metric.precision(hits, N)
    recall = Metric.recall(hits, origin)
    NDCG = Metric.NDCG(origin, predicted, N)
    measure["Hit_Ratio"] = hr
    measure["Precision"] = prec
    measure["Recall"] = recall
    measure["NDCG"] = NDCG
    return measure


def rating_evaluation(res):
    measure = []
    mae = Metric.MAE(res)
    measure.append("MAE:" + str(mae) + "\n")
    rmse = Metric.RMSE(res)
    measure.append("RMSE:" + str(rmse) + "\n")
    return measure


@jit(nopython=True)
def find_k_largest(K, candidates):
    # 找前 k 大，稳定排序（优先 score 最大，其次 item id 最大）
    n_candidates = []
    for iid, score in enumerate(candidates[:K]):
        n_candidates.append((score, iid))

    heapq.heapify(n_candidates)

    for iid, score in enumerate(candidates[K:]):
        if score > n_candidates[0][0]:
            # 比堆顶元素大，入堆
            heapq.heapreplace(n_candidates, (score, iid + K))
    n_candidates.sort(key=lambda d: d, reverse=True)
    ids = [item[1] for item in n_candidates]
    k_largest_scores = [item[0] for item in n_candidates]
    return ids, k_largest_scores


def cal_metrics(c_data, top_k=100):
    # 构造 ground truth
    origin = defaultdict(dict)
    for user_id, item_id in c_data.query("label == 1")[["user_id", "item_id"]].values:
        origin[user_id][item_id] = 1

    # 产出 pred score
    rec_list = {}
    for user_id, group in tqdm(
        c_data.query(f"user_id in {list(origin.keys())}").groupby("user_id"),
        disable=True,
    ):
        candicates = group["score"].tolist()
        id2item = group["item_id"].tolist()

        item_ids, scores = find_k_largest(top_k, candicates, id2item)
        rec_list[user_id] = list(zip(item_ids, scores))

    return ranking_evaluation(origin, rec_list, top_k)
