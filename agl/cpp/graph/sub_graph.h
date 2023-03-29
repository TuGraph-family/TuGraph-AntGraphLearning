#ifndef AGL_SUB_GRAPH_H
#define AGL_SUB_GRAPH_H
#include <string>
#include <unordered_map>
#include <memory>

class UnitGraph;

namespace agl {
class SubGraph {
 private:
  std::unordered_map<std::string, std::shared_ptr<UnitGraph>> heter_graphs;

 public:
};

}  // namespace agl

#endif  // AGL_SUB_GRAPH_H
