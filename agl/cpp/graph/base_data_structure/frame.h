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
#ifndef AGL_FRAME_H
#define AGL_FRAME_H
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

#include "base_data_structure/coo.h"
#include "base_data_structure/csr.h"

namespace agl {

// Neighborhoods that span only one hop based on a specific kind of edges with
// seed nodes of a certain type.
// contains:
// (1) seed nodes, (n1 nodes)
// (2) one hop edges (a certain kind) of those seed nodes
// (3) neighbor nodes of those seed nodes (maybe seed nodes for next hop)
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
  // init with follows:
  std::string n1_name_;
  std::string n2_name_;
  std::string edge_name_;
  std::shared_ptr<std::vector<IdDType>> seed_nodes_;  // n1 nodes
  std::shared_ptr<CSRAdj> edges_whole_;  // all edges with name edge_name_

  // generate follows:
  // unique neighbor nodes of seed nodes
  std::shared_ptr<std::vector<IdDType>> n2_nodes_;
  // neighbor edges of seed nodes spanned on $edge_name_$ edge
  std::shared_ptr<COO> edges_for_seeds_;
};

// One hop Graph with different type of seed nodes and edges.
// contains:
// (1) different kinds of seed nodes
// (2) one hop edges of those seed nodes
// (3) neighbor nodes of those seed nodes (maybe seed nodes for next hop)
class Frame {
 public:
  /***
   * usage: first, generate UnitFrame, record edge_name_to_unit_frame
   * and node2_name_to_unit_frame to merge different unit_frames
   *
   * @param seed_nodes : different kinds of seed nodes
   * @param e_to_uf : edge_name -> unit frame. for edge with type $edge name$,
   *                  we may have a unit frame.
   * @param n2_to_uf : node2_name -> unit frame.  Note different unit frame
   *                   may have same n2_name, we would merge them at first.
   * @param add_seed_to_next : bool, whether adding seed nodes as seed for next
   * hop
   */
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
