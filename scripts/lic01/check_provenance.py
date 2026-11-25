#!/usr/bin/env python3
"""LIC-01: 移植ファイルの出所記録・著作権表示・NOTICE を検査する（ADR-018）。

検査すること
  R1 NOTICE がリポジトリ直下にあり、各上流の notice_marker・タグ・コミットを含む
  R2 Ported-From 行を持つファイルが、著作権表示・ライセンス表示・Modified-By・
     Modifications を持ち、Ported-From のコミットが固定済みのものである
  R3 port-map.csv の値が正しく、status=ported の行と実ファイルの記録が一致する
  R4 KSP 由来に見えるのに Ported-From 行が無いファイル（記録漏れ）が無い
  R5 （--upstream-dir を渡したとき）上流のそのコミットにファイルが実在し、
     上流ヘッダの Copyright 行がすべて移植先に残っている

使い方
  python3 scripts/lic01/check_provenance.py [--root DIR] [--upstream-dir ksp=PATH]

標準ライブラリだけで動く。違反があれば終了コード 1。
"""

from __future__ import annotations

import argparse
import csv
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

SOURCE_EXTS = {".kt", ".kts", ".java"}
# 記録漏れ検査（R4）とPorted-From 検索から除く場所
DEFAULT_EXCLUDES = (
    ".git/",
    "build/",
    ".gradle/",
    "node_modules/",
    "porting/",
    "scripts/lic01/",
)
STRATEGIES = {"as-is", "adapt", "reference", "defer", "exclude"}
STATUSES = {"planned", "ported", "not-ported"}
PORT_MAP_COLUMNS = [
    "upstream_path",
    "size_bytes",
    "upstream_copyright",
    "target_module",
    "strategy",
    "phase",
    "status",
    "target_paths",
    "notes",
]

PORTED_FROM_RE = re.compile(
    r"^\s*(?://|\*|#)\s*Ported-From:\s*(?P<owner>[A-Za-z0-9_.-]+)/(?P<repo>[A-Za-z0-9_.-]+)"
    r"@(?P<sha>[^:\s]+):(?P<path>\S+)\s*$"
)
PORTED_FROM_LOOSE_RE = re.compile(r"Ported-From:")
MODIFIED_BY_RE = re.compile(r"^\s*(?://|\*|#)\s*Modified-By:\s*\S.*$")
MODIFICATIONS_RE = re.compile(r"^\s*(?://|\*|#)\s*Modifications:\s*(?P<body>.*)$")
COPYRIGHT_RE = re.compile(r"Copyright\b", re.IGNORECASE)
LICENSE_RES = (
    re.compile(r"Licensed under the Apache License,\s*Version 2\.0"),
    re.compile(r"governed by the Apache 2\.0 license"),
)
SHA_RE = re.compile(r"^[0-9a-f]{40}$")
# 記録漏れの手がかり: KSP の著作権表示、または KSP のパッケージ宣言
SUSPECT_RES = (
    re.compile(r"Copyright\s+(?:\d{4}(?:-\d{4})?\s+)?Google LLC"),
    re.compile(r"^\s*package\s+com\.google\.devtools\.ksp\b", re.MULTILINE),
)


@dataclass
class Upstream:
    id: str
    dir: Path
    props: dict[str, str]
    rows: list[dict[str, str]] = field(default_factory=list)

    @property
    def repo_name(self) -> str:
        # https://github.com/google/ksp -> ksp
        return self.props.get("repository", "").rstrip("/").split("/")[-1]

    @property
    def owner(self) -> str:
        parts = self.props.get("repository", "").rstrip("/").split("/")
        return parts[-2] if len(parts) >= 2 else ""

    @property
    def allowed_commits(self) -> set[str]:
        commits = {self.props.get("commit", "")}
        prev = self.props.get("previous_commits", "")
        commits.update(c.strip() for c in prev.split(",") if c.strip())
        commits.discard("")
        return commits


