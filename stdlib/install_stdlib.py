import os
import platform
from pathlib import Path
import subprocess

def check_path(p: Path):
    if not p.exists() or not p.is_dir():
        if not p.is_dir():
            os.remove(str(p))
        os.mkdir(str(p))

iswindows = platform.platform() == "win32"

homedir = os.environ["userprofile"] if iswindows else os.environ["HOME"]
py_command = "python" if iswindows else "python3"

current = Path(".")
jil = Path(homedir, "jil")
lib = Path(jil, "lib")
std = Path(lib, "std")

check_path(jil)
check_path(lib)
check_path(std)

comp = []
for f in current.iterdir():
    if f.is_file() and f.name.split(os.extsep)[-1] == "java":
        ex = subprocess.run([py_command, "build_module.py", "-f", f.name, "-i"])
        if ex.returncode:
            print(ex.stderr)
