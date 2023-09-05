#!/usr/bin/env bash

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}"); pwd)
PROJECT_DIR="${SCRIPT_DIR}/../"
set -euo pipefail
pushd ${PROJECT_DIR}
./output/bin/unit_test
popd
