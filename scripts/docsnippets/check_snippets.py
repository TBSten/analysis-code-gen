#!/usr/bin/env python3
"""docs サイトのコード例が、出所のフィクスチャと一致しているかを検査する（ADR-031）。

frontmatter に `fixture: <リポジトリ直下からのディレクトリ>` を書いたページでは、
`title="<フィクスチャ内の相対パス>"` を付けたコードブロックの中身が、そのファイルの一部と
一字一句同じ（行末の空白は無視）でなければならない。title の無いコードブロック（コマンドや出力）は検査しない。

フィクスチャは E2E（DOC-02・03）がビルド・実行するので、ページの例が「動くコード」から離れないようにする。

使い方:
  python3 scripts/docsnippets/check_snippets.py [--root DIR] [PATH ...]
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path

DOCS_GLOB = "docs/site/src/content/docs/**/*.md*"
FENCE = re.compile(r"^(?P<indent>\s*)(?P<fence>`{3,}|~{3,})(?P<info>.*)$")
TITLE = re.compile(r"""title=(?:"([^"]+)"|'([^']+)')""")


@dataclass
class Block:
    line: int
    title: str | None
    body: str


def frontmatter(text: str) -> dict[str, str]:
    if not text.startswith("---\n"):
        return {}
    end = text.find("\n---", 4)
    if end < 0:
        return {}
    result = {}
    for raw in text[4:end].splitlines():
        if ":" in raw and not raw.startswith((" ", "\t")):
            key, value = raw.split(":", 1)
            result[key.strip()] = value.strip().strip("\"'")
    return result


def code_blocks(text: str) -> list[Block]:
    blocks: list[Block] = []
    lines = text.splitlines()
    i = 0
    while i < len(lines):
        m = FENCE.match(lines[i])
        if not m:
            i += 1
            continue
        fence, info, start = m.group("fence"), m.group("info"), i
        t = TITLE.search(info)
        body: list[str] = []
        i += 1
        while i < len(lines) and not lines[i].strip().startswith(fence):
            body.append(lines[i])
            i += 1
        blocks.append(Block(start + 1, (t.group(1) or t.group(2)) if t else None, "\n".join(body)))
        i += 1
    return blocks


def normalize(text: str) -> str:
    return "\n".join(line.rstrip() for line in text.strip("\n").splitlines())


def check(page: Path, root: Path) -> list[str]:
    text = page.read_text(encoding="utf-8")
    fixture = frontmatter(text).get("fixture")
    rel = page.relative_to(root).as_posix() if page.is_relative_to(root) else str(page)
    if not fixture:
        return []
    base = root / fixture
    if not base.is_dir():
        return [f"{rel}:1: fixture のディレクトリ {fixture} がありません"]
    errors = []
    for block in code_blocks(text):
        if block.title is None:
            continue
        target = base / block.title
        if not target.is_file():
            errors.append(f"{rel}:{block.line}: {fixture}/{block.title} がありません")
        elif normalize(block.body) not in normalize(target.read_text(encoding="utf-8")):
            errors.append(f"{rel}:{block.line}: コード例が {fixture}/{block.title} の中身と一致しません")
    return errors


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="docs のコード例とフィクスチャの一致を検査する（ADR-031）")
    ap.add_argument("paths", nargs="*")
    ap.add_argument("--root", default=".")
    args = ap.parse_args(argv)
    root = Path(args.root).resolve()
    pages = [Path(p).resolve() for p in args.paths] if args.paths else sorted(root.glob(DOCS_GLOB))
    errors = [e for p in pages for e in check(p, root)]
    for e in errors:
        print(e)
    checked = sum(1 for p in pages if frontmatter(p.read_text(encoding="utf-8")).get("fixture"))
    print(f"{len(pages)} ページ（fixture 付き {checked}）を検査し、不一致 {len(errors)} 件", file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
