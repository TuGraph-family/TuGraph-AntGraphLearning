#include <pybind11/pybind11.h>
using namespace pybind11::literals;

int add(int i = 1, int j = 2) {
    return i + j;
}

PYBIND11_MODULE(pyagl, m) {
    m.doc() = "pybind11 api of agl";
    m.def("add2", &add, "i"_a=1, "j"_a=2);
}