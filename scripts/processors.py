#!/usr/bin/env python3
"""fixtures/processors/processors.toml を検査する（ADR-019）。

使い方:
  python3 scripts/processors.py check   # 規則を検査（違反で exit 1）
  python3 scripts/processors.py list    # Processor と源 ② の数を表示
"""
from __future__ import annotations

import re
import sys
import tomllib
from pathlib import Path

LIST = Path(__file__).resolve().parent.parent / "fixtures" / "processors" / "processors.toml"
PERMISSIVE = {"Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause"}
LEVELS = {"required", "optional", "none"}
SHA = r"[0-9a-f]{40}"
SOURCE_RE = re.compile(rf"^(github:[\w.-]+/[\w.-]+@{SHA}|maven-sources:[\w.-]+:[\w.-]+:[\w.-]+)$")
VERSION_RE = re.compile(r"^\d+\.\d+\.\d+(?:-[\w.]+)?$")
MIN_PROJECTS = 3  # 源 ② は Processor ごとに 3〜5 個（pbt-differential-design 2.2）
NO_PROJECT_OK = {"CRM"}  # 外部の利用プロジェクトが無い（ADR-019）


def load() -> dict:
    with LIST.open("rb") as f:
        return tomllib.load(f)


def check(data: dict) -> list[str]:
    errors: list[str] = []
    procs = data.get("processor", [])
    ids = [p.get("id") for p in procs]
    if len(ids) != len(set(ids)):
        errors.append(f"Processor の id が重複: {ids}")
    for p in procs:
        pid = p.get("id", "?")
        for key in ("id", "name", "artifacts", "version", "source", "license", "bridge", "native", "kmp"):
            if key not in p:
                errors.append(f"{pid}: {key} が無い")
        if not VERSION_RE.match(p.get("version", "")):
            errors.append(f"{pid}: 版の形式が不正: {p.get('version')}")
        if not SOURCE_RE.match(p.get("source", "")):
            errors.append(f"{pid}: source の形式が不正（40 桁の SHA か maven-sources）: {p.get('source')}")
        if p.get("source", "").startswith("maven-sources:") and not p["source"].endswith(":" + p.get("version", "")):
            errors.append(f"{pid}: maven-sources の版が version と違う")
        if p.get("license") not in PERMISSIVE:
            errors.append(f"{pid}: 寛容なライセンスではない: {p.get('license')}")
        for key in ("bridge", "native"):
            if p.get(key) not in LEVELS:
                errors.append(f"{pid}: {key} は {sorted(LEVELS)} のどれか: {p.get(key)}")
        if p.get("bridge") != "required":
            errors.append(f"{pid}: 対象の Processor はすべて Bridge 版が必須（04 スコープ）")
    known = set(ids)
    counts = {i: 0 for i in ids}
    repos = []
    for pr in data.get("project", []):
        repo = pr.get("repo", "?")
        repos.append(repo)
        if not re.fullmatch(SHA, pr.get("commit", "")):
            errors.append(f"{repo}: commit は 40 桁の SHA")
        if pr.get("license") not in PERMISSIVE:
            errors.append(f"{repo}: 寛容なライセンスではない: {pr.get('license')}")
        uses = pr.get("uses", [])
        if not uses:
            errors.append(f"{repo}: uses が空")
        for u in uses:
            if u not in known:
                errors.append(f"{repo}: 未知の Processor id: {u}")
            else:
                counts[u] += 1
    if len(repos) != len(set(repos)):
        errors.append("源 ② のプロジェクトが重複している")
    for pid, n in counts.items():
        if pid not in NO_PROJECT_OK and n < MIN_PROJECTS:
            errors.append(f"{pid}: 源 ② が {n} 個（{MIN_PROJECTS} 個以上必要）")
    return errors


def main(argv: list[str]) -> int:
    cmd = argv[1] if len(argv) > 1 else "check"
    data = load()
    if cmd == "check":
        errors = check(data)
        for e in errors:
            print(f"error: {e}", file=sys.stderr)
        if errors:
            return 1
        print(f"ok: Processor {len(data['processor'])} 個、源 ② {len(data.get('project', []))} 個")
        return 0
    if cmd == "list":
        for p in data["processor"]:
            n = sum(p["id"] in pr["uses"] for pr in data.get("project", []))
            print(f"{p['id']}\t{p['name']}\t{p['version']}\tbridge={p['bridge']}\tnative={p['native']}\t源②={n}")
        return 0
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
