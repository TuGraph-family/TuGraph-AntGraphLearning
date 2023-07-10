#ifndef AGL_FRAME_H
#define AGL_FRAME_H
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#include "base_data_structure/coo.h"
#include "base_data_structure/csr.h"

namespace agl {

// 一种类型的边对应的 Frame
// 记录一种边对应的 seeds nodes, 根据 seeds nodes 进行 bfs 得到的边，以及 unique
// n2 nodes
class UnitFrame {
 public:
  UnitFrame(const std::string& n1_name, const std::string& n2_name,
            const std::string& edge_name,
            std::shared_ptr<std::vector<IdDType>>& seed_nodes,
            std::shared_ptr<CSRAdj>& adj)
      : n1_name_(n1_name),
        n2_name_(n2_name),
        edge_name_(edge_name),
        seed_nodes_(seed_nodes),
        edges_whole_(adj) {}

  void Init();

  std::shared_ptr<COO>& GetEdgeCOO() { return edges_for_seeds_; }
  std::shared_ptr<std::vector<IdDType>>& GetN2Nodes() { return n2_nodes_; }

 private:
  // init with follow:
  std::string n1_name_;
  std::string n2_name_;
  std::string edge_name_;
  std::shared_ptr<std::vector<IdDType>> seed_nodes_;  // n1
  std::shared_ptr<CSRAdj> edges_whole_;

  // generate follow:
  std::shared_ptr<std::vector<IdDType>> n2_nodes_;
  // 只筛选出特定数目的 edge, 再使用 csr 似乎没有必要
  std::shared_ptr<COO> edges_for_seeds_;
};

// 用于描述 one hop graph
// 包含:
// (1)当前graph 的 root nodes (n1 nodes)
// (2)当前 graph 的各种 edges
// (3)当前graph 的 n2 nodes (可以作为下一跳的 root nodes)

// 是否需要每种类型的边，一个 Frame?
// 当前的考虑是不用，主要原因是一种类型的seed nodes 可能产生多种类型的边，
// 如果每种一个Frame, seed nodes 可能会存储多份
class Frame {
 public:
  Frame(std::unordered_map<std::string, std::vector<IdDType>> seed_nodes,
        std::unordered_map<std::string, std::shared_ptr<UnitFrame>>& e_to_uf,
        std::unordered_map<std::string,
                           std::vector<std::shared_ptr<UnitFrame>>>& n2_to_uf,
        bool add_seed_to_next = true);

  const std::unordered_map<std::string, std::vector<IdDType>>& GetSeeds() const;
  const std::unordered_map<std::string, std::vector<IdDType>>& GetNextNodes()
      const;
  const std::unordered_map<std::string, std::shared_ptr<COOAdj>>& GetCOOEdges()
      const;

 private:
  std::unordered_map<std::string, std::vector<IdDType>> seed_nodes_;
  std::unordered_map<std::string, std::vector<IdDType>> next_nodes_;
  std::unordered_map<std::string, std::shared_ptr<COOAdj>> edges_;
};

}  // namespace agl
#endif  // AGL_FRAME_H
