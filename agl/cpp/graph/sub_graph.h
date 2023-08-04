#ifndef AGL_SUB_GRAPH_H
#define AGL_SUB_GRAPH_H
#include <memory>
#include <string>
#include <unordered_map>

#include "edge_unit.h"
#include "node_unit.h"
#include "base_data_structure/frame.h"
#include "spec/unit_spec.h"

class IDMapping;

namespace agl {
class SubGraph {
 public:

  void AddNodeSpec(const std::string& n_name,
                   const std::shared_ptr<NodeSpec>& spec);
  void AddEdgeSpec(const std::string& e_name,
                   const std::shared_ptr<EdgeSpec>& spec);

  /**
   * 简化版 merge case, 全融合（去重）的方式进行解析或者 全不去重的方式解析
   * @param pbs ： pbs
   * @param merge : 是否要做去重处理
   * @param uncompress: 是否需要解压
   */
  void CreateFromPB(const std::vector<const char*>& pbs, const std::vector<size_t>& pb_length, bool merge,
                    bool uncompress);

  // 一系列的 Get 逻辑
  std::shared_ptr<CSRAdj> GetEdgeIndexCSR(const std::string& e_name) const;

  std::shared_ptr<Frame> GetEdgeIndexOneHop(
      std::unordered_map<std::string, std::vector<IdDType>>& ids,
      bool add_seed_to_next) const;

  std::vector<std::shared_ptr<Frame>> GetEgoFrames(int hops,
                                                   bool add_seed_to_next) const;

  std::shared_ptr<DenseFeatureArray> GetEdgeDenseFeatureArray(
      const std::string& e_name, const std::string& f_name) const;
  std::shared_ptr<SparseKVFeatureArray> GetEdgeSparseKVArray(
      const std::string& e_name, const std::string& f_name) const;
  std::shared_ptr<SparseKFeatureArray> GetEdgeSparseKArray(
      const std::string& e_name, const std::string& f_name) const;

  std::shared_ptr<DenseFeatureArray> GetNodeDenseFeatureArray(
      const std::string& n_name, const std::string& f_name) const;
  std::shared_ptr<SparseKVFeatureArray> GetNodeSparseKVArray(
      const std::string& n_name, const std::string& f_name) const;
  std::shared_ptr<SparseKFeatureArray> GetNodeSparseKArray(
      const std::string& n_name, const std::string& f_name) const;

  const std::shared_ptr<EdgeUint>& GetEdgeUnit(const std::string& e_name) const;
  const std::shared_ptr<NodeUint>& GetNodeUnit(const std::string& n_name) const;

  const std::unordered_map<std::string,
                           std::vector<std::pair<std::string, std::string>>>&
  GetTopo() const;

  const std::unordered_map<std::string, std::vector<std::vector<IdDType>>>&
  GetRootIds() const;

  const std::unordered_map<std::string, std::vector<int>>& GetNodeNumPerSample()
      const;

  const std::unordered_map<std::string, std::vector<int>>& GetEdgeNumPerSample()
      const;

 private:
  void CreateFromPBNonMerge(const std::vector<const char*>& pbs, const std::vector<size_t>& pb_length,
                            bool uncompress);

 private:
  bool is_ego_;
  int ego_hops_;

  bool merge_ = false;
  // node and edge unit, id是经过mapping后的，特征是存储在连续内存块的
  // node name -> node unit
  std::unordered_map<std::string, std::shared_ptr<NodeUint>> nodes_;
  // edge name -> edge unit
  std::unordered_map<std::string, std::shared_ptr<EdgeUint>> edges_;

  // root ids:
  // std::unordered_map<std::string, std::shared_ptr<RootNodeUnit>> root_ids_;
  // n_name -> pb_nums -> root_num
  std::unordered_map<std::string, std::vector<std::vector<IdDType>>> root_ids_;

  // id mapping, 在某些情况下，可能是需要原始id的
  std::unordered_map<std::string, std::shared_ptr<IDMapping>> node_id_mappings_;
  std::unordered_map<std::string, std::shared_ptr<IDMapping>>
      edge_id_mappings_;  // optional

  // node and edge spec， 用于做解析使用, 以及后续的check
  std::unordered_map<std::string, std::shared_ptr<NodeSpec>> node_specs_;
  std::unordered_map<std::string, std::shared_ptr<EdgeSpec>> edge_specs_;
  // topo node1_name <- {{node_2_name1, edge_name1}, {node_2_nameXXX,
  // edge_nameXXX}}}
  std::unordered_map<std::string,
                     std::vector<std::pair<std::string, std::string>>>
      topo_;

  // if merge_ == false， 存在以下内容
  // 主要提供以下内容，不融合的case下，每个样本中 点的数目 和 边的数目
  // 或者偏移量
  std::unordered_map<std::string, std::vector<int>> n_num_per_sample_;
  std::unordered_map<std::string, std::vector<int>> e_num_per_sample_;
};

}  // namespace agl

#endif  // AGL_SUB_GRAPH_H
