import getpass
import os
import subprocess
from datetime import datetime
from string import Template

from setuptools import setup, find_packages

cwd = os.path.abspath(os.path.dirname(__file__))
# Read Version
with open(os.path.join(cwd, "AGL_VERSION"), "r") as rf:
    version = rf.readline().strip("\n").strip()

# Generate agl.__init__.py
# More type: https://patorjk.com/software/taag/
with open(os.path.join(cwd, "agl/version.py"), "w") as wf:
    content = Template("""
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
    """).substitute(VERSION=version,
                    GIT_RECV=subprocess.check_output("git rev-parse HEAD", shell=True).decode("utf-8").strip(),
                    BUILD_USER=getpass.getuser(),
                    BUILD_DATE=datetime.now().strftime('%Y-%m-%d %H:%M:%S'))
    wf.write(content)

setup(name="agl",
      version=version,
      description="AGL (Ant Graph Learning)",
      url="https://code.alipay.com/Alps/AGL",
      author="Ant AI",
      packages=find_packages(
          where=".",
          exclude=['.*test.py', 'tests', 'tests.*', "configs", "configs.*", "test", "test.*", '*.tests', '*.tests.*',
                   "*.pyc"]),
      package_data={"agl": []},
      install_requires=[r.strip() for r in open("requirements.txt", "r") if not r.strip().startswith("#")],
      )
