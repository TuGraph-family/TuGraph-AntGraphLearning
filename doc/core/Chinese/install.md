# AGL安装说明

AGL 目前主要以镜像的方式提供使用能力，包括编译与运行环境。

## Docker镜像：

```
aglimage/agl:agl-ubuntu-gcc9.4.0-py3.8-cuda11.8-pytorch2.0.1-0825
```

镜像中包含运行 AGL 所需的依赖，包括Java, Maven, Spark, Pytorch, PyG, Cuda等。
在该镜像的 docker 容器中，用户无需再关注环境配置.

## 安装步骤：

### 1. Clone 代码

```
git clone https://github.com/TuGraph-family/TuGraph-AntGraphLearning.git
```

### 2. 启动 docker

启动 docker 脚本 [参考](../../../docker/start_docker_with_image.sh)

```
cd docker
bash start_docker_with_image.sh
```

### 3. 编译源码

```
bash build.sh
```

执行这个脚本，将会基于当前分支编译一个whl并覆盖安装到当前docker中, whl包位于 dist 目录中，名称类似agl-0.0.1-cp38-cp38-linux_x86_64.whl


