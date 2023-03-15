include(ExternalProject)
ExternalProject_Get_property(protobuf BINARY_DIR)

add_custom_target(pb_gen bash "-c" "PROTOC_BIN_DIR=${BINARY_DIR} bash src_gen.sh"
        WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}/agl/proto/)
add_dependencies(pb_gen protobuf)
