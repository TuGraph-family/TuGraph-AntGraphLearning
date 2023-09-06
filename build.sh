#!/usr/bin/env bash

# add safe.directory
git config --global --add safe.directory /graph_ml

set -e

PROJECT_DIR=$(cd "$(dirname $0)" && pwd)
TEST_RESULT_DIR="$PROJECT_DIR"/testresult
test ! -d ${TEST_RESULT_DIR} && mkdir -p "$TEST_RESULT_DIR"

# step 1： build some dependency
bash "${PROJECT_DIR}"/third_party/dependency.sh

# step 2： cmake and make
bash "${PROJECT_DIR}"/script/cpp_build.sh

# setp 3: build wheel
time bash "${PROJECT_DIR}"/script/python_build.sh

# and install wheel
time bash "${PROJECT_DIR}"/script/python_install.sh

# cpp and python ut
time bash "${PROJECT_DIR}"/script/cpp_ut.sh 2>&1 | tee "$TEST_RESULT_DIR"/cpp_ut.log

time bash "${PROJECT_DIR}"/script/python_ut.sh 2>&1 | tee "$TEST_RESULT_DIR"/python_ut.log
