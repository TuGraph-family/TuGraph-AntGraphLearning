#include <pybind11/pybind11.h>
#include <pybind11/stl.h>

#include <sstream>
#include <iostream>
#include <memory>
#include <string>
#include <chrono>

#include "base_data_structure/coo.h"
#include "base_data_structure/csr.h"
#include "base_data_structure/dtype.h"
#include "base_data_structure/nd_array.h"
#include "common/safe_check.h"
#include "spec/feature_spec.h"
#include "spec/unit_spec.h"
#include "features/dense_feature_array.h"
#include "features/sparsekv_feature_array.h"
#include "features/sparsek_feature_array.h"
#include "sub_graph.h"
#include "common/thread_pool.h"
#include "py_api/tools/mutli_dense_parser.h"

namespace py = pybind11;
using namespace agl;

namespace {
using std::getline;
std::string PyBufferFormatAccodingAGLDtype(AGLDType dtype) {
  switch (dtype) {
    case INT8:
      return py::format_descriptor<int8_t>::format();
    case UINT8:
      return py::format_descriptor<uint8_t>::format();
    case UINT16:
      return py::format_descriptor<uint16_t>::format();
    case INT16:
      return py::format_descriptor<int16_t>::format();
    case UINT32:
      return py::format_descriptor<uint32_t>::format();
    case INT32:
      return py::format_descriptor<int32_t>::format();
    case UINT64:
      return py::format_descriptor<uint64_t>::format();
    case INT64:
      return py::format_descriptor<int64_t>::format();
    case FLOAT:
      return py::format_descriptor<float>::format();
    case DOUBLE:
      return py::format_descriptor<double>::format();
    default:
      AGL_CHECK(false) << " Not supported AGLDType:" << dtype;
      return 0;
  }
}

std::vector<std::shared_ptr<NDArray>> multi_dense_decode(
    std::vector<const char*>& pbs, std::vector<size_t>& pbs_length,
    char group_sep, char sep, int dim, AGLDType dtype) {

  std::vector<std::shared_ptr<NDArray>> final_result(pbs.size());

  for (size_t i = 0; i < pbs.size(); ++i) {
    // 第一次循环，找到element num
    std::string str(pbs[i], pbs_length[i]);
    std::istringstream iss(str);
    std::string token;
    int element_num = 0;
    while (std::getline(iss, token, group_sep)) {
      element_num++;
    }

    // 创建结果的 NDarray, 创建parser, 每个 element_num 一个 parser (会触发copy,
    // 有一些overhead)
    final_result[i] = std::make_shared<NDArray>(element_num, dim, dtype);
    auto* data = final_result[i]->data();
    std::vector<std::shared_ptr<StringToNumberParser>> parsers;
    // 第二次循环
    std::string str2(pbs[i], pbs_length[i]);
    std::istringstream iss2(str);
    std::string token2;
    int j = 0;
    while (std::getline(iss2, token2, group_sep)) {
      parsers.push_back(std::make_shared<StringToNumberParser>(token2, sep, dim,
                                                               j, data, dtype));
      j++;
    }

    ThreadPool pool(10);  // todo hard code 10 thread
    for (auto& parser : parsers) {
      pool.AddTask([&parser]() { parser->Parse(); });
    }
    pool.CloseAndJoin();
  }

  return final_result;
}

}  // namespace

