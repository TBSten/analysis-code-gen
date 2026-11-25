#!/usr/bin/env python3
"""Maven Central の Kotlin の版と gradle/kotlin-versions.txt を比べ、足りない版を出す（ADR-026、VER-04）。

使い方:
  python3 scripts/ci/kotlin_upstream.py [--metadata-file <maven-metadata.xml>]
出力: 差があれば Markdown を標準出力に出し、GITHUB_OUTPUT に missing=true を書く。
"""
from __future__ import annotations

import argparse
import importlib.util
import os
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
URL = "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml"

spec = importlib.util.spec_from_file_location("kv", ROOT / "scripts" / "kotlin-versions.py")
kv = importlib.util.module_from_spec(spec)
spec.loader.exec_module(kv)


def expected(upstream: list[str]) -> tuple[list[str], str | None]:
    """2.4.0 以降の全 stable と、最新 stable より新しい最新のプレリリース（ADR-008）。"""
    valid = []
    for v in upstream:
        try:
            kv.version_key(v)
            valid.append(v)
        except ValueError:
            continue  # 2.4.0-ij... など一覧の対象外の版
    floor = kv.version_key(kv.BASELINE)
    valid = sorted((v for v in valid if kv.version_key(v) >= floor), key=kv.version_key)
    stables = [v for v in valid if kv.version_key(v)[3] == kv.MATURITY[None]]
    pres = [v for v in valid if kv.version_key(v)[3] != kv.MATURITY[None] and kv.version_key(v)[3] > 0]
    latest_pre = pres[-1] if pres and (not stables or kv.version_key(pres[-1]) > kv.version_key(stables[-1])) else None
    return stables, latest_pre


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--metadata-file")
    args = ap.parse_args()
    if args.metadata_file:
        xml = Path(args.metadata_file).read_text(encoding="utf-8")
    else:
        with urllib.request.urlopen(URL, timeout=30) as r:
            xml = r.read().decode("utf-8")
    upstream = re.findall(r"<version>([^<]+)</version>", xml)
    stables, latest_pre = expected(upstream)
    listed = {v: role for v, role, _ in kv.load()}
    lines = []
    for v in stables:
        if v not in listed:
            lines.append(f"- stable `{v}` が一覧に無い")
    current_pre = [v for v, r in listed.items() if r == "prerelease"]
    if latest_pre and latest_pre not in listed:
        lines.append(f"- 最新プレリリース `{latest_pre}` が一覧に無い（一覧: {current_pre or 'なし'}）")
    if not latest_pre and current_pre:
        lines.append(f"- 一覧のプレリリース {current_pre} より新しい stable が出ている。prerelease 行を外す")
    if lines:
        print("## gradle/kotlin-versions.txt の更新が要る\n")
        print("\n".join(lines))
        print("\n手順: 一覧を直し `python3 scripts/kotlin-versions.py check` を通す。`pr` 印は baseline と最新 stable の 2 行（ADR-011）。")
        print("version catalog の `kotlin` も最新 stable に合わせる。")
    else:
        print("ok: 一覧は最新")
    out = os.environ.get("GITHUB_OUTPUT")
    if out:
        with open(out, "a", encoding="utf-8") as fh:
            fh.write(f"missing={'true' if lines else 'false'}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
