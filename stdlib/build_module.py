import argparse
import os
from pathlib import Path
import sys
import platform
import subprocess

JIL_CLASS_PATH = "../out/production/jil/"

parser = argparse.ArgumentParser()

parser.add_argument("-i", "--install", action="store_true")
parser.add_argument("-f", "--file", required=True)

parsed = parser.parse_args()

iswindows = "windows" in platform.platform().lower()

homedir = os.environ["userprofile"] if iswindows else os.environ["HOME"]

file: str = parsed.file  # pyright: ignore
file_comp = os.extsep.join(file.split(os.extsep)[:-1]) + ".class"
install_path = Path(homedir, "jil", "lib", "std", file_comp)

ex = subprocess.run(["javac", file, "--release", "17", "-cp", JIL_CLASS_PATH])
if ex.returncode:
    sys.stderr.write(str(ex.stderr) + "\n")
    exit(1)

if parsed.install:  # pyright: ignore
    with open(file_comp, "rb") as inf:
        jclass = inf.read()

    with open(install_path, "wb") as ouf:
        ouf.write(jclass)

    os.remove(str(file_comp))
