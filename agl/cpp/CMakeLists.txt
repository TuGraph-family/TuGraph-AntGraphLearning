set(CMAKE_POSITION_INDEPENDENT_CODE ON)

FILE(GLOB SRCS *.cc */*.cc */*/*.cc)

add_library(agl SHARED ${SRCS})
target_link_libraries(agl
        ${DEPENDENCE_DYNAMIC_LIB}
        -Wl,-whole-archive ${DEPENDENCE_STATIC_LIB} -Wl,-no-whole-archive
        )
target_include_directories(agl PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${PYBIND11_INCLUDE_DIR}
        )
target_link_directories(agl PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        )