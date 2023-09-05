#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}"); pwd)

cd "${SCRIPT_DIR}"/..
# prepare compile dir
test -d ./build && rm -rf ./build
mkdir -p ./build
# compile
cd build
cmake ..
time make -j
cd ..