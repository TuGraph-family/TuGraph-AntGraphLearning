ExternalProject_Add(protobuf
        PREFIX ${CMAKE_CURRENT_SOURCE_DIR}/third_party/output/protobuf
        SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/protobuf
        CMAKE_ARGS -DCMAKE_CXX_FLAGS="-Dprotobuf=${PROTOBUF_NS}" -DCMAKE_POSITION_INDEPENDENT_CODE:BOOL=ON
        )