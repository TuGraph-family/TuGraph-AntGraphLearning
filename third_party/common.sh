#!/usr/bin/env bash

function LOG_ERROR() {
  echo "[$(date +%F_%T)][ERROR] $@"
}

function LOG_INFO() {
  echo "[$(date +%F_%T)][INFO] $@"
}

function find_gcc_bin_dir() {
  if [[ $# -lt 1 ]]; then
    LOG_ERROR "Need parameter:'gcc_version', Usage: find_gcc 5.3.0"
    exit 1
  fi
  TARGET_VER=$1
  LEN=${#TARGET_VER} # get length

  GCC_LIST="/usr/bin/gcc /usr/local/bin/gcc /usr/local/gcc-${TARGET_VER}/bin/gcc"
  for GCC_BIN in ${GCC_LIST}; do
    if [[ ! -f ${GCC_BIN} ]]; then
      continue
    fi
    set +e
    GCC_VER=$(echo "$GCC_BIN -v" | bash 2>&1 | grep "gcc version" | awk '{print $3}')
    set -e
    if [[ "${GCC_VER:0:$LEN}" == "${TARGET_VER}" ]]; then
      dirname "${GCC_BIN}"
      return
    fi
  done
  LOG_ERROR "Can not find $TARGET_VER in $GCC_LIST"
  exit 1
}

function init_cmake3() {
  # cmake version
  CMAKE_MAJOR_VERSION=$(cmake --version | grep version | awk '{print $3}' | cut -d "." -f 1)
  if [[ ${CMAKE_MAJOR_VERSION} -eq 2 ]]; then
    pip install cmake
  fi
}

function show_env() {
  LOG_INFO "############### ENV Checker  ############### "
  rpm -qa | grep -qw gcc-c++ || yum install gcc-c++ -y >/dev/null 2>&1
  rpm -qa | grep -qw python3-devel || yum install python3-devel -y >/dev/null 2>&1
  rpm -qa | grep -qw libidn-devel || yum install libidn-devel -y >/dev/null 2>&1
  gcc -march=native -Q --help=target | grep march
  gcc -v
  uname -a
  lscpu
  LOG_INFO "############### ENV Checker  ############### "
}