PYBIND11_MODULE(pyagl, m) {
  // AGLDType
  py::enum_<AGLDType>(m, "AGLDType")
      .value("UNKNOWN", AGLDType::UNKNOWN)
      .value("INT8", AGLDType::INT8)
      .value("UINT16", AGLDType::UINT16)
      .value("INT16", AGLDType::INT16)
      .value("UINT32", AGLDType::UINT32)
      .value("INT32", AGLDType::INT32)
      .value("UINT64", AGLDType::UINT64)
      .value("INT64", AGLDType::INT64)
      .value("FLOAT", AGLDType::FLOAT)
      .value("DOUBLE", AGLDType::DOUBLE)
      .value("STR", AGLDType::STR)
      .value("UINT8", AGLDType::UINT8)
      .value("BF16", AGLDType::BF16)
      .value("FP16", AGLDType::FP16)
      .value("DType_INT_MIN_SENTINEL_DO_NOT_USE_",
             AGLDType::DType_INT_MIN_SENTINEL_DO_NOT_USE_)
      .value("DType_INT_MAX_SENTINEL_DO_NOT_USE_",
             AGLDType::DType_INT_MAX_SENTINEL_DO_NOT_USE_);

  // NDArray
  py::class_<NDArray, std::shared_ptr<NDArray>>(m, "NDArray",
                                                py::buffer_protocol())
      .def(py::init<int, int, AGLDType>())
      .def("GetRowNumber", &NDArray::GetRowNumber)
      .def("GetColNumber", &NDArray::GetColNumber)
      .def_buffer([](NDArray& nd) -> py::buffer_info {
        // todo zdl 考虑是否用buffer的方式支持zero copy，由于这些内容实际上是c++端subgraph的成员变量
        // 大概率是 readonly 的，而 torch tensor 没有只读的概念。 目前使用numpy array warpper 这个buffer的时候采用copy的方式
        // 也即 np.array(NDArray) 而没有使用 np.array(NDArray, copy=False)
        return py::buffer_info(
            nd.data(),                   /* Pointer to buffer */
            GetDtypeSize(nd.GetDType()), /* Size of one scalar */
            PyBufferFormatAccodingAGLDtype(
                nd.GetDType()), /* Python struct-style format descriptor */
            2,                  /* Number of dimensions */
            {nd.GetRowNumber(), nd.GetColNumber()}, /* Buffer dimensions */
            {GetDtypeSize(nd.GetDType()) *
                 nd.GetColNumber(), /* Strides (in bytes) for each index */
             GetDtypeSize(nd.GetDType())});
      })
      .def("FloatNd", [](std::shared_ptr<NDArray>& my_self) {
        // todo delete latter, just for test
        AGL_CHECK(my_self->GetDType() == AGLDType::FLOAT);
        auto* float_ptr = my_self->Flat<float>();
        for (size_t f_i = 0; f_i < my_self->GetRowNumber(); ++f_i) {
          for (size_t f_j = 0; f_j < my_self->GetColNumber(); ++f_j) {
            float_ptr[f_i * (my_self->GetColNumber()) + f_j] =
                f_i * (my_self->GetColNumber()) + f_j;
          }
        }
      });

  // struct CSR
  // todo：考虑各个数性是否要设置成 readwrite or readonly
  // 目前是 readonly， 因为这个 CSR只会在 c++创建并传递至python
  py::class_<CSR, std::shared_ptr<CSR>>(m, "CSR")
      .def(py::init<>())
      .def("Init", &CSR::Init)
      .def_readonly("row_num", &CSR::rows_nums_)
      .def_readonly("col_num", &CSR::col_nums_)
      .def_readonly("sorted", &CSR::sorted_)
      .def("GetIndPtr",
           [](std::shared_ptr<CSR>& myself) { return myself->ind_; })
      .def("GetIndices",
           [](std::shared_ptr<CSR>& myself) { return myself->indices_; })
      .def("GetData",
           [](std::shared_ptr<CSR>& myself) { return myself->data_; });

  // todo：考虑是否直接为 CSRAdj 添加相应的 get fcun
  py::class_<CSRAdj, std::shared_ptr<CSRAdj>>(m, "CSRAdj")
      .def_property_readonly("row_num", [](std::shared_ptr<CSRAdj>& myself){
        return myself->GetCSRNDArray()->rows_nums_;
      })
      .def_property_readonly("col_num", [](std::shared_ptr<CSRAdj>& myself){
        return myself->GetCSRNDArray()->col_nums_;
      })
      .def_property_readonly("sorted", [](std::shared_ptr<CSRAdj>& myself){
        return myself->GetCSRNDArray()->sorted_;
      })
      .def("GetIndPtr", [](std::shared_ptr<CSRAdj>& myself){
        return myself->GetCSRNDArray()->ind_;
      })
      .def("GetIndices", [](std::shared_ptr<CSRAdj>& myself){
        return myself->GetCSRNDArray()->indices_;
      })
      .def("GetData", [](std::shared_ptr<CSRAdj>& myself){
        return myself->GetCSRNDArray()->data_;
      })
      ;

  // struct COO
  // just pass data to python
  py::class_<COO, std::shared_ptr<COO>>(m, "COO")
      .def_readonly("row_num", &COO::rows_nums_)
      .def_readonly("col_num", &COO::col_nums_)
      .def_readonly("row_sorted", &COO::row_sorted_)
      .def_readonly("col_sorted", &COO::col_sorted_)
      .def("GetN1Indices", [](std::shared_ptr<COO>& myself) { return myself->row_; })
      .def("GetN2Indices", [](std::shared_ptr<COO>& myself) { return myself->col_; })
      .def("GetEdgeIndex", [](std::shared_ptr<COO>& myself) { return myself->data_; })
      ;

  py::class_<COOAdj, std::shared_ptr<COOAdj>>(m, "COOAdj")
      .def_property_readonly("row_num", [](std::shared_ptr<COOAdj>& myself) {
        return myself->GetCOONDArray()->row_; })
      .def_property_readonly("col_num",
                             [](std::shared_ptr<COOAdj>& myself) {
                               return myself->GetCOONDArray()->col_; })
      .def_property_readonly("row_sorted", [](std::shared_ptr<COOAdj>& myself) {
        return myself->GetCOONDArray()->row_sorted_;})
      .def_property_readonly("col_sorted", [](std::shared_ptr<COOAdj>& myself) {
        return myself->GetCOONDArray()->col_sorted_;})
      .def("GetN1Indices", [](std::shared_ptr<COOAdj>& myself) {
        return myself->GetCOONDArray()->row_;})
      .def("GetN2Indices", [](std::shared_ptr<COOAdj>& myself) {
        return myself->GetCOONDArray()->col_;})
      .def("GetEdgeIndex", [](std::shared_ptr<COOAdj>& myself) {
        return myself->GetCOONDArray()->data_;});

  // dense feature array
  py::class_<DenseFeatureArray, std::shared_ptr<DenseFeatureArray>>(m, "DenseFeatureArray")
      .def("GetFeatureArray", &DenseFeatureArray::GetFeatureArray);

  // sparse kv feature array
  // -> SparseKVNDArray
  py::class_<SparseKVNDArray, std::shared_ptr<SparseKVNDArray>>(m, "SparseKVNDArray")
      .def("GetIndOffset", [](std::shared_ptr<SparseKVNDArray>& myself) { return myself->ind_offset_; })
      .def("GetKeys", [](std::shared_ptr<SparseKVNDArray>& myself) { return myself->keys_; })
      .def("GetVals", [](std::shared_ptr<SparseKVNDArray>& myself) { return myself->values_; });

  // -> SparseKVFeatureArray
  py::class_<SparseKVFeatureArray, std::shared_ptr<SparseKVFeatureArray>>(m, "SparseKVFeatureArray")
      .def("Init", &SparseKVFeatureArray::Init)
      .def("GetFeatureArray", &SparseKVFeatureArray::GetFeatureArray);

  // sparse k feature
  // -> SparseKeyNDArray
  py::class_<SparseKeyNDArray, std::shared_ptr<SparseKeyNDArray>>(m, "SparseKeyNDArray")
      .def("GetIndOffset", [](std::shared_ptr<SparseKeyNDArray>& myself) { return myself->ind_offset_; })
      .def("GetKeys", [](std::shared_ptr<SparseKeyNDArray>& myself) { return myself->keys_; });

  // SparseKFeatureArray
  py::class_<SparseKFeatureArray, std::shared_ptr<SparseKFeatureArray>>(m, "SparseKFeatureArray")
      .def("Init", &SparseKFeatureArray::Init)
      .def("GetFeatureArray", &SparseKFeatureArray::GetFeatureArray);

  // DenseFeatureSpec
  py::class_<DenseFeatureSpec, std::shared_ptr<DenseFeatureSpec>>(
      m, "DenseFeatureSpec")
      .def(py::init<const std::string&, int, AGLDType>())
      .def("GetFeatureDtype", &DenseFeatureSpec::GetFeatureDtype)
      .def("GetDim", &DenseFeatureSpec::GetDim)
      .def("GetFeatureName", &DenseFeatureSpec::GetFeatureName);

  // SparseKVSpec
  py::class_<SparseKVSpec, std::shared_ptr<SparseKVSpec>>(m, "SparseKVSpec")
      .def(py::init<const std::string&, int, AGLDType, AGLDType>())
      .def("GetFeatureName", &SparseKVSpec::GetFeatureName)
      .def("GetMaxDim", &SparseKVSpec::GetMaxDim)
      .def("GetKeyDtype", &SparseKVSpec::GetKeyDtype)
      .def("GetValDtype", &SparseKVSpec::GetValDtype);

  // SparseKSpec
  py::class_<SparseKSpec, std::shared_ptr<SparseKSpec>>(m, "SparseKSpec")
      .def(py::init<const std::string&, int, AGLDType>())
      .def("GetFeatureName", &SparseKSpec::GetFeatureName)
      .def("GetMaxDim", &SparseKSpec::GetMaxDim)
      .def("GetKeyDtype", &SparseKSpec::GetKeyDtype);

  // node spec
  // todo : 考虑 Get 方法是否有必要，目前 unordered_map 通过 pybind11 隐式转换为
  // Python dict
  py::class_<NodeSpec, std::shared_ptr<NodeSpec>>(m, "NodeSpec")
      .def(py::init<const std::string&, AGLDType>())
      .def("AddDenseSpec", &NodeSpec::AddDenseSpec)
      .def("AddSparseKVSpec", &NodeSpec::AddSparseKVSpec)
      .def("AddSparseKSpec", &NodeSpec::AddSparseKSpec)
      .def("GetDenseFeatureSpec", &NodeSpec::GetDenseFeatureSpec)
      .def("GetSparseKVSpec", &NodeSpec::GetSparseKVSpec)
      .def("GetSparseKSpec", &NodeSpec::GetSparseKSpec)
      .def("GetNodeIdDtype", &NodeSpec::GetNodeIdDtype)
      .def("GetNodeName", &NodeSpec::GetNodeName);

  // edge spec
  // todo: 考虑是否有必要把 api 都透传到python 层
  py::class_<EdgeSpec, std::shared_ptr<EdgeSpec>>(m, "EdgeSpec")
      .def(py::init<const std::string, const std::shared_ptr<NodeSpec>&,
                    const std::shared_ptr<NodeSpec>&, AGLDType>())
      .def("AddDenseSpec", &EdgeSpec::AddDenseSpec)
      .def("AddSparseKVSpec", &EdgeSpec::AddSparseKVSpec)
      .def("AddSparseKSpec", &EdgeSpec::AddSparseKSpec)
      .def("GetDenseFeatureSpec", &EdgeSpec::GetDenseFeatureSpec)
      .def("GetSparseKVSpec", &EdgeSpec::GetSparseKVSpec)
      .def("GetSparseKSpec", &EdgeSpec::GetSparseKSpec)
      .def("GetEidDtype", &EdgeSpec::GetEidDtype)
      .def("GetN1Name", &EdgeSpec::GetN1Name)
      .def("GetN2Name", &EdgeSpec::GetN2Name)
      .def("GetEdgeName", &EdgeSpec::GetEdgeName);

  // subgraph
  py::class_<SubGraph, std::shared_ptr<SubGraph>>(m, "SubGraph")
      .def(py::init<>())
      .def("AddNodeSpec", &SubGraph::AddNodeSpec)
      .def("AddEdgeSpec", &SubGraph::AddEdgeSpec)
      .def(
          "CreateFromPB",
          [](std::shared_ptr<SubGraph>& myself,
             const std::vector<std::string>& lst, bool merge, bool uncompress) {
            std::vector<const char*> pb_char;
            std::vector<size_t> pb_byte_length;
            pb_char.reserve(lst.size());
            pb_byte_length.reserve(lst.size());
            for (const auto& item : lst) {
              pb_char.push_back(item.c_str());
              pb_byte_length.push_back(item.size());
            }
            myself->CreateFromPB(pb_char, pb_byte_length, merge, uncompress);
          }
          ,
          py::keep_alive<1, 2>()
              )
      // edge level get function adj and feature array
      .def("GetEdgeIndexCSR", &SubGraph::GetEdgeIndexCSR)
      .def("GetEdgeDenseFeatureArray", &SubGraph::GetEdgeDenseFeatureArray)
      .def("GetEdgeSparseKVArray", &SubGraph::GetEdgeSparseKVArray)
      .def("GetEdgeSparseKArray", &SubGraph::GetEdgeSparseKArray)
      // node level get function
      .def("GetNodeDenseFeatureArray", &SubGraph::GetNodeDenseFeatureArray)
      .def("GetNodeSparseKVArray", &SubGraph::GetNodeSparseKVArray)
      .def("GetNodeSparseKArray", &SubGraph::GetNodeSparseKArray)
      .def("GetRootIds", &SubGraph::GetRootIds)
      .def("GetNodeNumPerSample", &SubGraph::GetNodeNumPerSample)
      .def("GetEdgeNumPerSample", &SubGraph::GetEdgeNumPerSample)
      .def("GetEgoEdgeIndex", [](std::shared_ptr<SubGraph>& myself, int hops){
          py::gil_scoped_release release;
          auto res_frame = myself->GetEgoFrames(hops, true);
          AGL_CHECK(res_frame.size()==hops);
          std::vector<std::unordered_map<std::string, std::shared_ptr<COOAdj>>> res_coo_adj(hops);
          for(size_t i = 0; i<hops; ++i) {
            res_coo_adj[i] = res_frame[i]->GetCOOEdges();
          }
          py::gil_scoped_acquire acquire;
          return res_coo_adj;
      })
      .def("CreateFromPBBytesArray", [](std::shared_ptr<SubGraph>& myself, const
                                        py::list& lst, bool merge, bool uncompress){
        std::vector<const char*> pbs;
        std::vector<size_t> pbs_length;
        for(auto& byte_list: lst){
          if(PyByteArray_Check(byte_list.ptr())){
            pbs.emplace_back(PyByteArray_AsString(byte_list.ptr()));
            pbs_length.emplace_back(PyByteArray_Size(byte_list.ptr()));
          }
        }
        py::gil_scoped_release release;
        myself->CreateFromPB(pbs, pbs_length, merge, uncompress);
        py::gil_scoped_acquire acquire;
      } ,
          py::keep_alive<1, 2>()
           )
      .def("TestGetNodeDenseFeature", [](std::shared_ptr<SubGraph>& myself, const std::string& n_name, const std::string& f_name) {
        std::cout << "TestGetNodeDenseFeature, node name:" << n_name << ", f name:" << f_name << "\n";
        auto f_array_ptr = myself->GetNodeDenseFeatureArray(n_name, f_name);
        auto nd = f_array_ptr->GetFeatureArray();
        std::cout << "TestGetNodeDenseFeature nd dtype:" << nd->GetDType() << "\n";
        return nd;
      })
        .def("TestString", [](std::shared_ptr<SubGraph>& myself, const
     std::vector<std::string>& lst) { std::cout<< "vector size：" << lst.size()
     << "\n";
        })
      .def("TestBytesArray", [](std::shared_ptr<SubGraph>& myself, const
                                py::list& lst){
          std::vector<const char*> pbs;
          std::vector<size_t> pbs_length;
          for(auto& byte_list: lst){
            if(PyByteArray_Check(byte_list.ptr())){
              pbs.emplace_back(PyByteArray_AsString(byte_list.ptr()));
              pbs_length.emplace_back(PyByteArray_Size(byte_list.ptr()));
            }
          }
          std::cout<< "pbs size:" << pbs.size() << "\n";
          std::cout<<"test string:"<<std::string(pbs[0], pbs_length[0]) << "\n";
      })
      ;

  // m.def("multi_dense_decode", &multi_dense_decode);
  m.def(
      "multi_dense_decode_bytes",
      [](const py::list& lst, char group_sep, char sep, int dim, AGLDType dtype) {
        std::vector<const char*> pbs;
        std::vector<size_t> pbs_length;
        for (auto& byte_list : lst) {
          if (PyByteArray_Check(byte_list.ptr())) {
            pbs.emplace_back(PyByteArray_AsString(byte_list.ptr()));
            pbs_length.emplace_back(PyByteArray_Size(byte_list.ptr()));
          }
        }
        auto result =
            multi_dense_decode(pbs, pbs_length, group_sep, sep, dim, dtype);
        return result;
      });

  m.def(
      "multi_dense_decode_string",
      [](const std::vector<std::string>& lst, char group_sep, char sep, int dim,
         AGLDType dtype) {
        std::vector<const char*> pb_char;
        std::vector<size_t> pb_byte_length;
        pb_char.reserve(lst.size());
        pb_byte_length.reserve(lst.size());
        for (const auto& item : lst) {
          pb_char.push_back(item.c_str());
          pb_byte_length.push_back(item.size());
        }
        auto result = multi_dense_decode(pb_char, pb_byte_length, group_sep,
                                         sep, dim, dtype);
        return result;
      });
}