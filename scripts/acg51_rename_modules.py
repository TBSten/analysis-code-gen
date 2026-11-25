#!/usr/bin/env python3
"""ACG-51 / ADR-042: プロダクトモジュールを公開向けの名前に変える。

- ディレクトリを git mv し、テキスト中の参照（Gradle パス・ディレクトリパス・成果物名・group・プラグイン ID）を書き換える
- 何度実行しても同じ結果になる（すでに新しい名前のものは触らない）
- 進行中の branch は main を取り込んだあと、衝突した自分の変更にこれを流すと名前をそろえられる:
    python3 scripts/acg51_rename_modules.py            # 作業ツリー全体
    python3 scripts/acg51_rename_modules.py path ...   # 指定したファイルだけ（git mv はしない）
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

PREFIX = "analysis-code-gen-"
OLD_GROUP = "me.tbsten.analysiscodegen"
NEW_GROUP = "me.tbsten.analysis.code.gen"

# 公開する（候補の）モジュール。テスト・検査用の内部モジュールは対象外（ADR-042）
MODULES = [
    "processor-api",
    "project-model",
    "engine-spi",
    "analysis-engine",
    "processing-engine",
    "incremental-engine",
    "gradle-plugin",
    "cli",
    "testing",
    "testing-kct",
    "ksp-bridge",
    "ksp-bridge-api",
    "ksp-bridge-gradle-plugin",
]
# 英単語としても出てくる名前。ここは Gradle パス・ディレクトリ・`バッククオート` の形だけを直す
AMBIGUOUS = {"cli", "testing"}

ROOT = Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip())
SKIP_PARTS = {".git", "build", ".gradle", "node_modules", ".pbt-state"}
SKIP_FILES = {"scripts/acg51_rename_modules.py"}
TEXT_SUFFIXES = {
    ".kt", ".kts", ".java", ".md", ".mdx", ".yml", ".yaml", ".toml", ".properties", ".py", ".sh",
    ".json", ".csv", ".txt", ".xml", ".astro", ".mjs", ".js", ".ts", ".html", ".gitignore", ".cfg",
}


def build_rules() -> list[tuple[re.Pattern[str], str]]:
    rules: list[tuple[re.Pattern[str], str]] = []
    # group とプラグイン ID。パッケージ名（me.tbsten.analysiscodegen.xxx）は変えない
    g = re.escape(OLD_GROUP)
    rules.append((re.compile(g + r"(?=:)"), NEW_GROUP))  # Maven 座標 group:artifact
    rules.append((re.compile(g + r"(?=\.ksp-bridge\b)"), NEW_GROUP))  # Bridge のプラグイン ID
    rules.append((re.compile(r'(?<=["\'`])' + g + r'(?=["\'`])'), NEW_GROUP))  # 文字列・コード片ちょうど
    # 長い名前から順に（ksp-bridge-api を ksp-bridge より先に）
    for name in sorted(MODULES, key=len, reverse=True):
        n = re.escape(name)
        new = PREFIX + name
        not_after = r"(?<![\w.$/-])"  # 直前が識別子・. ・/・- なら別物（acg.gradle-plugin、java-gradle-plugin、a/testing など）
        not_before = r"(?![\w-]|\.\w)"  # project-model.json のようなファイル名は別物
        # Gradle のプロジェクトパスと Maven の artifact（:name）
        rules.append((re.compile(r"(?<![\w-]):" + n + not_before), ":" + new))
        if name in AMBIGUOUS:
            # docs の URL（testing/advanced など）と区別するため、モジュールの中身を指すパスだけ
            rules.append((re.compile(not_after + r"(?<!:)" + n + r"(?=/(src|build|\*\*|README))"), new))
            rules.append((re.compile(r"(?<=`)" + n + r"(?=`)"), new))
            rules.append((re.compile(r'(?<=")' + n + r'(?=")'), new))
        else:
            rules.append((re.compile(not_after + r"(?<!:)" + n + r"(?=/)"), new))
            rules.append((re.compile(not_after + r"(?<!:)" + n + not_before), new))
    return rules


RULES = build_rules()
# すでに新しい名前になっている部分を守るための目印
GUARD = re.compile(re.escape(PREFIX) + r"[\w-]+")


# 書き換えない行（フィクスチャ自身の rootProject.name、docs サイトの slug）
SKIP_LINE = re.compile(r"rootProject\.name|slug:")


def rewrite(text: str) -> str:
    return "".join(line if SKIP_LINE.search(line) else rewrite_line(line) for line in text.splitlines(keepends=True))


def rewrite_line(text: str) -> str:
    saved: list[str] = []

    def hide(m: re.Match[str]) -> str:
        saved.append(m.group(0))
        return f"\x00{len(saved) - 1}\x00"

    text = GUARD.sub(hide, text)
    for pat, rep in RULES:
        # 置き換え結果が別の規則に再度かからないよう、置いたものはすぐ隠す
        text = pat.sub(lambda m, rep=rep: hide(re.match(r".*", rep)), text)  # type: ignore[arg-type]
    return re.sub(r"\x00(\d+)\x00", lambda m: saved[int(m.group(1))], text)


def tracked_files() -> list[Path]:
    out = subprocess.check_output(["git", "ls-files", "-co", "--exclude-standard"], cwd=ROOT, text=True)
    files = []
    for line in out.splitlines():
        p = Path(line)
        if any(part in SKIP_PARTS for part in p.parts) or line in SKIP_FILES:
            continue
        if p.suffix in TEXT_SUFFIXES or p.name in {"Dockerfile", "CODEOWNERS", "NOTICE"}:
            files.append(ROOT / p)
    return files


def move_dirs() -> None:
    for name in MODULES:
        src, dst = ROOT / name, ROOT / (PREFIX + name)
        if src.is_dir() and not dst.exists():
            subprocess.check_call(["git", "mv", name, PREFIX + name], cwd=ROOT)


def main(argv: list[str]) -> int:
    if argv:
        files = [Path(a).resolve() for a in argv]
    else:
        move_dirs()
        files = tracked_files()
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
