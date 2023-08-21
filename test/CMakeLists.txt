cmake_minimum_required(VERSION 3.5)
enable_testing()
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_STANDARD 11)

set(CMAKE_CXX_LINK_FLAGS "${CMAKE_CXX_LINK_FLAGS} -pthread -lpthread -O0 ")
set(CPP_BASE_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../agl/cpp")
set(CPP_BASE_TEST_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../test")
set(THIRDPARTY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../third_party")
#set(THIRDPARTY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../third_party")
set(protobuf_INSTALL_DIR "${THIRDPARTY_DIR}/protobuf/protobuf_install")

include_directories(
        ${CPP_BASE_SRC_DIR}
        ${CPP_BASE_TEST_DIR}
        ${CPP_BASE_SRC_DIR}/graph/
        ${CPP_BASE_SRC_DIR}/graph/spec
        ${CPP_BASE_SRC_DIR}/graph/base_data_structure
        ${CPP_BASE_SRC_DIR}/graph/features
        ${THIRDPARTY_DIR}/boost/boost_install/include
        #${THIRDPARTY_DIR}/output/protobuf/include
        ${protobuf_INSTALL_DIR}/include
        ${THIRDPARTY_DIR}/googletest/include
        ${pybind11_INCLUDE_DIRS}
)
message(STATUS "CPP_BASE_SRC_DIR: ${CPP_BASE_SRC_DIR}, CPP_BASE_TEST_DIR:${CPP_BASE_TEST_DIR}, pybind11_INCLUDE_DIRS:${pybind11_INCLUDE_DIRS}, CMAKE_LIBRARY_OUTPUT_DIRECTORY: ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

link_directories(
        ${THIRDPARTY_DIR}/googletest/lib
        ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
)

set(DEPENDENCE_DYNAMIC_LIB dl z)

FILE(GLOB SRCS *.cc */*.cc */*/*.cc */*/*/*.cc */*/*/*/*.cc)
message("TEST SRCS:\n${SRCS}")
add_executable(unit_test ${SRCS})
target_link_libraries(unit_test gtest_main gtest -lagl ${DEPENDENCE_DYNAMIC_LIB})
add_dependencies(unit_test pb_cpp agl)
add_test(unit_test unit_test)


