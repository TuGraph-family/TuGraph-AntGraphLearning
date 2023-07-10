cmake_minimum_required(VERSION 3.5)
project(agl C CXX)

# for *.so/*.a/binary
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/output/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/output/lib)
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/output/bin)

# settings
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(PB_NS agl_protobuf)
set(CMAKE_BUILD_TYPE RelWithDebInfo) # Debug/Release
# set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -D__FILENAME__='\"$(notdir $<)\"'")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -pthread -lpthread -Dprotobuf=${PB_NS}")

# cmake utils
include(cmake/submodule.cmake)
include(cmake/pybind11.cmake)
include(cmake/protobuf.cmake)

add_subdirectory(agl/cpp)
add_subdirectory(test)
