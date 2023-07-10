#!/usr/bin/env bash

base=$(dirname "$0")
base=$(
  cd "$base"
  pwd
)
cd ${base}

source ${base}/common.sh
set -e

download_src() {
  package=$1
  dst_dir=$2
  wget -q http://alps-common.oss-cn-hangzhou-zmf.aliyuncs.com/user/agl_deps_src/$package -O $dst_dir/$package
}

boost_dir=${base}/boost
if [ ! -e ${boost_dir}/boost_install/success ]; then
  echo "start to compile boost"

  rpm -qa | grep -qw python3-devel || yum install python3-devel -y >/dev/null 2>&1

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
  download_src ${boost_tar} ./
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
  if [ -f ${base}/pre_download/googletest.tar.gz ]; then
    cp ${base}/pre_download/googletest.tar.gz googletest.tar.gz
  else
    # wget -q https://github.com/google/googletest/archive/release-1.8.1.tar.gz -O googletest.tar.gz
    download_src googletest.tar.gz ./
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