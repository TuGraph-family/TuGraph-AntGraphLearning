#!/usr/bin/env bash

# NOTICE: this is for an older docker version on Linux.

set -e
set -x

base=`dirname "$0"`
base=`cd "$base"; pwd`
cd ${base}

image_version=reg.docker.alibaba-inc.com/alipay-alps/alps-runtime:cpu-tf1.13.1-alps2.0.9

# docker login reg.docker.alibaba-inc.com
if [[ -z $(docker images -q ${image_version}) ]];
then
    docker pull ${image_version}
fi

docker run --net=host --rm -it -m 30000m \
    -v ${base}/..:/graph_ml \
    -w /graph_ml \
    ${image_version} \
    "/bin/bash"