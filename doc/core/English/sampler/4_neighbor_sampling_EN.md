# Neighbor Sampling

We have implemented some common sampling functionalities in graph learning: random sampling, top-k sampling, and weighted probability sampling.

## Sampling Configuration

The `sample_cond` expression format is similar to a Python function call and has the following format:
`random_method(by=column_name, limit=k, replacement=True/False, reverse=True/False)`.

| Configuration   | Description                                                               |
| --------------- | ------------------------------------------------------------------------- |
| random_method   | Optional values are `random_sample`, `topk`, and `weighted_sample`, corresponding to random sampling, top-k sampling, and weighted probability sampling, respectively. |
| limit           | Limit on the number of direct neighboring nodes for each node.           |
| by              | Specifies a column from the edge table for sampling. For example, weighted sampling based on the "rate" column, not applicable in random_sample mode. |
| replacement     | Indicates whether to perform sampling with or without replacement. Not applicable in topk mode. |
| reverse         | Specifies whether to perform top-k sampling in ascending or descending order. Only applicable in topk mode. |

### Random Sampling
When `random_method` is configured as `random_sample`, it performs random sampling by selecting k nodes randomly from n candidates. You can choose to sample with or without replacement.

### Top-k Sampling
When `random_method` is configured as `topk`, it performs top-k sampling by selecting the k neighbors with the highest or lowest attribute values (as specified by `by`).
It's worth mentioning that if the attribute has already been sorted during the indexing phase, you can reuse the sorted data to return the top-k neighbors directly.

### Weighted Probability Sampling
When `random_method` is configured as `weighted_sample`, it performs weighted probability sampling. The probability of a sample being selected is proportional to its weight. You can choose to sample with or without replacement.