@dataclass
class PortedRef:
    file: Path
    line: int
    owner: str
    repo: str
    sha: str
    path: str


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []

    def error(self, rule: str, where: str, msg: str) -> None:
        self.errors.append(f"[{rule}] {where}: {msg}")


def read_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        k, v = line.split("=", 1)
        props[k.strip()] = v.strip()
    return props


def load_upstreams(root: Path, report: Report) -> dict[str, Upstream]:
    ups: dict[str, Upstream] = {}
    porting = root / "porting"
    if not porting.is_dir():
        return ups
    for d in sorted(p for p in porting.iterdir() if p.is_dir()):
        props_file = d / "UPSTREAM.properties"
        if not props_file.is_file():
            continue
        rel = props_file.relative_to(root).as_posix()
        props = read_properties(props_file)
        for key in ("id", "repository", "commit", "tag", "notice_marker"):
            if not props.get(key):
                report.error("R3", rel, f"必須キー {key} が無い")
        if props.get("commit") and not SHA_RE.match(props["commit"]):
            report.error("R3", rel, f"commit が 40 桁の SHA ではない: {props['commit']}")
        if props.get("id") and props["id"] != d.name:
            report.error("R3", rel, f"id ({props['id']}) とディレクトリ名 ({d.name}) が違う")
        up = Upstream(id=d.name, dir=d, props=props)
        map_file = d / "port-map.csv"
        if map_file.is_file():
            up.rows = load_port_map(root, map_file, report)
        else:
            report.error("R3", d.relative_to(root).as_posix(), "port-map.csv が無い")
        ups[d.name] = up
    return ups


