#!/usr/bin/env python3
"""テストを弱める変更を PR で止める（ADR-026、テストケース一覧 3 章「一覧とテストの変え方」）。

base（PR の向き先）と HEAD のテストソースを比べ、次を検出する。

  1. 保留に戻した: base で保留でなかったケース ID が HEAD で保留（PendingCase）になった → 失敗（例外なし、R-01）
  2. 保留以外の無効化: 追加行に @Ignore / @Disabled / enabled = false / xtest / "!名前" などがある → 失敗
  3. ケースの削除: base のテストにあった ID が HEAD のどのテストにも無い → PR 本文に
     `Case-Removal-ADR: ADR-<番号>` があれば警告のみ、無ければ失敗

使い方:
  python3 scripts/ci/test_guard.py --base <ref> [--pr-body-file <path>]
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field

# テストソースとみなすパス（src/test・src/*Test・testFixtures・fixtures 配下の test）
TEST_PATH_RE = re.compile(r"(^|/)src/(test|[A-Za-z0-9]*Test|testFixtures)[A-Za-z0-9]*/.*\.(kt|kts|java)$")
# ケース ID（例: INC-14, BRG-03a, RW-CRM-B1）。テストケース一覧の ID 体系。
# "UTF-8" などを拾わないよう接頭辞を限る。quality/test-cases.csv がリポジトリにあればそこから読む（ACG-37 で移す）
KNOWN_PREFIXES = (
    "ANA API BIN BINC BKMP BRG CCH CLI DEF DET DOC FAIL GCC GRD HYB INC KMP LIC LIF MEM MIX MOD OUT PBT PERF "
    "RND RW TRM TST VER XD"
).split()
CASES_CSV = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "quality", "test-cases.csv")


def load_prefixes() -> list[str]:
    prefixes = set(KNOWN_PREFIXES)
    if os.path.exists(CASES_CSV):
        with open(CASES_CSV, encoding="utf-8") as fh:
            for line in fh.readlines()[1:]:
                m = re.match(r"([A-Z]+)-", line)
                if m:
                    prefixes.add(m.group(1))
    return sorted(prefixes, key=len, reverse=True)


CASE_ID = r"(?:" + "|".join(load_prefixes()) + r")-(?:\d{2,4}[a-z]?|[A-Z]{2,4}-[A-Z]\d{1,2}[a-z]?)"
CASE_ID_LITERAL_RE = re.compile(r'"(' + CASE_ID + r')(?![\w-])')
# 保留の印。@PendingCase(id = "X") / pendingCase("X") / .config(pending = PendingCase("X")) などを広く拾う
PENDING_RE = re.compile(r"(?i)pending\s*case")
PENDING_WINDOW = 300  # 印の後ろ何文字以内の最初の ID を保留とみなすか

# 保留（PendingCase）以外でテストを無効化する書き方
DISABLE_PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("@Ignore / @Ignored", re.compile(r"@(Ignore|Ignored)\b")),
    ("@Disabled*", re.compile(r"@Disabled[A-Za-z]*\b")),
    ("enabled = false", re.compile(r"\benabled\s*=\s*false\b")),
    ("enabledIf / enabledOrReasonIf", re.compile(r"\benabled(If|OrReasonIf)\s*=")),
    ("kotest の x 接頭辞（xtest / xcontext など）", re.compile(r"(?<![\w.])x(test|context|describe|it|should|given|when|then|feature|scenario|expect|f?Spec)\s*\(")),
    ("kotest の '!' 接頭辞", re.compile(r'"![^"]*"\s*(-\s*)?\{')),
    ("assumeTrue / Assumptions", re.compile(r"\b(assumeTrue|assumeFalse|Assumptions\.)")),
]
REMOVAL_ADR_RE = re.compile(r"^Case-Removal-ADR:\s*ADR-\d{3}", re.M)


@dataclass
class Snapshot:
    all_ids: set[str] = field(default_factory=set)
    pending_ids: set[str] = field(default_factory=set)


def scan_text(text: str, snap: Snapshot) -> None:
    snap.all_ids.update(CASE_ID_LITERAL_RE.findall(text))
    for m in PENDING_RE.finditer(text):
        found = CASE_ID_LITERAL_RE.search(text, m.end(), m.end() + PENDING_WINDOW)
        if found:
            snap.pending_ids.add(found.group(1))


def git(*args: str) -> str:
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout


def snapshot_at(ref: str | None) -> Snapshot:
    snap = Snapshot()
    if ref is None:
        files = git("ls-files").splitlines()
        for f in files:
            if TEST_PATH_RE.search(f) and os.path.exists(f):
                with open(f, encoding="utf-8", errors="replace") as fh:
                    scan_text(fh.read(), snap)
    else:
        for f in git("ls-tree", "-r", "--name-only", ref).splitlines():
            if TEST_PATH_RE.search(f):
                scan_text(git("show", f"{ref}:{f}"), snap)
    return snap


def added_lines(base: str) -> list[tuple[str, int, str]]:
    """base...HEAD で追加されたテストソースの行（ファイル, 行番号, 内容）。"""
    out = git("diff", "--unified=0", "--no-color", f"{base}...HEAD")
    result, file, line_no = [], None, 0
    for raw in out.splitlines():
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
                errors.append(f"{file}:{no}: 保留（PendingCase）以外の無効化 [{label}]: {text.strip()}")
    return errors


def judge(base: Snapshot, head: Snapshot, disables: list[str], pr_body: str) -> tuple[list[str], list[str]]:
    errors, warnings = list(disables), []
    base_active = base.all_ids - base.pending_ids
    for cid in sorted((head.pending_ids - base.pending_ids) & base_active):
        errors.append(f"{cid}: base では保留でなかったケースを保留に戻している（禁止。退行は直す、R-01）")
    removed = sorted(base.all_ids - head.all_ids)
    if removed:
        msg = f"テストから消えたケース ID: {', '.join(removed)}"
        if REMOVAL_ADR_RE.search(pr_body):
            warnings.append(msg + "（PR 本文の Case-Removal-ADR を確認）")
        else:
            errors.append(msg + "。削除は ADR が要る。PR 本文に `Case-Removal-ADR: ADR-<番号>` を書くこと")
    return errors, warnings


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True, help="比較元の git ref（例: origin/main）")
    ap.add_argument("--pr-body-file")
    args = ap.parse_args()
    body = ""
    if args.pr_body_file and os.path.exists(args.pr_body_file):
        with open(args.pr_body_file, encoding="utf-8") as fh:
            body = fh.read()
    base = snapshot_at(args.base)
    head = snapshot_at(None)
    errors, warnings = judge(base, head, find_disables(added_lines(args.base)), body)
    for w in warnings:
        print(f"::warning::{w}")
    for e in errors:
        print(f"::error::{e}")
    print(f"ケース ID: base {len(base.all_ids)}（保留 {len(base.pending_ids)}） → HEAD {len(head.all_ids)}（保留 {len(head.pending_ids)}）")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
