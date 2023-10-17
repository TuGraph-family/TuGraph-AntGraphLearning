# Neighbor Filtering

We have observed that the algorithms for graph learning are becoming increasingly sophisticated and diverse. There is a growing demand for filtering and sampling based on the properties of nodes and edges. Therefore, AGL supports neighbor filtering based on the attributes of nodes and edges.

For example, if node A has transactions with many other nodes, you can configure a filter like `neighbor.trade_amount >= 100` to filter out neighbors of A with transaction amounts less than 100.

We also provide indexing functionality to optimize the efficiency of filtering and sampling. This document primarily focuses on filtering functionality, while indexing and sampling will be discussed in more detail in subsequent sections.

## Property Format

The format for properties that filtering depends on is `SOURCE.COLUMN`, where SOURCE represents the source data, and COLUMN is the attribute of that source. The possible values for SOURCE are as follows:

|   SOURCE    |          Meaning                |
| ----------- | ------------------------------- |
|   neighbor  | Attribute of the current neighbor (regardless of hop) |
|  neighbor_1 | Attribute of neighbors at the 1st hop   |
|  neighbor_k | Attribute of neighbors at the kth hop   |
|    seed     | Attribute of the seed node      |

## Type-Based Filtering Conditions

Filtering based on the type of a column's attribute supports operators such as `in`, `not in`, `=`, `==`, and `!=`. Here are some examples:

* `neighbor.user_level in ('Gold', 'Platinum')`: This means only consider neighbors with "Gold" or "Platinum" levels.
* `neighbor.city == seed.city`: This means the city attribute of neighbors must be the same as the seed node.
* `neighbor.type != 'merchant'`: This means do not consider neighbors with a type of "merchant".

## Numeric Range Filtering Conditions

Filtering based on the range of a column's attribute supports operators like `<`, `<=`, `>`, and `>=`. Here are some examples:

* `neighbor.trade_amount >= 100`: This means do not consider neighbors with transaction amounts less than 100.
* `neighbor.time > seed.time`: This means the time attribute of neighbors must be greater than the seed node, where is used in dynamic graph scenarios.
* `neighbor_2.time > neighbor_1.time`: This means the time attribute of second-hop neighbors must be greater than their first-hop neighbors.

## Logical Combination of Multiple Filtering Conditions

We support complex filtering conditions using logical operators like `AND`, `OR`, and `()`, similar to SQL's `WHERE` clause. Here are some examples:

* `neighbor.company = seed.company AND (neighbor.trade_amount >= 100 OR neighbor.user_level in ('Gold', 'Platinum'))`: The `()` ensures that the `OR` operator is evaluated before the `AND` operator.

## Applying Separate Filtering Conditions to Each Hop Neighbor Sampling

When conducting multi-hop sampling, users can configure a single filter condition for the entire process or specify separate filter condition for each hop, separated by semicolons. Here are some examples:

* `neighbor_1.time > seed.time; neighbor_2.time > neighbor_1.time`: This means the seed time is less than that of the first-hop neighbors, and the first-hop neighbor's time is less than next-hop neighbors.
* `neighbor.type = 'user-merchant'; neighbor.type = 'merchant-item'`: This describes the metapath sampling requirement as "user-merchant-item".
* `neighbor.type in ('user-merchant', 'user-item'); neighbor.type in ('merchant-item', 'item-item')`: This describes the metapath sampling requirement as "user-merchant-item" or "user-item-item."
