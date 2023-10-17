# Neighbor Attribute Indexing

Since subgraph sampling needs to propagate neighbors layer by layer, the size of subgraphs grows exponentially with the number of hops. 
Filtering and sampling of neighbors for each node are performed multiple times, and as the graph becomes denser, the number of filtering and sampling operations also increases.

To avoid traversing all neighbor nodes every time during filtering and sampling, we have developed attribute-based indexing functionality that can significantly improve the performance of neighbor filtering and sampling.

## Preparing Neighbor Attributes

To establish neighbor attribute indexing, users need to first build neighbor attributes. These attributes can come from edges or from node2_ids. 

For example, in the following edge table, the first float in edge_feature is extracted as a separate column "rate", 
and a string element from an auxiliary attribute table is joined using node2_id as the foreign key, then stored as a separate column "n2_city".

| node1_id  | node2_id  |   edge_feature  |  rate   | n2_city |
| --------- | --------- | --------------- | ------- | ------- |
| user1     | item1     |   0.0 0.1 0.3   |   0.1   |    BJ   |
| user2     | item1     |   0.3 0.5 0.2   |   0.3   |    SH   |
| user3     | item2     |   0.1 0.4 0.7   |   0.1   |    HZ   |

## Neighbor Hash Indexing

### Hash Index Configuration

The configuration format for hash indexing is "hash_index:key_name:key_type". It consists of three components: the index type is "hash", the column name for the index attribute, and the type of the attribute value.

### Example of Hash Indexing

For the above edge table, we can use the "n2_city" column to establish a hash index, configured as "hash_index:n2_city:String". This way, we have created a hash index using "n2_city".

## Neighbor Range Indexing

### Range Index Configuration

The configuration format for range indexing is "range_index:key_name:key_type". It consists of three components: the index type is "range", the column name for the index attribute, and the type of the attribute value.

### Example of Range Indexing

For the above edge table, we can use the "rate" column to establish a range index, configured as "range_index:rate:float". This way, we have created a range index using "rate".

## Establishing Multiple Indexes

It is also possible to establish multiple indexes simultaneously. By configuring "hash_index:n2_city:String;range_index:n2_city:String," we have created both a hash index and a range index. The separator for multiple index configurations is ';'.