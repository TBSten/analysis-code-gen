#!/usr/bin/env python3
"""ドキュメントの構成・分量チェック（ADR-028、pm/14「ドキュメントの構成と分量の基準」）。

人間のレビューを軽くするため、ドキュメントサイトのページが長くなりすぎていないかを検査する。
基準値は docs/doc-lint.toml にあり、パスごとのプロファイル（guide / internal / page）で切り替える。

文字数の数え方
  「本文」= 地の文だけ。コードブロック・表・リスト・見出し・MDX の import/export・
  コンポーネントのタグ（LinkCard など）・HTML コメントは数えない。
  リンクは表示テキストだけ、インラインコードは中身だけを数える。空白（全角空白を含む）は数えない。

検査すること（ルール ID）
  DL001 ページ全体の本文の文字数         DL007 本文に H1 がある（タイトルは frontmatter）
  DL002 1 段落の文字数                   DL008 ガイド: コード例の無い H2 節がある
  DL003 H2 節の本文の文字数              DL009 ガイド: 最後の H2 が Summary → Next steps でない
  DL004 コードブロック 1 個の行数         DL010 ガイド: Summary 節に表が無い
  DL005 H2 の個数                        DL011 閉じていないコードブロック
  DL006 見出しの深さ                     DL012 frontmatter の docLint に未知のプロファイル名

使い方
  python3 scripts/doclint/doc_lint.py [--root DIR] [--config FILE] [--format text|github] [--stats] [PATH ...]

PATH を渡すとそのファイルだけを検査する（include / exclude は使わない。プロファイルはパスで決まる）。
標準ライブラリだけで動く（Python 3.11 以上）。違反があれば終了コード 1。
"""

from __future__ import annotations

import argparse
import re
import sys
import tomllib
from dataclasses import dataclass, field
from pathlib import Path

DEFAULT_CONFIG = "docs/doc-lint.toml"

# ---------------------------------------------------------------- 設定


@dataclass
class Profile:
    name: str
    match: list[str]
    page_chars: int
    paragraph_chars: int
    section_chars: int
    code_lines: int
    h2_max: int
    max_depth: int
    guide_shape: bool


@dataclass
class Config:
    include: list[str]
    exclude: list[str]
    summary_headings: list[str]
    next_steps_headings: list[str]
    profiles: list[Profile]

    def profile_named(self, name: str) -> Profile | None:
        return next((p for p in self.profiles if p.name == name), None)

    def profile_for(self, rel: str) -> Profile:
        for p in self.profiles:
            if any(glob_match(pat, rel) for pat in p.match):
                return p
        raise SystemExit(f"doc-lint: {rel} に合うプロファイルが無い（最後に match = [\"**\"] を置く）")


def load_config(path: Path) -> Config:
    data = tomllib.loads(path.read_text(encoding="utf-8"))
    profiles = [
        Profile(
            name=p["name"],
            match=list(p["match"]),
            page_chars=int(p["page_chars"]),
            paragraph_chars=int(p["paragraph_chars"]),
            section_chars=int(p["section_chars"]),
            code_lines=int(p["code_lines"]),
            h2_max=int(p["h2_max"]),
            max_depth=int(p["max_depth"]),
            guide_shape=bool(p.get("guide_shape", False)),
        )
        for p in data.get("profile", [])
    ]
    if not profiles:
        raise SystemExit(f"doc-lint: {path} に [[profile]] が無い")
    return Config(
        include=list(data.get("include", [])),
        exclude=list(data.get("exclude", [])),
        summary_headings=list(data.get("summary_headings", ["Summary"])),
        next_steps_headings=list(data.get("next_steps_headings", ["Next steps"])),
        profiles=profiles,
    )


_GLOB_CACHE: dict[str, re.Pattern[str]] = {}


def glob_match(pattern: str, rel: str) -> bool:
    """`**` が 0 個以上のディレクトリに、`*` が / 以外に一致する glob。"""
    rx = _GLOB_CACHE.get(pattern)
    if rx is None:
        out, i = "", 0
        while i < len(pattern):
            if pattern.startswith("**/", i):
                out, i = out + "(?:.*/)?", i + 3
            elif pattern.startswith("**", i):
                out, i = out + ".*", i + 2
            elif pattern[i] == "*":
                out, i = out + "[^/]*", i + 1
            elif pattern[i] == "?":
                out, i = out + "[^/]", i + 1
            else:
                out, i = out + re.escape(pattern[i]), i + 1
        rx = _GLOB_CACHE[pattern] = re.compile(out + r"\Z")
    return rx.match(rel) is not None


