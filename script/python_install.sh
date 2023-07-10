#!/usr/bin/env bash

set -e
SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}"); pwd)
pushd ${SCRIPT_DIR}/../
WHEEL_PKG=$(find ./dist -iname agl-*.whl)
pip install $WHEEL_PKG
popd