import getpass
import os
import sys
import glob
import subprocess
from datetime import datetime
from string import Template

from setuptools import setup, find_packages, Extension

cwd = os.path.abspath(os.path.dirname(__file__))
# Read Version
with open(os.path.join(cwd, "AGL_VERSION"), "r") as rf:
    version = rf.readline().strip("\n").strip()

# Generate agl.__init__.py
# More type: https://patorjk.com/software/taag/
with open(os.path.join(cwd, "agl/version.py"), "w") as wf:
    content = Template(
        """
__version__ = "${VERSION}"

class VersionInfo:
    BUILD_DATE = "${BUILD_DATE}"
    BUILD_VERSION = "${VERSION}"
    GIT_RECV = "${GIT_RECV}"
    BUILD_USER = "${BUILD_USER}"
    log_once = False

    def info(self):
        if self.log_once:
            return
        self.log_once = True
        import sys
        print(f\"\"\"
===========================================================
              .█████╗...██████╗..██╗
              ██╔══██╗.██╔════╝..██║
              ███████║.██║..███╗.██║
              ██╔══██║.██║...██║.██║
              ██║..██║.╚██████╔╝.███████╗
              ╚═╝..╚═╝..╚═════╝..╚══════╝                
============ AGL(Ant Graph Learning) ReleaseInfo ==========
BUILD_VERSION:{VersionInfo.BUILD_VERSION}
BUILD_DATE:{VersionInfo.BUILD_DATE}
GIT_RECV:{VersionInfo.GIT_RECV}
BUILD_USER:{VersionInfo.BUILD_USER}
===========================================================\"\"\",
            file=sys.stderr, flush=True)
    """
    ).substitute(
        VERSION=version,
        GIT_RECV=subprocess.check_output("git rev-parse HEAD", shell=True)
        .decode("utf-8")
        .strip(),
        BUILD_USER=getpass.getuser(),
        BUILD_DATE=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    )
    wf.write(content)

include_dirs = []
extra_compile_args = []
extra_link_args = []
extensions = []
library_dirs = []
libraries = []

ROOT_PATH = os.path.abspath(os.path.join(os.getcwd()))
CUT_PATH = sys.path[0]

py11_include_str = os.popen("python3 -m pybind11 --includes").read().strip().split()
py11_include = [inc_str.split("-I")[1] for inc_str in py11_include_str]

include_dirs.extend(py11_include)
include_dirs.append(ROOT_PATH + "/agl/cpp/")
include_dirs.append(ROOT_PATH + "/agl/cpp/graph/")
include_dirs.append(ROOT_PATH + "/agl/cpp/graph/base_data_structure/")
include_dirs.append(ROOT_PATH + "/agl/cpp/graph/features")
include_dirs.append(ROOT_PATH + "/agl/py_api/tools")
include_dirs.append(ROOT_PATH + "/third_party/boost/boost_install/include")
include_dirs.append(ROOT_PATH + "/third_party/output/protobuf/include")

extra_compile_args.append("-std=c++11")

extra_link_args.append("-Wl,-rpath=" + ROOT_PATH + "/agl/python/lib/")
library_dirs.append(ROOT_PATH + "/output/lib/")
libraries.append("agl")

sources = [ROOT_PATH + "/agl/cpp/py_api/pybind11_wrapper.cc"]

try_extension = Extension(
    "pyagl",
    sources,
    extra_compile_args=extra_compile_args,
    extra_link_args=extra_link_args,
    include_dirs=include_dirs,
    library_dirs=library_dirs,
    libraries=libraries,
)
extensions.append(try_extension)

agl_data = []
try:
    f = "libagl.so"
    fname = f"{cwd}/output/lib/{f}"
    assert os.path.exists(fname), f"Compile cxx first, {fname}"
    cmd = f"cp -f {fname} {cwd}/agl/python/lib/{f}"
    print(f"Do {cmd}")
    subprocess.check_output(cmd, shell=True)
    agl_data.append(f"{cwd}/agl/python/lib/{f}")
except Exception as e:
    print(e)
    print("*so not found, build python only")

print(f">>>>>>>> agl_data: {agl_data}")

setup(
    name="agl",
    version=version,
    description="AGL (Ant Graph Learning)",
    author="Ant AI",
    packages=find_packages(
        where=".",
        exclude=[
            ".*test.py",
            "*_test.py",
            "*.txt",
            "tests",
            "tests.*",
            "configs",
            "configs.*",
            "test",
            "test.*",
            "*.tests",
            "*.tests.*",
            "*.pyc",
        ],
    ),
    install_requires=[
        r.strip()
        for r in open("requirements.txt", "r")
        if not r.strip().startswith("#")
    ],
    package_dir={"pyagl": "."},
    package_data={"agl": agl_data},
    ext_package="pyagl",
    ext_modules=extensions,
)