# ---------------------------------------------------------------- 解析


@dataclass
class CodeBlock:
    line: int
    lines: int


@dataclass
class Paragraph:
    line: int
    chars: int
    text: str


@dataclass
class Section:
    title: str  # 導入部は ""
    line: int
    chars: int = 0
    code_blocks: int = 0
    has_table: bool = False


@dataclass
class Heading:
    level: int
    title: str
    line: int


@dataclass
class Doc:
    frontmatter: dict[str, str] = field(default_factory=dict)
    headings: list[Heading] = field(default_factory=list)
    paragraphs: list[Paragraph] = field(default_factory=list)
    sections: list[Section] = field(default_factory=list)
    code_blocks: list[CodeBlock] = field(default_factory=list)
    unclosed_fence: int | None = None

    @property
    def chars(self) -> int:
        return sum(s.chars for s in self.sections)

    @property
    def h2_sections(self) -> list[Section]:
        return [s for s in self.sections if s.title]


FENCE_RE = re.compile(r"^\s*(`{3,}|~{3,})")
HEADING_RE = re.compile(r"^(#{1,6})\s+(.*?)\s*#*\s*$")
LIST_RE = re.compile(r"^\s*(?:[-*+]|\d+[.)])\s+")
HR_RE = re.compile(r"^\s*(?:-{3,}|\*{3,}|_{3,})\s*$")
TAG_LINE_RE = re.compile(r"^\s*</?[A-Za-z][\w.:-]*")
LINK_DEF_RE = re.compile(r"^\s*\[[^\]]+\]:\s")
CODE_COMPONENT_RE = re.compile(r"^\s*<Code\b")
FRONTMATTER_KV_RE = re.compile(r"^([A-Za-z_][\w-]*)\s*:\s*(.*?)\s*$")

INLINE_SUBS: list[tuple[re.Pattern[str], str]] = [
    (re.compile(r"!\[[^\]]*\]\([^)]*\)"), ""),  # 画像
    (re.compile(r"\[([^\]]*)\]\([^)]*\)"), r"\1"),  # リンク
    (re.compile(r"\[([^\]]*)\]\[[^\]]*\]"), r"\1"),  # 参照リンク
    (re.compile(r"<https?://[^>]+>"), ""),  # 自動リンク
    (re.compile(r"<!--.*?-->"), ""),  # 行内コメント
    (re.compile(r"</?[A-Za-z][^>]*>"), ""),  # 行内タグ
    (re.compile(r"\{/\*.*?\*/\}"), ""),  # MDX コメント
    (re.compile(r"\\(.)"), r"\1"),  # エスケープ
    (re.compile(r"[*`~]"), ""),  # 強調・インラインコードの記号
]


def prose_text(line: str) -> str:
    s = line.strip()
    s = re.sub(r"^(>\s?)+", "", s)  # 引用
    for rx, rep in INLINE_SUBS:
        s = rx.sub(rep, s)
    return s.strip()


def count_chars(text: str) -> int:
    return sum(1 for ch in text if not ch.isspace())


