#include "sub_graph.h"

namespace agl {

void SubGraph::AddNodeSpec(const std::string& n_name,
                           const std::shared_ptr<NodeSpec>& spec) {
  auto find = node_specs_.find(n_name);
  AGL_CHECK(find == node_specs_.end())
      << "node spec with name:" << n_name << " already exists!";
  node_specs_[n_name] = spec;
  nodes_[n_name] = std::make_shared<NodeUint>();
}

void SubGraph::AddEdgeSpec(const std::string& e_name,
                           const std::shared_ptr<EdgeSpec>& spec) {
  auto find = edge_specs_.find(e_name);
  AGL_CHECK(find == edge_specs_.end())
      << "edge spec with name:" << e_name << " already exists!";
  // 保存spec
  edge_specs_[e_name] = spec;
  // 创建 e_name 对应的 edge unit
  edges_[e_name] = std::make_shared<EdgeUint>();
  // 根据spec信息创建topo
  const auto& n1_name = spec->GetN1Name();
  const auto& n2_name = spec->GetN2Name();
  topo_[n1_name].push_back({n2_name, e_name});
}

std::shared_ptr<CSRAdj> SubGraph::GetEdgeIndexCSR(
    const std::string& e_name) const {
  auto& e_unit = GetEdgeUnit(e_name);
  return e_unit->GetCSRAdj();
}

std::shared_ptr<DenseFeatureArray> SubGraph::GetEdgeDenseFeatureArray(
    const std::string& e_name, const std::string& f_name) const {
  auto& e_unit = GetEdgeUnit(e_name);
  return e_unit->GetDenseFeatureArray(f_name);
}
std::shared_ptr<SparseKVFeatureArray> SubGraph::GetEdgeSparseKVArray(
    const std::string& e_name, const std::string& f_name) const {
  auto& e_unit = GetEdgeUnit(e_name);
  return e_unit->GetSparseKVArray(f_name);
}

std::shared_ptr<SparseKFeatureArray> SubGraph::GetEdgeSparseKArray(
    const std::string& e_name, const std::string& f_name) const {
  auto& e_unit = GetEdgeUnit(e_name);
  return e_unit->GetSparseKArray(f_name);
}

std::shared_ptr<DenseFeatureArray> SubGraph::GetNodeDenseFeatureArray(
    const std::string& n_name, const std::string& f_name) const {
  auto& n_unit = GetNodeUnit(n_name);
  return n_unit->GetDenseFeatureArray(f_name);
}
std::shared_ptr<SparseKVFeatureArray> SubGraph::GetNodeSparseKVArray(
    const std::string& n_name, const std::string& f_name) const {
  auto& n_unit = GetNodeUnit(n_name);
  return n_unit->GetSparseKVArray(f_name);
}
std::shared_ptr<SparseKFeatureArray> SubGraph::GetNodeSparseKArray(
    const std::string& n_name, const std::string& f_name) const {
  auto& n_unit = GetNodeUnit(n_name);
  return n_unit->GetSparseKArray(f_name);
}

const std::shared_ptr<EdgeUint>& SubGraph::GetEdgeUnit(
    const std::string& e_name) const {
  auto find = edges_.find(e_name);
  AGL_CHECK(find != edges_.end())
      << "Cant not find EdgeUnit with name:" << e_name;
  return find->second;
}

const std::shared_ptr<NodeUint>& SubGraph::GetNodeUnit(
    const std::string& n_name) const {
  auto find = nodes_.find(n_name);
  AGL_CHECK(find != nodes_.end())
      << "Cant not find NodeUnit with name:" << n_name;
  return find->second;
}

const std::unordered_map<std::string,
                         std::vector<std::pair<std::string, std::string>>>&
SubGraph::GetTopo() const {
  return topo_;
}

const std::unordered_map<std::string, std::vector<std::vector<IdDType>>>&
SubGraph::GetRootIds() const {
  return root_ids_;
}

const std::unordered_map<std::string, std::vector<int>>&
SubGraph::GetNodeNumPerSample() const {
  AGL_CHECK(!merge_) << "GetNodeNumPerSample Not supported when merge = true!";
  return n_num_per_sample_;
}

const std::unordered_map<std::string, std::vector<int>>&
SubGraph::GetEdgeNumPerSample() const {
  AGL_CHECK(!merge_) << " GetEdgeNumPerSample Not supported when merge = true!";
  return e_num_per_sample_;
}

std::shared_ptr<Frame> SubGraph::GetEdgeIndexOneHop(
    std::unordered_map<std::string, std::vector<IdDType>>& ids,
    bool add_seed_to_next) const {
  AGL_CHECK(!topo_.empty()) << "should construct topo first!";
  std::unordered_map<std::string, std::shared_ptr<UnitFrame>> e_to_unit_frame;
  std::unordered_map<std::string, std::vector<std::shared_ptr<UnitFrame>>>
      n2_to_unit_frame;

  for (auto& seed_t : ids) {
    auto& name = seed_t.first;
    auto& seed_vec = seed_t.second;
    auto seed_vec_ptr = std::make_shared<std::vector<IdDType>>(seed_vec);
    auto find = topo_.find(name);
    if (find == topo_.end()) continue;
    auto& n2_edge_name_vec = find->second;
    for (auto& pair : n2_edge_name_vec) {
      auto& n2_name = pair.first;
      auto& edge_name = pair.second;
      auto csr_adj = GetEdgeIndexCSR(edge_name);
      auto u_frame_ptr = std::make_shared<UnitFrame>(name, n2_name, edge_name,
                                                     seed_vec_ptr, csr_adj);
      // todo 后续考虑多线程（进程）处理。
      // 目前异构类型较少的情况下，直接for循环，init（）本身是多线程的
      u_frame_ptr->Init();
      e_to_unit_frame[edge_name] = u_frame_ptr;
      // 方便具有相同 dst 节点的 unit frame, 需要对 next seed nodes 做去重
      n2_to_unit_frame[n2_name].push_back(u_frame_ptr);
    }
  }

  auto frame_ptr = std::make_shared<Frame>(ids, e_to_unit_frame,
                                           n2_to_unit_frame, add_seed_to_next);
  return frame_ptr;
}

std::vector<std::shared_ptr<Frame>> SubGraph::GetEgoFrames(int hops,
                                                 bool add_seed_to_next) const {
  std::vector<std::shared_ptr<Frame>> res(hops);
  std::unordered_map<std::string, std::vector<IdDType>> seeds;
  // prepare seeds
  for(auto& root_pair: root_ids_) {
    auto& name = root_pair.first;
    auto& vec_root = root_pair.second;
    auto& vec_to_fill = seeds[name];
    for(auto& vec : vec_root) {
      for(auto index : vec){
        vec_to_fill.push_back(index);
      }
    }
  }

  for(int i=0; i< hops; ++i) {
    auto res_i = GetEdgeIndexOneHop(seeds, add_seed_to_next);
    res[i] = res_i;
    seeds = res_i->GetNextNodes();
  }
  return res;
}

}  // namespace agl