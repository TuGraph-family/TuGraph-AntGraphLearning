# AGL Installation Guide

AGL currently primarily provides its functionality through images, encompassing both compilation and runtime
environments.

## Docker Image

```
aglimage/agl:agl-ubuntu-gcc9.4.0-py3.8-cuda11.8-pytorch2.0.1-0825
```

The image contains all the dependencies required to run AGL, including Java, Maven, Spark, Pytorch, PyG, and Cuda.
Users no longer need to worry about environment configuration within the Docker container.

## Installation Steps:

### 1. Clone the code

```
git clone https://github.com/TuGraph-family/TuGraph-AntGraphLearning.git
```

### 2. Start Docker

```
cd docker
bash start_docker_with_image.sh
```

The script to start the docker container [link](../../../docker/start_docker_with_image.sh)

### 3. Compile the Source Code

```
bash build.sh
```

By executing this script, a whl package will be compiled based on the current branch and installed in the current
Docker, replacing any existing installation. The whl package can be found in the dist directory and is typically named
agl-0.0.1-cp38-cp38-linux_x86_64.whl.






