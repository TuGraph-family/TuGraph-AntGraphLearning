# 进阶教程总览

## 各种图应用总结

### 数据准备与生成

为了方便使用图计算框架，我们实现了许多常用图算法，并且在内部业务中进行了充分测试，保证了算法的运行效率和正确性。用户使用时无需过多调整，便可快速使用。

| 算法名称        | 算法类型                  | 说明                             |
| -------------- | ----------------------- | ------------------------------- |
| Geniepath      | 节点表征-同构图            |                                 |
| HeGNN          | 节点表征-异质图            |                                 |
| ST-GNN         | 节点表征-离散时间动态图     |                                 |
| MERIT          | 节点表征-连续时间动态图     |                                 |
| Gaia           | 节点表征-时序图            |                                |
| PaGNN          | 链接预测                  |                                |
| CD-GNN         | 链接预测-跨域推荐          |                                 |
| KCAN           | 链接预测-基于知识图谱的推荐  |                                 |
| SSR            | 链接预测-社交推荐          |                                 |
| LAGCL          | 链接预测                  |                                |
| NASA           | 图自监督                  |                                |
| DR-GST         | 图自训练                  |                                |
| TSAGMM         | 图异常检测                |                                 |

### 模型实现方式