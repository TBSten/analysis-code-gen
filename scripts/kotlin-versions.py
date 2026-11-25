#!/usr/bin/env python3
"""gradle/kotlin-versions.txt を検査し、CI の matrix を JSON で出す（ADR-011）。

使い方:
  python3 scripts/kotlin-versions.py check          # 一覧の規則を検査（違反で exit 1）
  python3 scripts/kotlin-versions.py matrix pr      # PR 用: ["2.4.0", "2.4.20"]
  python3 scripts/kotlin-versions.py matrix full    # 定期実行用: すべて
  python3 scripts/kotlin-versions.py latest-stable  # -Pacg.kotlin の既定値
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

LIST = Path(__file__).resolve().parent.parent / "gradle" / "kotlin-versions.txt"
BASELINE = "2.4.0"
ROLES = {"baseline", "stable", "prerelease"}
VERSION_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-(dev|Beta|RC)(\d*)(?:-(\d+))?)?$")
# Maturity の順（KotlinToolingVersion と同じ考え方）: dev < Beta < RC < stable
MATURITY = {"dev": 0, "Beta": 1, "RC": 2, None: 3}


def version_key(v: str) -> tuple:
    m = VERSION_RE.match(v)
    if not m:
        raise ValueError(f"版の形式が不正: {v}")
    major, minor, patch, kind, n, build = m.groups()
    return (int(major), int(minor), int(patch), MATURITY[kind], int(n or 0), int(build or 0))


def load() -> list[tuple[str, str, bool]]:
    rows = []
    for no, raw in enumerate(LIST.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) not in (2, 3) or (len(parts) == 3 and parts[2] != "pr"):
            raise ValueError(f"{LIST.name}:{no}: 書式は '<版> <役割> [pr]': {raw}")
        rows.append((parts[0], parts[1], len(parts) == 3))
    return rows


def check(rows) -> list[str]:
    errors = []
    versions = [v for v, _, _ in rows]
    for v, role, _ in rows:
        if role not in ROLES:
            errors.append(f"{v}: 役割が不正: {role}")
        try:
            version_key(v)
        except ValueError as e:
            errors.append(str(e))
    if errors:
        return errors
    if len(set(versions)) != len(versions):
        errors.append("同じ版が 2 回以上ある")
    if versions != sorted(versions, key=version_key):
        errors.append("版を古い順に並べること")
    baselines = [v for v, r, _ in rows if r == "baseline"]
    if baselines != [BASELINE]:
        errors.append(f"baseline は {BASELINE} の 1 行だけ: {baselines}")
    stables = [v for v, r, _ in rows if r in ("baseline", "stable")]
    for v in stables:
        if version_key(v)[3] != MATURITY[None]:
            errors.append(f"{v}: stable / baseline にプレリリースの版は置けない")
    pres = [v for v, r, _ in rows if r == "prerelease"]
    if len(pres) > 1:
        errors.append(f"prerelease は最新の 1 つだけ: {pres}")
    for v in pres:
        if version_key(v)[3] == MATURITY[None]:
            errors.append(f"{v}: prerelease に stable の版は置けない")
        if stables and version_key(v) < version_key(stables[-1]):
            errors.append(f"{v}: prerelease が最新 stable {stables[-1]} より古い")
    pr = [v for v, _, p in rows if p]
    expected_pr = sorted({BASELINE, stables[-1]}, key=version_key) if stables else []
    if pr != expected_pr:
        errors.append(f"pr は baseline と最新 stable の 2 つ: 期待 {expected_pr}, 実際 {pr}")
    return errors


def main(argv: list[str]) -> int:
    rows = load()
    errors = check(rows)
    cmd = argv[1] if len(argv) > 1 else "check"
    if errors:
        for e in errors:
            print(f"error: {e}", file=sys.stderr)
        return 1
    if cmd == "check":
        print(f"ok: {len(rows)} versions")
    elif cmd == "matrix" and len(argv) > 2 and argv[2] in ("pr", "full"):
        sel = [v for v, _, p in rows if p] if argv[2] == "pr" else [v for v, _, _ in rows]
        print(json.dumps(sel))
    elif cmd == "latest-stable":
        print([v for v, r, _ in rows if r in ("baseline", "stable")][-1])
    else:
        print(__doc__, file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
