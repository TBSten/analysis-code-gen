#!/usr/bin/env python3
"""kotlin-compile-testing (kctfork) の main ソースを testing-kct へ取り込む（ADR-021）。

固定コミットは porting/kct/UPSTREAM.properties の commit。fork 元を更新するときは
UPSTREAM.properties の tag / commit を書き換えてからこのスクリプトを流し直し、差分を確認して PR にする。

使い方
  python3 porting/kct/vendor.py --upstream-dir /path/to/kotlin-compile-testing

やること
  - core/src/main と ksp/src/main の .kt を testing-kct/src/main/kotlin/... へコピーする
  - パッケージを me.tbsten.analysiscodegen.testing.kct（kapt は .kapt）へ移す
  - @AutoService を外す（サービス登録は testing-kct/src/main/resources/META-INF/services に手書き）
  - コンパイラプラグイン ID を本家と衝突しないものに変える
  - 先頭に MPL-2.0 の表示と Ported-From / Modified-By / Modifications を書く（LIC-01）
  - porting/kct/port-map.csv を書き直す
"""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TARGET_ROOT = ROOT / "testing-kct/src/main/kotlin/me/tbsten/analysiscodegen/testing/kct"
NEW_PKG = "me.tbsten.analysiscodegen.testing.kct"

# 長いものから置き換える
PACKAGE_REWRITES = [
    ("com.facebook.buck.jvm.java.javax.com.tschuchort.compiletesting.kapt", f"{NEW_PKG}.kapt"),
    ("com.tschuchort.compiletesting.kapt", f"{NEW_PKG}.kapt"),
    ("com.tschuchort.compiletesting", NEW_PKG),
    ("com.facebook.buck.jvm.java.javax", NEW_PKG),
]

MODIFICATIONS = (
    f"パッケージを {NEW_PKG} へ移動、@AutoService を手書きの META-INF/services に置換、"
    "コンパイラプラグイン ID を変更（それ以外の処理は変えていない）"
)


def read_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    return props


def git_show(upstream: Path, sha: str, path: str) -> str:
    return subprocess.run(
        ["git", "-C", str(upstream), "show", f"{sha}:{path}"], check=True, capture_output=True, text=True
    ).stdout


def git_ls(upstream: Path, sha: str, prefix: str) -> list[str]:
    out = subprocess.run(
        ["git", "-C", str(upstream), "ls-tree", "-r", "--name-only", sha, prefix],
        check=True, capture_output=True, text=True,
    ).stdout
    return sorted(p for p in out.splitlines() if p.endswith(".kt"))


def target_path(upstream_path: str) -> Path:
    # core/src/main/kotlin/com/tschuchort/compiletesting/kapt/util.kt -> kapt/util.kt
    rel = upstream_path.split("/src/main/kotlin/", 1)[1]
    rel = rel.removeprefix("com/tschuchort/compiletesting/")
    return TARGET_ROOT / rel


def transform(text: str) -> str:
    for old, new in PACKAGE_REWRITES:
        text = text.replace(old, new)
    text = re.sub(r"^import com\.google\.auto\.service\.AutoService\n", "", text, flags=re.MULTILINE)
    text = re.sub(r"^@AutoService\([^)]*\)\n", "", text, flags=re.MULTILINE)
    text = text.replace('"dev.zacsweers.kotlincompiletesting"', '"me.tbsten.analysiscodegen.testing.kct"')
    return text


def upstream_copyright(text: str) -> str:
    head = text.splitlines()[:30]
    found = [l.strip().lstrip("/*# ").strip() for l in head if re.search(r"Copyright\b", l)]
    return " | ".join(found)


def header(owner_repo: str, sha: str, upstream_path: str) -> str:
    return (
        "// This Source Code Form is subject to the terms of the Mozilla Public\n"
        "// License, v. 2.0. If a copy of the MPL was not distributed with this\n"
        "// file, You can obtain one at https://mozilla.org/MPL/2.0/.\n"
        "// Copyright kotlin-compile-testing contributors (tschuchortdev/kotlin-compile-testing, ZacSweers/kotlin-compile-testing)\n"
        f"// Ported-From: {owner_repo}@{sha}:{upstream_path}\n"
        "// Modified-By: analysis-code-gen contributors\n"
        f"// Modifications: {MODIFICATIONS}\n"
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--upstream-dir", required=True, type=Path)
    args = ap.parse_args()
    props = read_properties(ROOT / "porting/kct/UPSTREAM.properties")
    sha = props["commit"]
    owner_repo = "/".join(props["repository"].rstrip("/").split("/")[-2:])

    files = git_ls(args.upstream_dir, sha, "core/src/main") + git_ls(args.upstream_dir, sha, "ksp/src/main")
    if TARGET_ROOT.exists():
        for old in TARGET_ROOT.rglob("*.kt"):
            old.unlink()
    rows = []
    for up in files:
        original = git_show(args.upstream_dir, sha, up)
        dest = target_path(up)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(header(owner_repo, sha, up) + transform(original), encoding="utf-8")
        rows.append({
            "upstream_path": up,
            "size_bytes": str(len(original.encode("utf-8"))),
            "upstream_copyright": upstream_copyright(original),
            "target_module": "testing-kct",
            "strategy": "as-is",
            "phase": "P1",
            "status": "ported",
            "target_paths": dest.relative_to(ROOT).as_posix(),
            "notes": "",
        })
    with (ROOT / "porting/kct/port-map.csv").open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()), lineterminator="\n")
        w.writeheader()
        w.writerows(rows)
    print(f"{len(rows)} files vendored from {owner_repo}@{sha}")


if __name__ == "__main__":
    main()
