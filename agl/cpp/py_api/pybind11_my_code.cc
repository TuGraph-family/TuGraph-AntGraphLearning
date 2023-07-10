#include <pybind11/pybind11.h>
#include <iostream>
#include <string>
#include <chrono>
#include "graph/sub_graph.h"
using namespace pybind11::literals;
namespace py = pybind11;

void print_list(const py::list& lst) {
  auto* sg = new agl::SubGraph();
  auto start = std::chrono::high_resolution_clock::now(); // 记录开始时间
  int i = 0;
  for (const auto& item : lst) {
    std::string str = py::cast<std::string>(item);
    if(i==0) {
      std::cout << "String: Address: " << &str
                << ", Python Object Address: " << item.ptr() << std::endl;
    }
    i++;
  }
  std::cout << "Python List Address: " << lst.ptr() << std::endl;  // 打印Python对象的内存地址
  auto end = std::chrono::high_resolution_clock::now(); // 记录结束时间
  auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end - start); // 计算时间差
  std::cout << "操作耗时 " << duration.count() << " 微秒" << std::endl; // 输出时间差
}

PYBIND11_MODULE(pyaglt, m) {
  m.doc() = "pybind11 pre test api of agl";
  m.def("print_list", &print_list);
}