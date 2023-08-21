#!/usr/bin/env bash

GCC_VERSION=9.4.0
PY_VERSION=3.8
CUDA_VERSION=11.8
PYTORCH_VERSION=2.0.1


download_resource() {
  # In case your network can not access those resource, we pack it into docker image
  # this script is used download those resource before building docker image

  # boost, version: 1_72_0
  wget https://boostorg.jfrog.io/artifactory/main/release/1.72.0/source/boost_1_72_0.tar.gz -O boost_1_72_0.tar.gz

  # googletest, version: 1.8.1
  wget https://github.com/google/googletest/archive/release-1.8.1.tar.gz -O googletest.tar.gz

  # protobuf, version: v3.20.3
  wget https://github.com/protocolbuffers/protobuf/archive/refs/tags/v3.20.3.tar.gz -O protobuf.tar.gz

  # todo change to open link
  # spark 3.1.1
  wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/spark-3.1.1-bin-hadoop3.2.tgz -O spark-3.1.1-bin-hadoop3.2.tgz
  # maven 3.6.1
  wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/apache-maven-3.6.1-bin.tar.gz -O apache-maven-3.6.1-bin.tar.gz
  # jdk1.8.0_202
  wget http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/jdk-8u202-linux-x64.tar.gz -O jdk-8u202-linux-x64.tar.gz
}

final_tag=agl-gcc${GCC_VERSION}-py${PY_VERSION}-cuda${CUDA_VERSION}-pytorch${PYTORCH_VERSION}

# todo change BASE_IMAGE as image
BASE_IMAGE=agl_image

image_name=${BASE_IMAGE}:${final_tag}

echo "agl image name " ${image_name}

download_resource

docker build --net=host -f Dockerfile.gpu -t ${image_name} .

