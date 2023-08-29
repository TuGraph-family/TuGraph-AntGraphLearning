#!/usr/bin/env bash

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}"); pwd)
PROJECT_DIR="${SCRIPT_DIR}/../"
TEST_RESULT_DIR="$PROJECT_DIR"/testresult

test ! -d ${TEST_RESULT_DIR} && mkdir -p "$TEST_RESULT_DIR"
test -f ${TEST_RESULT_DIR}/fail_ut.log && rm ${TEST_RESULT_DIR}/fail_ut.log

set -euo pipefail

source ${PROJECT_DIR}/third_party/common.sh

pip install coverage -i http://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com
pip install unittest-xml-reporting

exitcode=0
pushd ${PROJECT_DIR}/
for file in $(find agl -type f -name "*_test.py"); do
  name=$(echo ${file} | tr '/' '.')
  echo ${name} | grep ".py$" 1>/dev/null 2>&1
  if [[ $? == 0 ]]; then
    len=${#name}-3
    name=${name:0:len}
  fi
  logfile="$TEST_RESULT_DIR/${name}.log"

  LOG_INFO "Run test: ${name}"
  # shellcheck disable=SC2046
  set +e
  coverage run --branch --source "${PROJECT_DIR}"/$(dirname "${file}") --omit *_test.py -p \
    -m xmlrunner discover -s "${PROJECT_DIR}"/$(dirname "${file}") \
    -p $(basename "${file}") -o "$TEST_RESULT_DIR" 2>&1 | tee $logfile
  RET=${PIPESTATUS[0]}
  set -e
  if [[ $RET -ne 0 ]]; then
    exitcode=1
    LOG_ERROR "${name} Fatal Error, Please refer to file: ${logfile}"
    echo "${name} failed, log in ${logfile}\n" >> ${TEST_RESULT_DIR}/fail_ut.log
  else
    LOG_INFO "${name} Success"
  fi
done

coverage combine
coverage report 2>&1 | tee "$TEST_RESULT_DIR/coverage.report.log"
coverage xml -o "$TEST_RESULT_DIR"/cobertura.xml
popd &>/dev/null

echo "Search \"Fatal Error\" if failed"
set -e
exit $exitcode