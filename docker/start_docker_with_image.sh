#!/usr/bin/env bash

set -e
set -x

base=`dirname "$0"`
base=`cd "$base"; pwd`
cd ${base}

image_version=aglimage/agl:agl-ubuntu-gcc9.4.0-py3.8-cuda11.8-pytorch2.0.1-0825

if [[ -z $(docker images -q ${image_version}) ]];
then
    docker pull ${image_version}
fi

docker run --net=host --rm -it -m 30000m \
    -v ${base}/..:/graph_ml \
    -w /graph_ml \
    ${image_version} \
    "/bin/bash"
