#!/usr/bin/env python3
"""ACG-51 / ADR-044: Kotlin のパッケージ名を me.tbsten.analysiscodegen.* から me.tbsten.analysis.code.gen.* に変える。

- `me/tbsten/analysiscodegen` のディレクトリを `me/tbsten/analysis/code/gen` へ git mv する
- ファイル名に FQN を含むもの（META-INF/services など）も改名する
- テキスト中の `me.tbsten.analysiscodegen` と `me/tbsten/analysiscodegen` を書き換える
- 何度実行しても同じ結果になる。進行中の branch は main を取り込んだあとにこれを流す:
    python3 scripts/acg51_rename_packages.py            # 作業ツリー全体
    python3 scripts/acg51_rename_packages.py path ...   # 指定したファイルの中身だけ
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

OLD_DOT, NEW_DOT = "me.tbsten.analysiscodegen", "me.tbsten.analysis.code.gen"
OLD_PATH, NEW_PATH = "me/tbsten/analysiscodegen", "me/tbsten/analysis/code/gen"

ROOT = Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip())
SKIP_PARTS = {".git", "build", ".gradle", "node_modules", ".pbt-state"}
# 旧 group を扱うモジュール改名の script と、この script 自身は書き換えない
SKIP_FILES = {"scripts/acg51_rename_modules.py", "scripts/acg51_rename_packages.py"}


def git_files() -> list[str]:
    out = subprocess.check_output(["git", "ls-files", "-co", "--exclude-standard"], cwd=ROOT, text=True)
    return [line for line in out.splitlines() if not any(p in SKIP_PARTS for p in Path(line).parts)]


def git_mv(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    subprocess.check_call(["git", "mv", str(src.relative_to(ROOT)), str(dst.relative_to(ROOT))], cwd=ROOT)


def move_paths() -> None:
    for rel in git_files():
        new_rel = rel.replace(OLD_PATH + "/", NEW_PATH + "/")
        name = Path(new_rel).name
        if OLD_DOT in name:
            new_rel = str(Path(new_rel).with_name(name.replace(OLD_DOT, NEW_DOT)))
        if new_rel != rel and (ROOT / rel).exists():
            git_mv(ROOT / rel, ROOT / new_rel)
    # 空になった古いディレクトリを消す
    for d in sorted(ROOT.rglob("analysiscodegen"), key=lambda p: len(p.parts), reverse=True):
        if d.is_dir() and not any(d.rglob("*")) and not any(p in SKIP_PARTS for p in d.relative_to(ROOT).parts):
            d.rmdir()


def rewrite(text: str) -> str:
    return text.replace(OLD_DOT, NEW_DOT).replace(OLD_PATH, NEW_PATH)


def main(argv: list[str]) -> int:
    if argv:
        files = [Path(a).resolve() for a in argv]
    else:
        move_paths()
        files = [ROOT / f for f in git_files() if f not in SKIP_FILES]
    changed = 0
    for f in files:
        if not f.is_file():
            continue
        try:
            with f.open(encoding="utf-8", newline="") as fh:
                text = fh.read()
        except UnicodeDecodeError:
            continue
        new = rewrite(text)
        if new != text:
            with f.open("w", encoding="utf-8", newline="") as fh:
                fh.write(new)
            changed += 1
    print(f"rewrote {changed} files")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
