include(ExternalProject)
ExternalProject_Add(protobuf
        PREFIX ${PROJECT_SOURCE_DIR}/third_party/output/protobuf
        SOURCE_DIR ${PROJECT_SOURCE_DIR}/third_party/protobuf
        SOURCE_SUBDIR cmake
        CMAKE_ARGS -DCMAKE_CXX_FLAGS="-Dprotobuf=${PB_NS}"
                   -DCMAKE_POSITION_INDEPENDENT_CODE:BOOL=ON
                   -Dprotobuf_BUILD_TESTS=OFF
                   -DCMAKE_INSTALL_PREFIX:PATH=${PROJECT_SOURCE_DIR}/third_party/output/protobuf
        )
install()
