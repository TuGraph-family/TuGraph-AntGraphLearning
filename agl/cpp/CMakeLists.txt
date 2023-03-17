set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# libpb_cpp.a
include(ExternalProject)
ExternalProject_Get_property(protobuf BINARY_DIR)
set(PROTO_FILE_DIR "${PROJECT_SOURCE_DIR}/agl/proto")
FILE(GLOB PROTO_FILES "${PROTO_FILE_DIR}/*.proto")
FOREACH (PROTO_FILE ${PROTO_FILES})
    STRING(REGEX REPLACE "[.]proto$" ".pb.cc" OUTPUT_SOURCE ${PROTO_FILE})
    STRING(REGEX REPLACE "agl/proto" "agl/cpp/proto" OUTPUT_SOURCE ${OUTPUT_SOURCE})
    message("${PROTO_FILE} -> ${OUTPUT_SOURCE}")
    list(APPEND OUTPUT_SOURCES ${OUTPUT_SOURCE})
ENDFOREACH ()

add_custom_command(OUTPUT ${OUTPUT_SOURCES}
        COMMAND ${BINARY_DIR}/protoc -I ${PROTO_FILE_DIR} --cpp_out=${CMAKE_CURRENT_SOURCE_DIR}/proto ${PROTO_FILES} 
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        DEPENDS protobuf ${PROTO_FILES})
add_library(pb_cpp STATIC ${OUTPUT_SOURCES})

# libagl.so
FILE(GLOB SRCS *.cc */*.cc */*/*.cc)
list(FILTER SRCS EXCLUDE REGEX "proto/.*")
set(STATIC_LIB_DEPS pb_cpp)
add_library(agl SHARED ${SRCS})
target_link_libraries(agl 
    -Wl,-whole-archive ${STATIC_LIB_DEPS} -Wl,-no-whole-archive
)
target_include_directories(
        agl PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${pybind11_INCLUDE_DIRS}
)
target_link_directories(
        agl PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
)
add_dependencies(agl pb_cpp protobuf)