def parse(text: str) -> Doc:
    doc = Doc()
    lines = text.splitlines()
    i = 0

    # frontmatter
    if lines and lines[0].strip() == "---":
        for j in range(1, len(lines)):
            if lines[j].strip() == "---":
                for fl in lines[1:j]:
                    m = FRONTMATTER_KV_RE.match(fl)
                    if m:
                        doc.frontmatter[m.group(1)] = m.group(2).strip("'\"")
                i = j + 1
                break

    section = Section(title="", line=i + 1)
    doc.sections.append(section)
    para: list[str] = []
    para_line = 0
    in_list = False
    in_comment = False
    in_tag = False
    fence: str | None = None
    fence_start = 0
    fence_lines = 0

    def flush() -> None:
        nonlocal para
        if para:
            joined = "".join(para)
            n = count_chars(joined)
            if n:
                doc.paragraphs.append(Paragraph(para_line, n, joined))
                section.chars += n
            para = []

    while i < len(lines):
        raw = lines[i]
        lineno = i + 1
        i += 1
        stripped = raw.strip()

        if fence is not None:
            m = FENCE_RE.match(raw)
            if m and m.group(1)[0] == fence[0] and len(m.group(1)) >= len(fence) and stripped == m.group(1):
                doc.code_blocks.append(CodeBlock(fence_start, fence_lines))
                section.code_blocks += 1
                fence = None
            else:
                fence_lines += 1
            continue
        if in_comment:
            if "-->" in raw:
                in_comment = False
            continue
        if in_tag:
            if ">" in raw:
                in_tag = False
            continue

        m = FENCE_RE.match(raw)
        if m:
            flush()
            in_list = False
            fence, fence_start, fence_lines = m.group(1), lineno, 0
            continue

        if not stripped:
            flush()
            in_list = False if not in_list else "blank"  # type: ignore[assignment]
            continue

        if stripped.startswith("<!--"):
            flush()
            if "-->" not in stripped:
                in_comment = True
            continue

        hm = HEADING_RE.match(raw)
        if hm:
            flush()
            in_list = False
            level, title = len(hm.group(1)), prose_text(hm.group(2))
            doc.headings.append(Heading(level, title, lineno))
            if level <= 2:
                section = Section(title=title or "(無題)", line=lineno)
                doc.sections.append(section)
            continue

        if LIST_RE.match(raw):
            flush()
            in_list = True
            continue
        if in_list is True or (in_list == "blank" and raw.startswith(("  ", "\t"))):
            in_list = True  # リストの続き
            continue
        in_list = False

        if stripped.startswith("|"):
            flush()
            section.has_table = True
            continue
        if HR_RE.match(raw) or stripped.startswith(":::") or LINK_DEF_RE.match(raw):
            flush()
            continue
        if re.match(r"^(import|export)\s", stripped):
            flush()
            continue
        if TAG_LINE_RE.match(raw) or stripped.startswith("{/*"):
            flush()
            if CODE_COMPONENT_RE.match(raw):
                section.code_blocks += 1
            # 1 行で閉じないタグ（属性を複数行に書く LinkCard など）は > まで読み飛ばす
            if stripped.startswith("<") and ">" not in stripped:
                in_tag = True
            continue

        if not para:
            para_line = lineno
        para.append(prose_text(raw))

    flush()
    if fence is not None:
        doc.unclosed_fence = fence_start
    return doc


# ---------------------------------------------------------------- 検査


@dataclass
class Violation:
    path: str
    line: int
    rule: str
    message: str

    def text(self) -> str:
        return f"{self.path}:{self.line}: {self.rule} {self.message}"

    def github(self) -> str:
        msg = self.message.replace("%", "%25").replace("\n", "%0A")
        return f"::error file={self.path},line={self.line},title=doc-lint {self.rule}::{msg}"


def check(rel: str, text: str, config: Config) -> tuple[list[Violation], Doc, Profile]:
    doc = parse(text)
    out: list[Violation] = []

    def v(line: int, rule: str, msg: str) -> None:
        out.append(Violation(rel, line, rule, msg))

    profile = config.profile_for(rel)
    override = doc.frontmatter.get("docLint")
    if override:
        named = config.profile_named(override)
        if named is None:
            names = ", ".join(p.name for p in config.profiles)
            v(1, "DL012", f"docLint: {override} は未知のプロファイル（使えるもの: {names}）")
        else:
            profile = named

    if doc.chars > profile.page_chars:
        v(1, "DL001", f"ページの本文が {doc.chars} 字（上限 {profile.page_chars}）。ページを分けるか、詳細ページへ移す")
    for p in doc.paragraphs:
        if p.chars > profile.paragraph_chars:
            v(p.line, "DL002", f"段落が {p.chars} 字（上限 {profile.paragraph_chars}）。段落を分けるか、文を削る")
    for s in doc.sections:
        if s.chars > profile.section_chars:
            name = f"H2「{s.title}」" if s.title else "導入部（最初の H2 より前）"
            v(s.line, "DL003", f"{name}の本文が {s.chars} 字（上限 {profile.section_chars}）。節を分けるか、詳細ページへ移す")
    for c in doc.code_blocks:
        if c.lines > profile.code_lines:
            v(c.line, "DL004", f"コードブロックが {c.lines} 行（上限 {profile.code_lines}）。要点だけに絞る")
    h2 = [h for h in doc.headings if h.level == 2]
    if len(h2) > profile.h2_max:
        v(h2[profile.h2_max].line, "DL005", f"H2 が {len(h2)} 個（上限 {profile.h2_max}）。ページを分ける")
    for h in doc.headings:
        if h.level == 1:
            v(h.line, "DL007", "本文に H1 がある。タイトルは frontmatter の title に書き、本文は H2 から始める")
        elif h.level > profile.max_depth:
            v(h.line, "DL006", f"H{h.level} は深すぎる（H{profile.max_depth} まで）。構成を浅くする")
    if doc.unclosed_fence is not None:
        v(doc.unclosed_fence, "DL011", "コードブロックが閉じていない")

    if profile.guide_shape:
        check_guide(doc, config, v)
    return out, doc, profile


