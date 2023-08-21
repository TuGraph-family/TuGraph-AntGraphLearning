#!/usr/bin/env bash

base=$(dirname "$0")
base=$(
  cd "$base"
  pwd
)
cd ${base}

source ${base}/common.sh
set -e

#download_src() {
#  package=$1
#  dst_dir=$2
#  wget -q http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/user/agl_deps_src/$package -O $dst_dir/$package
#}

boost_dir=${base}/boost
if [ ! -e ${boost_dir}/boost_install/success ]; then
  echo "start to compile boost"

  # rpm -qa | grep -qw python3-devel || yum install python3-devel -y >/dev/null 2>&1
  # apt-get install python3-devel -y >/dev/null 2>&1

  #C_INCLUDE_PATH=$C_INCLUDE_PATH:/usr/include/python3.6m
  #export C_INCLUDE_PATH
  #CPLUS_INCLUDE_PATH=$CPLUS_INCLUDE_PATH:/usr/include/python3.6m
  #export CPLUS_INCLUDE_PATH

  if [ -d ${boost_dir} ]; then
    rm -rf ${boost_dir}
  fi
  mkdir ${boost_dir}
  cd ${boost_dir}
  boost_untar_dir="${boost_dir}/boost_1_72_0"
  boost_tar="boost_1_72_0.tar.gz"
  #download_src ${boost_tar} ./
  if [ -f /agl_resource/boost_1_72_0.tar.gz ]; then
    echo ">>>>>>>>>>> use /agl_resource/boost_1_72_0.tar.gz"
    cp /agl_resource/boost_1_72_0.tar.gz boost_1_72_0.tar.gz
  else
    wget -q https://boostorg.jfrog.io/artifactory/main/release/1.72.0/source/boost_1_72_0.tar.gz -O ${boost_tar}
  fi
  tar -zxf ${boost_tar}
  rm ${boost_tar}
  cd ${boost_untar_dir}
  sh bootstrap.sh --prefix=${boost_dir}/boost_install
  ./b2 cxxflags=-fPIC -a -j8 -q --prefix=${boost_dir}/boost_install \
    link=static runtime-link=shared \
    install
  cd ..
  rm -rf ${boost_untar_dir}
  touch ${boost_dir}/boost_install/success
  cd ${base}
fi
echo $'boost\t\t ok'


gtest_dir="${base}/googletest"
if [ ! -e "${gtest_dir}/success" ]; then
  echo "start to compile googletest"
  if [ -d ${gtest_dir} ]; then
    rm -rf ${gtest_dir}
  fi
  mkdir -p ${gtest_dir}
  cd ${gtest_dir}
  if [ -f /agl_resource/googletest.tar.gz ]; then
    echo ">>>>>>>>>>> use /agl_resource/googletest.tar.gz"
    cp /agl_resource/googletest.tar.gz googletest.tar.gz
  else
    wget -q https://github.com/google/googletest/archive/release-1.8.1.tar.gz -O googletest.tar.gz
  fi
  tar -zxf googletest.tar.gz
  mv googletest-release-1.8.1 googletest_source

  cd googletest_source
  cmake \
    -DCMAKE_CXX_STANDARD:STRING="11" \
    -DCMAKE_INSTALL_PREFIX:PATH=${gtest_dir} \
    -DBUILD_SHARED_LIBS=OFF \
    .
  make -j8
  make install
  if [[ -d "${gtest_dir}/lib64" && ! -d "${gtest_dir}/lib" ]]; then
    cp -r "${gtest_dir}/lib64" "${gtest_dir}/lib"
  fi
  rm ${gtest_dir}/googletest.tar.gz
  touch ${gtest_dir}/success
  cd ${base}
fi
echo $'gtest\t\t ok'

PB_NS="agl_protobuf"
protobuf_dir=${base}/protobuf
if [ ! -e ${protobuf_dir}/protobuf_install/success ]; then
  echo "start to compile protobuf"
  if [ -d ${protobuf_dir} ]; then
    rm -rf ${protobuf_dir}
  fi
  mkdir ${protobuf_dir}
  cd ${protobuf_dir}
  protobuf_url="http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/open_agl_deps%2Fprotobuf%2Fv3.20.3.tar.gz"
  # "https://github.com/protocolbuffers/protobuf/archive/refs/tags/v3.20.3.tar.gz" # network problem
  protobuf_untar_dir="${protobuf_dir}/protobuf-3.20.3"
  if [ -f /agl_resource/protobuf.tar.gz ]; then
    echo ">>>>>>>>>>> use /agl_resource/protobuf.tar.gz"
    cp /agl_resource/protobuf.tar.gz protobuf.tar.gz
  else
    wget ${protobuf_url} -O protobuf.tar.gz
  fi
  tar -zxf protobuf.tar.gz
  rm protobuf.tar.gz
  cd ${protobuf_untar_dir}

  mkdir -p ${protobuf_dir}/protobuf_install/build
  cd ${protobuf_dir}/protobuf_install/build

  cmake -Dprotobuf_BUILD_SHARED_LIBS:BOOL=OFF \
    -Dprotobuf_BUILD_TESTS:BOOL=OFF \
    -Dprotobuf_WITH_ZLIB:BOOL=ON \
    -Dprotobuf_MSVC_STATIC_RUNTIME:BOOL=OFF \
    -DCMAKE_INSTALL_PREFIX:PATH=${protobuf_dir}/protobuf_install \
    -DCMAKE_POSITION_INDEPENDENT_CODE:BOOL=ON \
    -DCMAKE_CXX_STANDARD=11 \
    -DCMAKE_CXX_FLAGS="-Dprotobuf=${PB_NS}" \
    ${protobuf_untar_dir}/cmake
  make -j8
  make install
  ln -s ${protobuf_dir}/protobuf_install/lib64 ${protobuf_dir}/protobuf_install/lib
  touch ${protobuf_dir}/protobuf_install/success
  rm -rf ${protobuf_untar_dir}
  cd ${base}
fi
echo $'protobuf\t ok'
