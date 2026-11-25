#!/usr/bin/env python3
"""PendingCase 以外の方法でテストを無効にする変更を PR で止める（ADR-026、テストケース一覧 3 章）。

PR の base...HEAD で追加されたテストソースの行に、@Ignore / @Ignored / @Disabled / enabled = false /
xtest・xcase などの x 接頭辞 / "!名前" / assume 系があれば失敗にする。保留は case(..., pending = PendingCase(...))
だけで表す（ACG-37）。

「有効 → 保留への逆戻り」と「有効だったケースの消失」はここでは見ない。ACG-37 の :quality:checkCaseIds が
quality/case-status.csv を origin/main と比べて検出する（./gradlew build に含まれる）。

使い方:
  python3 scripts/ci/test_guard.py --base <ref>
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys

# テストソースとみなすパス（src/test・src/*Test・testFixtures）
TEST_PATH_RE = re.compile(r"(^|/)src/(test|[A-Za-z0-9]*Test|testFixtures)[A-Za-z0-9]*/.*\.(kt|kts|java)$")

DISABLE_PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("@Ignore / @Ignored", re.compile(r"@(Ignore|Ignored)\b")),
    ("@Disabled*", re.compile(r"@Disabled[A-Za-z]*\b")),
    ("enabled = false", re.compile(r"\benabled\s*=\s*false\b")),
    ("enabledIf / enabledOrReasonIf", re.compile(r"\benabled(If|OrReasonIf)\s*=")),
    (
        "x 接頭辞（xtest / xcase / xcontext など）",
        re.compile(r"(?<![\w.])x(test|case|context|describe|it|should|given|when|then|feature|scenario|expect|f?Spec)\s*\("),
    ),
    ("kotest の '!' 接頭辞", re.compile(r'"![^"]*"\s*(-\s*)?\{')),
    ("assumeTrue / Assumptions", re.compile(r"\b(assumeTrue|assumeFalse|Assumptions\.)")),
]


def git(*args: str) -> str:
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout


def added_lines(diff: str) -> list[tuple[str, int, str]]:
    """unified diff から、テストソースに追加された行（ファイル, 行番号, 内容）を取り出す。"""
    result, file, line_no = [], None, 0
    for raw in diff.splitlines():
        if raw.startswith("+++ "):
            path = raw[4:]
            file = path[2:] if path.startswith("b/") else None
        elif raw.startswith("@@"):
            m = re.search(r"\+(\d+)", raw)
            line_no = int(m.group(1)) if m else 0
        elif raw.startswith("+") and file and TEST_PATH_RE.search(file):
            result.append((file, line_no, raw[1:]))
            line_no += 1
    return result


def find_disables(lines: list[tuple[str, int, str]]) -> list[str]:
    errors = []
    for file, no, text in lines:
        code = text.split("//", 1)[0]
        for label, pat in DISABLE_PATTERNS:
            if pat.search(code):
                errors.append(f"{file}:{no}: PendingCase 以外の無効化 [{label}]: {text.strip()}")
    return errors


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True, help="比較元の git ref（例: PR の base SHA）")
    args = ap.parse_args()
    lines = added_lines(git("diff", "--unified=0", "--no-color", f"{args.base}...HEAD"))
    errors = find_disables(lines)
    for e in errors:
        print(f"::error::{e}")
    print(f"テストソースの追加行 {len(lines)} 行を検査、違反 {len(errors)} 件")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
