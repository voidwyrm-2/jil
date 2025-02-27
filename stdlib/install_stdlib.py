import os
import platform
from pathlib import Path
import subprocess


def check_path(p: Path):
    ps = str(p)
    if not p.exists():
        os.mkdir(ps)
    elif not p.is_dir():
        os.remove(ps)
        os.mkdir(ps)


iswindows = "windows" in platform.platform().lower()

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
    if f.is_file():
        ext = f.name.split(os.extsep)[-1].lower()
        if ext == "java":
            ex = subprocess.run([py_command, "build_module.py", "-f", f.name, "-i"])
            if ex.returncode:
                print(ex.stderr)
        elif ext == "jil":
            with open(f, "rb") as inf:
                content = inf.read()

            with open(Path(std, f.name), "wb") as ouf:
                ouf.write(content)
