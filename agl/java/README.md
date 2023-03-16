# 分布式支持过滤采样的GNN子图样本框架
* 图表征学习的算法发展越来越精细化，针对节点和边的某种属性的采样需求越来越大, 因此实现点和边的属性索引功能，实现高效的过滤和采样的功能。
* 详细的设计参考 https://yuque.antfin.com/graph_embedding/graphml-internal/nupqgf?singleDoc# 《FlatV3支持过滤和采样》

## antlr生成parser
* 为了支持类似SQL的过滤条件，我们使用antlr4.5.3生成JAVA API的语法解析代码，方法如下：
```
export CLASSPATH=".:/usr/local/lib/antlr-4.5.3.jar:$CLASSPATH"
java org.antlr.v4.Tool Filter.g4 -visitor
```
* 将生成的文件放置于src/main/java/com/alipay/alps/flatv3/antlr4目录下