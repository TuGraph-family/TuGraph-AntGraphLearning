set(CMAKE_POSITION_INDEPENDENT_CODE ON)
find_package (PythonLibs 3.6)

message(STATUS "PYTHON_LIBRARIES = ${PYTHON_LIBRARIES}")
# libpb_cpp.a
include(ExternalProject)
ExternalProject_Get_property(protobuf BINARY_DIR)
ExternalProject_Get_property(protobuf SOURCE_DIR)
ExternalProject_Get_property(protobuf INSTALL_DIR)
set(protobuf_BINARY_DIR ${BINARY_DIR})
set(protobuf_SOURCE_DIR ${SOURCE_DIR})
set(protobuf_INSTALL_DIR ${INSTALL_DIR})
message(STATUS "bindir:${protobuf_BINARY_DIR}, srcdir:${protobuf_SOURCE_DIR}, install_dir:${protobuf_INSTALL_DIR}")

set(PROTO_FILE_DIR "${PROJECT_SOURCE_DIR}/agl/proto")
FILE(GLOB PROTO_FILES "${PROTO_FILE_DIR}/*.proto")

if(NOT EXISTS ${PROJECT_SOURCE_DIR}/agl/cpp/proto)
    # 如果不存在 cpp/proto 目录，则创建该目录
    FILE(MAKE_DIRECTORY ${PROJECT_SOURCE_DIR}/agl/cpp/proto)
endif()

FOREACH (PROTO_FILE ${PROTO_FILES})
    STRING(REGEX REPLACE "[.]proto$" ".pb.cc" OUTPUT_SOURCE ${PROTO_FILE})
    STRING(REGEX REPLACE "agl/proto" "agl/cpp/proto" OUTPUT_SOURCE ${OUTPUT_SOURCE})
    message("${PROTO_FILE} -> ${OUTPUT_SOURCE}")
    list(APPEND OUTPUT_SOURCES ${OUTPUT_SOURCE})
ENDFOREACH ()

add_custom_command(OUTPUT ${OUTPUT_SOURCES}
        COMMAND ${protobuf_INSTALL_DIR}/bin/protoc -I ${PROTO_FILE_DIR} --cpp_out=${CMAKE_CURRENT_SOURCE_DIR}/proto ${PROTO_FILES}
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        DEPENDS protobuf ${PROTO_FILES})
add_library(pb_cpp STATIC ${OUTPUT_SOURCES})
target_include_directories(
        pb_cpp PUBLIC
        ${protobuf_INSTALL_DIR}/include
)
target_link_directories(
        pb_cpp PUBLIC
        ${protobuf_INSTALL_DIR}/lib64
)

set(THIRDPARTY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../third_party")

set(DEPENDENCE_DYNAMIC_LIB dl z)

# libagl.so
FILE(GLOB SRCS *.cc */*.cc */*/*.cc)
list(FILTER SRCS EXCLUDE REGEX "proto/.*")
set(STATIC_LIB_DEPS pb_cpp libprotobuf.a libboost_thread.a libboost_chrono.a
        libboost_stacktrace_addr2line.a)
add_library(agl SHARED ${SRCS})
target_link_libraries(agl PUBLIC
        ${DEPENDENCE_DYNAMIC_LIB}
        ${PYTHON_LIBRARIES}
        -Wl,-whole-archive ${STATIC_LIB_DEPS} -Wl,-no-whole-archive
        )
target_include_directories(
        agl PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/graph/
        ${CMAKE_CURRENT_SOURCE_DIR}/graph/spec
        ${CMAKE_CURRENT_SOURCE_DIR}/graph/base_data_structure
        ${CMAKE_CURRENT_SOURCE_DIR}/graph/features
        ${THIRDPARTY_DIR}/boost/boost_install/include
        ${THIRDPARTY_DIR}/output/protobuf/include
        ${CMAKE_CURRENT_SOURCE_DIR}/py_api/tools/
        ${pybind11_INCLUDE_DIRS}
)
target_link_directories(
        agl PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${THIRDPARTY_DIR}/boost/boost_install/lib
        ${THIRDPARTY_DIR}/output/protobuf/lib64
)
add_dependencies(agl pb_cpp protobuf)

