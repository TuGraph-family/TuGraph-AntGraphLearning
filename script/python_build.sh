#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}"); pwd)

pushd "${SCRIPT_DIR}"/..
test -d dist && rm -rf dist
python setup.py bdist_wheel
test -d build && rm -rf build
test -d agl.egg-LOG_INFO && rm -rf agl.egg-LOG_INFO
popd