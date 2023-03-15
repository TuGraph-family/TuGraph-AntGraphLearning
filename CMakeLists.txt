cmake_minimum_required(VERSION 3.5)
project(agl C CXX)

# for *.so/*.a/binary
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/output/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/output/lib)
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/output/bin)

# settings

set(CMAKE_CXX_STANDARD 17)

if (NOT PYBIND11_INCLUDE_DIR)
    message(FATAL_ERROR "specify -DPYBIND11_INCLUDE_DIR=/your_pybind11_dir/")
endif()

add_subdirectory(agl/cpp)