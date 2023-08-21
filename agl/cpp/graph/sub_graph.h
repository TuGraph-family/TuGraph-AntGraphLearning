/**
 * Copyright 2023 AntGroup CO., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
#ifndef AGL_SUB_GRAPH_H
#define AGL_SUB_GRAPH_H
#include <memory>
#include <string>
#include <unordered_map>

#include "base_data_structure/frame.h"
#include "edge_unit.h"
#include "node_unit.h"
#include "spec/unit_spec.h"

namespace agl {
// a container contains different type nodes, edges, and their features.
// you may use it as follows:
//  auto sg_ptr = unique_ptr<SubGraph>(new SubGraph());
//  sg_ptr->AddNodeSpec(n_name, node_spec);
//  sg_ptr->AddEdgeSpec(e_name, edge_spec);
//  sg_ptr->CreateFromPB(xxx)
// Then you can use get function to get information like adj,
// node feature, edge feature from it.
class SubGraph {
 public:
  void AddNodeSpec(const std::string& n_name,
                   const std::shared_ptr<NodeSpec>& spec);
  void AddEdgeSpec(const std::string& e_name,
                   const std::shared_ptr<EdgeSpec>& spec);

  /**
   * create subgraph from pb strings
   * @param pbs : protobuf strings,  use vector<char*> to avoid copy
   * @param pb_length : length for each pb strings
   * @param merge : now only support disjoint merge. that is merge = false
   * @param uncompress : whether we should uncompress those strings at first
   */
  void CreateFromPB(const std::vector<const char*>& pbs,
                    const std::vector<size_t>& pb_length, bool merge,
                    bool uncompress);

  // a set of get functions
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
  void CreateFromPBNonMerge(const std::vector<const char*>& pbs,
                            const std::vector<size_t>& pb_length,
                            bool uncompress);

 private:
  bool is_ego_;
  int ego_hops_;

  bool merge_ = false;
  // node and edge unit, features are stored in continuous memory blocks,
  // and IDs serve as the subscript of features
  // todo(zdl) Now we don't store either raw IDs or ID mappings.
  // node name -> node unit
  std::unordered_map<std::string, std::shared_ptr<NodeUint>> nodes_;
  // edge name -> edge unit
  std::unordered_map<std::string, std::shared_ptr<EdgeUint>> edges_;

  // root IDs:
  // n_name -> sample (pb) -> root IDs
  std::unordered_map<std::string, std::vector<std::vector<IdDType>>> root_ids_;

  // node and edge spec， for parsing protos and related valid check
  std::unordered_map<std::string, std::shared_ptr<NodeSpec>> node_specs_;
  std::unordered_map<std::string, std::shared_ptr<EdgeSpec>> edge_specs_;
  // topo node1_name <- {{node_2_name1, edge_name1}, {node_2_nameXXX,
  // edge_nameXXX}}}
  std::unordered_map<std::string,
                     std::vector<std::pair<std::string, std::string>>>
      topo_;

  // if merge_ == false，
  // provide node/edge num per sample
  std::unordered_map<std::string, std::vector<int>> n_num_per_sample_;
  std::unordered_map<std::string, std::vector<int>> e_num_per_sample_;
};

}  // namespace agl

#endif  // AGL_SUB_GRAPH_H