def check_guide(doc: Doc, config: Config, v) -> None:  # type: ignore[no-untyped-def]
    sections = doc.h2_sections
    summary = set(config.summary_headings)
    nexts = set(config.next_steps_headings)
    for s in sections:
        if s.title in summary or s.title in nexts:
            continue
        if s.code_blocks == 0:
            v(s.line, "DL008", f"H2「{s.title}」にコード例が無い。ガイドの各節は「紹介 1〜2 文 → コード例 → 説明 → 詳細リンク」の型にする")
    titles = [s.title for s in sections]
    want = f"「{config.summary_headings[0]}」→「{config.next_steps_headings[0]}」"
    if len(titles) < 2 or titles[-2] not in summary or titles[-1] not in nexts:
        line = sections[-1].line if sections else 1
        v(line, "DL009", f"最後の H2 が {want} になっていない（ガイドは Summary の表と Next steps で終える）")
    for s in sections:
        if s.title in summary and not s.has_table:
            v(s.line, "DL010", f"H2「{s.title}」に表が無い。ページの要素を表でまとめる")


# ---------------------------------------------------------------- 実行


def collect(root: Path, config: Config) -> list[Path]:
    found: set[Path] = set()
    for pat in config.include:
        for p in root.glob(pat):
            if p.is_file():
                rel = p.relative_to(root).as_posix()
                if not any(glob_match(ex, rel) for ex in config.exclude):
                    found.add(p)
    return sorted(found)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="ドキュメントの構成・分量チェック（ADR-028）")
    ap.add_argument("paths", nargs="*", help="検査するファイル（省略時は設定の include）")
    ap.add_argument("--root", default=".", help="リポジトリ直下（既定: カレント）")
    ap.add_argument("--config", default=None, help=f"設定ファイル（既定: <root>/{DEFAULT_CONFIG}）")
    ap.add_argument("--format", choices=["text", "github"], default="text")
    ap.add_argument("--stats", action="store_true", help="ページごとの計測値も表で出す")
    args = ap.parse_args(argv)

    root = Path(args.root).resolve()
    config = load_config(Path(args.config) if args.config else root / DEFAULT_CONFIG)
    files = [Path(p).resolve() for p in args.paths] if args.paths else collect(root, config)
    if not files:
        print("doc-lint: 検査対象のファイルが無い")
        return 0

    violations: list[Violation] = []
    rows: list[str] = []
    for f in files:
        try:
            rel = f.relative_to(root).as_posix()
        except ValueError:
            rel = f.as_posix()
        vs, doc, profile = check(rel, f.read_text(encoding="utf-8"), config)
        violations.extend(sorted(vs, key=lambda x: (x.line, x.rule)))
        if args.stats:
            max_para = max((p.chars for p in doc.paragraphs), default=0)
            max_sec = max((s.chars for s in doc.sections), default=0)
            max_code = max((c.lines for c in doc.code_blocks), default=0)
            h2 = sum(1 for h in doc.headings if h.level == 2)
            mark = "NG" if vs else "OK"
            rows.append(
                f"| {rel} | {profile.name} | {doc.chars}/{profile.page_chars} | {max_para}/{profile.paragraph_chars} "
                f"| {max_sec}/{profile.section_chars} | {max_code}/{profile.code_lines} | {h2}/{profile.h2_max} | {mark} |"
            )

    for x in violations:
        print(x.github() if args.format == "github" else x.text())
    if rows:
        print("\n| ページ | プロファイル | 本文 | 最大段落 | 最大節 | 最長コード | H2 | 判定 |")
        print("|---|---|---|---|---|---|---|---|")
        print("\n".join(rows))
    bad = len({x.path for x in violations})
    print(f"\ndoc-lint: {len(files)} ファイル中 {bad} ファイルで違反 {len(violations)} 件", file=sys.stderr)
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())