def load_port_map(root: Path, map_file: Path, report: Report) -> list[dict[str, str]]:
    rel = map_file.relative_to(root).as_posix()
    with map_file.open(encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        if reader.fieldnames != PORT_MAP_COLUMNS:
            report.error("R3", rel, f"列が {PORT_MAP_COLUMNS} と違う: {reader.fieldnames}")
            return []
        rows = list(reader)
    seen: set[str] = set()
    for i, row in enumerate(rows, start=2):
        where = f"{rel}:{i}"
        up = row["upstream_path"]
        if up in seen:
            report.error("R3", where, f"upstream_path が重複: {up}")
        seen.add(up)
        if row["strategy"] not in STRATEGIES:
            report.error("R3", where, f"strategy が不正: {row['strategy']}（{sorted(STRATEGIES)}）")
        if row["status"] not in STATUSES:
            report.error("R3", where, f"status が不正: {row['status']}（{sorted(STATUSES)}）")
        targets = split_list(row["target_paths"])
        if row["status"] == "ported" and not targets:
            report.error("R3", where, "status=ported なのに target_paths が空")
        if row["status"] == "ported" and row["strategy"] in {"reference", "exclude"}:
            report.error("R3", where, f"strategy={row['strategy']} のファイルは ported にしない")
        if row["status"] != "ported" and targets:
            report.error("R3", where, "status が ported でないのに target_paths がある")
    return rows


def split_list(value: str) -> list[str]:
    return [v.strip() for v in value.split(";") if v.strip()]


def is_excluded(rel: str, excludes: tuple[str, ...]) -> bool:
    rel_slash = rel + ("/" if not rel.endswith("/") else "")
    for ex in excludes:
        if ex.endswith("/"):
            if rel_slash.startswith(ex) or f"/{ex}" in f"/{rel_slash}":
                return True
        elif rel == ex:
            return True
    return False


def iter_source_files(root: Path, excludes: tuple[str, ...]):
    for dirpath, dirnames, filenames in os.walk(root):
        reldir = Path(dirpath).relative_to(root).as_posix()
        reldir = "" if reldir == "." else reldir
        keep = []
        for d in dirnames:
            r = f"{reldir}/{d}" if reldir else d
            if not is_excluded(r + "/", excludes):
                keep.append(d)
        dirnames[:] = keep
        for name in filenames:
            p = Path(dirpath) / name
            if p.suffix not in SOURCE_EXTS:
                continue
            rel = p.relative_to(root).as_posix()
            if is_excluded(rel, excludes):
                continue
            yield p


def check_notice(root: Path, ups: dict[str, Upstream], report: Report) -> None:
    notice = root / "NOTICE"
    if not ups:
        return
    if not notice.is_file():
        report.error("R1", "NOTICE", "リポジトリ直下に NOTICE が無い")
        return
    text = notice.read_text(encoding="utf-8")
    for up in ups.values():
        for key in ("notice_marker", "tag", "commit"):
            val = up.props.get(key, "")
            if val and val not in text:
                report.error("R1", "NOTICE", f"{up.id} の {key} ({val}) が書かれていない")
        if "Apache License" not in text:
            report.error("R1", "NOTICE", "Apache License への言及が無い")


def check_file(root: Path, path: Path, ups: dict[str, Upstream], report: Report) -> list[PortedRef]:
    rel = path.relative_to(root).as_posix()
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        return []
    refs: list[PortedRef] = []
    first_ref_line = None
    for i, line in enumerate(lines, start=1):
        if not PORTED_FROM_LOOSE_RE.search(line):
            continue
        m = PORTED_FROM_RE.match(line)
        if not m:
            report.error("R2", f"{rel}:{i}", "Ported-From の書式が違う（例: // Ported-From: google/ksp@<40桁SHA>:<path>）")
            continue
        refs.append(PortedRef(path, i, m["owner"], m["repo"], m["sha"], m["path"]))
        first_ref_line = first_ref_line or i
    if not refs:
        text = "\n".join(lines)
        if any(r.search(text) for r in SUSPECT_RES):
            report.error("R4", rel, "KSP 由来に見えるが Ported-From 行が無い（出所記録の漏れ）")
        return []

    header = "\n".join(lines[: first_ref_line - 1])
    if not COPYRIGHT_RE.search(header):
        report.error("R2", rel, "Ported-From より前に著作権表示（Copyright 行）が無い")
    if not any(r.search(header) for r in LICENSE_RES):
        report.error("R2", rel, "Ported-From より前にライセンス表示（Apache License 2.0）が無い")
    if not any(MODIFIED_BY_RE.match(l) for l in lines):
        report.error("R2", rel, "Modified-By 行が無い")
    mods = [MODIFICATIONS_RE.match(l) for l in lines]
    mods = [m for m in mods if m]
    if not mods:
        report.error("R2", rel, "Modifications 行が無い")
    elif not any(m["body"].strip() for m in mods):
        report.error("R2", rel, "Modifications の内容が空（変更が無いなら「なし（パッケージ名のみ変更）」等と書く）")

    for ref in refs:
        where = f"{rel}:{ref.line}"
        up = next((u for u in ups.values() if u.owner == ref.owner and u.repo_name == ref.repo), None)
        if up is None:
            report.error("R2", where, f"{ref.owner}/{ref.repo} は porting/ に登録されていない上流")
            continue
        if not SHA_RE.match(ref.sha):
            report.error("R2", where, f"コミットは 40 桁の SHA で書く（タグ・短縮 SHA は不可）: {ref.sha}")
        elif ref.sha not in up.allowed_commits:
            report.error("R2", where, f"コミット {ref.sha} は {up.id} の固定コミットではない（UPSTREAM.properties の commit / previous_commits）")
        row = next((r for r in up.rows if r["upstream_path"] == ref.path), None)
        if row is None:
            report.error("R3", where, f"{ref.path} が {up.id}/port-map.csv に無い")
        elif row["status"] != "ported":
            report.error("R3", where, f"{ref.path} の status が ported になっていない（{row['status']}）")
        elif rel not in split_list(row["target_paths"]):
            report.error("R3", where, f"{ref.path} の target_paths にこのファイル ({rel}) が無い")
    return refs


def check_port_map_targets(root: Path, ups: dict[str, Upstream], report: Report,
                           refs_by_file: dict[str, list[PortedRef]]) -> None:
    for up in ups.values():
        for row in up.rows:
            if row["status"] != "ported":
                continue
            for target in split_list(row["target_paths"]):
                where = f"porting/{up.id}/port-map.csv ({row['upstream_path']})"
                if not (root / target).is_file():
                    report.error("R3", where, f"target_paths のファイルが無い: {target}")
                    continue
                refs = refs_by_file.get(target, [])
                if not any(r.path == row["upstream_path"] and r.repo == up.repo_name for r in refs):
                    report.error("R3", where, f"{target} に {row['upstream_path']} の Ported-From 行が無い")


def git_show(upstream_dir: Path, sha: str, path: str) -> str | None:
    res = subprocess.run(
        ["git", "-C", str(upstream_dir), "show", f"{sha}:{path}"],
        capture_output=True, text=True,
    )
    return res.stdout if res.returncode == 0 else None


def check_against_upstream(root: Path, ups: dict[str, Upstream], upstream_dirs: dict[str, Path],
                           refs_by_file: dict[str, list[PortedRef]], report: Report) -> None:
    for rel, refs in refs_by_file.items():
        ported_text = (root / rel).read_text(encoding="utf-8")
        for ref in refs:
            up = next((u for u in ups.values() if u.owner == ref.owner and u.repo_name == ref.repo), None)
            if up is None or up.id not in upstream_dirs:
                continue
            original = git_show(upstream_dirs[up.id], ref.sha, ref.path)
            where = f"{rel}:{ref.line}"
            if original is None:
                report.error("R5", where, f"上流 {up.id}@{ref.sha} に {ref.path} が無い")
                continue
            head = original.splitlines()[:30]
            for line in head:
                if COPYRIGHT_RE.search(line):
                    notice = line.strip().lstrip("/*# ").strip()
                    if notice and notice not in ported_text:
                        report.error("R5", where, f"上流の著作権表示が残っていない: {notice}")


def parse_upstream_dirs(values: list[str]) -> dict[str, Path]:
    out: dict[str, Path] = {}
    for v in values:
        if "=" not in v:
            raise SystemExit(f"--upstream-dir は id=PATH の形で渡す: {v}")
        k, p = v.split("=", 1)
        out[k] = Path(p)
    return out


def run(root: Path, upstream_dirs: dict[str, Path], excludes: tuple[str, ...]) -> Report:
    report = Report()
    ups = load_upstreams(root, report)
    check_notice(root, ups, report)
    refs_by_file: dict[str, list[PortedRef]] = {}
    for path in iter_source_files(root, excludes):
        refs = check_file(root, path, ups, report)
        if refs:
            refs_by_file[path.relative_to(root).as_posix()] = refs
    check_port_map_targets(root, ups, report, refs_by_file)
    if upstream_dirs:
        check_against_upstream(root, ups, upstream_dirs, refs_by_file, report)
    report.ported_files = len(refs_by_file)  # type: ignore[attr-defined]
    report.planned = sum(1 for u in ups.values() for r in u.rows if r["status"] == "planned")  # type: ignore[attr-defined]
    report.ported_rows = sum(1 for u in ups.values() for r in u.rows if r["status"] == "ported")  # type: ignore[attr-defined]
    return report


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=".", help="リポジトリのルート（既定: カレント）")
    ap.add_argument("--upstream-dir", action="append", default=[],
                    help="上流の git checkout。id=PATH（例: ksp=/tmp/ksp）。R5 を有効にする")
    ap.add_argument("--exclude", action="append", default=[],
                    help="検査から外すパス（末尾 / でディレクトリ）。既定の除外に追加される")
    args = ap.parse_args(argv)
    root = Path(args.root).resolve()
    report = run(root, parse_upstream_dirs(args.upstream_dir), DEFAULT_EXCLUDES + tuple(args.exclude))
    summary = (f"移植済みファイル {report.ported_files} 件 / port-map の ported {report.ported_rows} 行・"  # type: ignore[attr-defined]
               f"planned {report.planned} 行")  # type: ignore[attr-defined]
    if report.errors:
        for e in report.errors:
            print(e)
        print(f"LIC-01: FAIL（違反 {len(report.errors)} 件。{summary}）")
        return 1
    print(f"LIC-01: PASS（{summary}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
