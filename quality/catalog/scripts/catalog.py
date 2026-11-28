#!/usr/bin/env python3
"""入力パターンカタログの取り込み・索引づくり・網羅表づくり（ADR-020）。

標準ライブラリだけで動く（Python 3.11 以上）。

  python3 quality/catalog/scripts/catalog.py index            # index.json と coverage.md を作り直す
  python3 quality/catalog/scripts/catalog.py check            # 形式・ID・必須の網羅を確かめる（CI 用）
  python3 quality/catalog/scripts/catalog.py import-cream <checkout>
  python3 quality/catalog/scripts/catalog.py import-moshi <checkout>
  python3 quality/catalog/scripts/catalog.py import-koin  <checkout>
  python3 quality/catalog/scripts/catalog.py import-real  <processor> <owner/repo> <checkout>
  python3 quality/catalog/scripts/catalog.py import-all   <cache-dir>   # processors.toml の固定コミットを取得して全部取り込む
  python3 quality/catalog/scripts/catalog.py apply-k      <tsv>         # K（KSP2）の判定を STATUS・NOTE に反映する（ADR-072）

取り込みは決定的に動く。同じ出所は id-map.tsv で同じ ID に対応づけるので、取り込み直しても ID は変わらない。
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import sys
import tomllib
import zlib
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent  # quality/catalog
PATTERNS = ROOT / "patterns"
ID_MAP = ROOT / "id-map.tsv"
SHARDS = 24

PROC_PREFIX = {"common": "CMN", "cream": "CRM", "moshi": "MSH", "koin": "KOI"}
# PROCESSORS に書ける名前（"*" は背景コード）。XD-01（ACG-40）も同じ一覧で検査する
KNOWN_PROCESSORS = {"*", "cream", "moshi", "koin", "kotlin-inject", "dagger", "room"}
SRC_LETTER = {"test": "T", "real": "R", "own": "O"}
KNOWN_HEADERS = {
    "PATTERN", "TITLE", "PROCESSORS", "EXPECT", "EXPECT_MESSAGE", "OPTIONS", "TAGS",
    "ORIGIN", "LICENSE", "STATUS", "NOTE",
}
SOURCE_SET_PLATFORM = {"jvmMain", "androidMain", "iosMain", "iosArm64Main", "iosX64Main",
                       "iosSimulatorArm64Main", "jsMain", "wasmJsMain", "macosMain", "linuxMain",
                       "mingwMain", "desktopMain", "iosTest", "jvmTest", "androidUnitTest"}
SOURCE_SET_INTERMEDIATE = {"appleMain", "nativeMain", "nonJvmMain", "jvmCommonMain", "mobileMain",
                           "nonWebMain", "nonAndroidMain", "webMain", "skikoMain", "jvmAndroidMain"}


# ============================================================ パターンのモデル

@dataclass
class SrcFile:
    path: str            # FILE: の値（"<module>/<sourceSet>/<相対パス>" か、ただのファイル名）
    content: str

    @property
    def module(self) -> str:
        parts = self.path.split("/")
        return parts[0] if len(parts) >= 3 and parts[1].endswith(("Main", "Test", "main", "test")) else ""

    @property
    def source_set(self) -> str:
        parts = self.path.split("/")
        if len(parts) >= 3 and parts[1].endswith(("Main", "Test", "main", "test")):
            return parts[1]
        if len(parts) >= 2 and parts[0].endswith(("Main", "Test")) or parts[0] in ("main", "test"):
            return parts[0]
        return "main"


@dataclass
class Pattern:
    id: str
    title: str
    processors: list[str]
    expect: str                     # ok / error / warning / compile-error
    expect_message: str = ""
    options: list[dict] = field(default_factory=list)
    tags: list[str] = field(default_factory=list)
    origin: str = "own"
    license: str = ""
    status: str = "candidate"       # candidate / verified（K で EXPECT どおりと確かめた）/ rejected（K で EXPECT と違った。NOTE に理由）
    note: str = ""
    files: list[SrcFile] = field(default_factory=list)
    path: Path | None = None

    @property
    def source(self) -> str:        # test / real / own
        return {"T": "test", "R": "real", "O": "own"}[self.id.split("-")[1]]

    @property
    def owner(self) -> str:         # common / cream / moshi / koin
        pre = self.id.split("-")[0]
        return {v: k for k, v in PROC_PREFIX.items()}[pre]

    def render(self) -> str:
        out = [f"// PATTERN: {self.id}", f"// TITLE: {self.title}",
               f"// PROCESSORS: {', '.join(self.processors)}", f"// EXPECT: {self.expect}"]
        if self.expect_message:
            out.append(f"// EXPECT_MESSAGE: {one_line(self.expect_message)}")
        for o in self.options:
            out.append("// OPTIONS: " + ", ".join(f"{k}={v}" for k, v in o.items()))
        if self.tags:
            out.append(f"// TAGS: {', '.join(self.tags)}")
        out.append(f"// ORIGIN: {self.origin}")
        if self.license:
            out.append(f"// LICENSE: {self.license}")
        out.append(f"// STATUS: {self.status}")
        if self.note:
            out.append(f"// NOTE: {one_line(self.note)}")
        for f in self.files:
            out.append(f"// FILE: {f.path}")
            out.append(f.content.rstrip("\n"))
        return "\n".join(out) + "\n"


def one_line(s: str) -> str:
    return re.sub(r"\s+", " ", s).strip()


def parse_pattern(path: Path) -> Pattern:
    text = path.read_text(encoding="utf-8")
    headers: dict[str, list[str]] = defaultdict(list)
    files: list[SrcFile] = []
    cur: list[str] | None = None
    cur_path = ""
    for line in text.splitlines():
        m = re.match(r"^// FILE: (.+)$", line)
        if m:
            if cur is not None:
                files.append(SrcFile(cur_path, "\n".join(cur) + "\n"))
            cur, cur_path = [], m.group(1).strip()
            continue
        if cur is None:
            m = re.match(r"^// ([A-Z_]+): ?(.*)$", line)
            if not m or m.group(1) not in KNOWN_HEADERS:
                raise ValueError(f"{path}: ヘッダーの外に不明な行があります: {line!r}")
            headers[m.group(1)].append(m.group(2).strip())
        else:
            cur.append(line)
    if cur is not None:
        files.append(SrcFile(cur_path, "\n".join(cur) + "\n"))
    for req in ("PATTERN", "TITLE", "PROCESSORS", "EXPECT", "ORIGIN", "STATUS"):
        if not headers.get(req):
            raise ValueError(f"{path}: {req} がありません")
    if not files:
        raise ValueError(f"{path}: FILE がありません")
    opts = []
    for o in headers.get("OPTIONS", []):
        d = {}
        for kv in split_list(o):
            k, _, v = kv.partition("=")
            d[k.strip()] = v.strip()
        opts.append(d)
    return Pattern(
        id=headers["PATTERN"][0], title=headers["TITLE"][0],
        processors=split_list(headers["PROCESSORS"][0]), expect=headers["EXPECT"][0],
        expect_message=" ".join(headers.get("EXPECT_MESSAGE", [])), options=opts,
        tags=[t for h in headers.get("TAGS", []) for t in split_list(h)],
        origin=headers["ORIGIN"][0], license=" ".join(headers.get("LICENSE", [])),
        status=headers["STATUS"][0], note=" ".join(headers.get("NOTE", [])), files=files, path=path,
    )


def split_list(s: str) -> list[str]:
    return [x.strip() for x in s.split(",") if x.strip()]


def load_patterns() -> list[Pattern]:
    return [parse_pattern(p) for p in sorted(PATTERNS.rglob("*.kt"))]


# ============================================================ 次元と自動タグ

def load_dims() -> dict:
    return tomllib.loads((ROOT / "dimensions.toml").read_text(encoding="utf-8"))["dim"]


def strip_comments(code: str) -> str:
    # 行コメントと /* */ を消す。KDoc（/**）は kt.misc=kdoc の判定に使うので印だけ残す。
    code = re.sub(r"/\*\*.*?\*/", "/** */", code, flags=re.S)
    code = re.sub(r"/\*(?!\*).*?\*/", "", code, flags=re.S)
    return "\n".join(l for l in code.splitlines() if not l.lstrip().startswith("//"))


_dim_cache: dict | None = None


def compiled_dims() -> dict:
    global _dim_cache
    if _dim_cache is None:
        dims = load_dims()
        for d in dims.values():
            for v in d["values"]:
                v["_re"] = re.compile(v["match"], re.M) if "match" in v else None
        _dim_cache = dims
    return _dim_cache


def auto_tags(p: Pattern) -> set[str]:
    dims = compiled_dims()
    kotlin = "\n".join(strip_comments(f.content) for f in p.files)
    tags: set[str] = set()
    for dname, d in dims.items():
        if d["scope"] != "all" and d["scope"] not in p.processors:
            continue
        for v in d["values"]:
            if v["_re"] is not None and v["_re"].search(kotlin):
                tags.add(f"{dname}={v['id']}")
    # 配置
    pkgs = {m.group(1) for f in p.files for m in [re.search(r"^package\s+([\w.]+)", f.content, re.M)] if m}
    sets = {f.source_set for f in p.files}
    mods = {f.module for f in p.files}
    tags.add("kt.layout=" + ("multi-file" if len(p.files) > 1 else "single-file"))
    if len(pkgs) > 1:
        tags.add("kt.layout=multi-package")
    if any(not re.search(r"^package\s", f.content, re.M) for f in p.files if f.path.endswith(".kt")):
        tags.add("kt.layout=default-package")
    if any(f.path.endswith(".java") for f in p.files):
        tags.add("kt.layout=java-source")
        tags.add("kt.types=platform-type")
    if len(mods) > 1:
        tags.add("kt.layout=multi-module")
    if "commonMain" in sets or "commonTest" in sets:
        tags.add("kt.layout=kmp-common")
    if sets & SOURCE_SET_PLATFORM:
        tags.add("kt.layout=kmp-platform")
    if sets & SOURCE_SET_INTERMEDIATE:
        tags.add("kt.layout=kmp-intermediate")
    if "androidMain" in sets or re.search(r"^import android(x)?\.", kotlin, re.M):
        tags.add("kt.layout=android")
    if any(s.lower().endswith("test") for s in sets):
        tags.add("kt.layout=test-source-set")
    # 結果と組み合わせ
    tags.add("kt.outcome=" + {"ok": "ok", "error": "processor-error", "warning": "processor-warning",
                              "compile-error": "compile-error"}[p.expect])
    if "*" not in p.processors:  # "*" は Processor を問わない背景コード（どの組み合わせにも混ぜる）
        tags.add("proc.combo=" + ("multi" if len(p.processors) > 1 else "single"))
    # オプション（既定値以外を指定したキー）
    for o in p.options:
        for k in o:
            proc = next((x for x in p.processors if f"{x}.option" in dims), None)
            for pr in p.processors:
                dn = f"{pr}.option"
                if dn in dims:
                    ids = {v["id"] for v in dims[dn]["values"]}
                    short = k.split(".", 1)[1] if k.startswith(pr + ".") and k.split(".", 1)[1] in ids else k
                    if short in ids:
                        tags.add(f"{dn}={short}")
    return tags


def all_tags(p: Pattern) -> set[str]:
    return auto_tags(p) | set(p.tags)


# ============================================================ ID の対応表

class IdMap:
    def __init__(self):
        self.by_key: dict[str, str] = {}
        if ID_MAP.exists():
            for line in ID_MAP.read_text(encoding="utf-8").splitlines():
                if line and not line.startswith("#"):
                    k, v = line.split("\t")
                    self.by_key[k] = v

    def get(self, owner: str, src: str, key: str) -> str:
        if key in self.by_key:
            return self.by_key[key]
        prefix = f"{PROC_PREFIX[owner]}-{SRC_LETTER[src]}-"
        used = [int(v[len(prefix):]) for v in self.by_key.values() if v.startswith(prefix)]
        own = [int(p.stem[len(prefix):]) for p in (PATTERNS / owner).glob(prefix + "*.kt")]
        n = max(used + own + [0]) + 1
        new = f"{prefix}{n:04d}"
        self.by_key[key] = new
        return new

    def save(self):
        lines = ["# 出所のキー → パターン ID（取り込み直しても ID を変えないための表。手で編集しない）"]
        lines += [f"{k}\t{v}" for k, v in sorted(self.by_key.items(), key=lambda kv: kv[1])]
        ID_MAP.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_patterns(owner: str, src: str, pats: list[Pattern]):
    """取り込み結果を書き出す。同じ出所種別の古いファイルのうち、今回出てこなかったものは消す。"""
    prefix = f"{PROC_PREFIX[owner]}-{SRC_LETTER[src]}-"
    d = PATTERNS / owner
    d.mkdir(parents=True, exist_ok=True)
    keep = set()
    for p in pats:
        f = d / f"{p.id}.kt"
        if f.exists():
            carry_k_status(p, f)
        f.write_text(p.render(), encoding="utf-8")
        keep.add(p.id)
    return keep, prefix


def carry_k_status(p: Pattern, old_file: Path):
    """取り込み直したとき、K に関わる中身（コード・EXPECT・OPTIONS・PROCESSORS）が前と同じなら K の判定を引き継ぐ（ADR-072）。
    変わっていれば candidate に戻り、次の verifyCatalog で判定し直す。"""
    try:
        old = parse_pattern(old_file)
    except ValueError:
        return
    if k_key(old) == k_key(p) and old.status != "candidate":
        p.status = old.status
        k = K_NOTE.search(old.note)
        p.note = (strip_k_note(p.note) + (" " + k.group(0) if k else "")).strip()


def k_key(p: Pattern):
    return (p.processors, p.expect, p.expect_message, p.options, [(f.path, f.content) for f in p.files])


# K の判定を書いた NOTE の部分（apply-k が付ける）と、取り込み時の「K で確かめるまで candidate」の決まり文句
K_NOTE = re.compile(r"K（KSP2 [^）]*）: .*$")
K_PENDING = "、K で確かめるまで candidate"


def strip_k_note(note: str) -> str:
    return K_NOTE.sub("", note).replace(K_PENDING, "").strip()


def ksp_version() -> str:
    m = re.search(r'(?m)^ksp\s*=\s*"([^"]+)"', (ROOT.parent.parent / "gradle/libs.versions.toml").read_text(encoding="utf-8"))
    return m.group(1) if m else "?"


def apply_k(tsv: Path) -> int:
    """verifyCatalog（e2e-tests）の TSV を読み、verified / rejected を各パターンに書く。skip・error の行は変えない。"""
    rows = {}
    for line in tsv.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cols = line.split("\t")
        rows[cols[0]] = (cols[1], cols[2] if len(cols) > 2 else "")
    ver = ksp_version()
    changed = Counter()
    for p in load_patterns():
        if p.id not in rows:
            continue
        status, reason = rows[p.id]
        if status not in ("verified", "rejected"):
            changed[f"変えない({status})"] += 1
            continue
        base = strip_k_note(p.note)
        p.status = status
        p.note = base if status == "verified" else (base + f" K（KSP2 {ver}）: {reason}").strip()
        p.path.write_text(p.render(), encoding="utf-8")
        changed[status] += 1
    print("apply-k: " + "、".join(f"{k} {v} 件" for k, v in sorted(changed.items())))
    return 0


def prune(owner: str, prefix: str, keep_all: set[str], origin_prefix: str):
    for f in (PATTERNS / owner).glob(prefix + "*.kt"):
        if f.stem in keep_all:
            continue
        try:
            if parse_pattern(f).origin.startswith(origin_prefix):
                f.unlink()
        except ValueError:
            pass


REGISTRY = ROOT.parent.parent / "fixtures/processors/processors.toml"  # ADR-019 の唯一の一覧
REGISTRY_ID = {"cream": "CRM", "moshi": "MSH", "koin": "KOI"}


def registry() -> dict:
    return tomllib.loads(REGISTRY.read_text(encoding="utf-8"))


def upstream(proc: str) -> dict:
    """① の取り込み元（Processor の固定コミット）を processors.toml から読む。"""
    for e in registry()["processor"]:
        if e["id"] == REGISTRY_ID[proc]:
            m = re.match(r"github:([\w.-]+/[\w.-]+)@([0-9a-f]{40})$", e["source"])
            if not m:
                raise SystemExit(f"{proc}: GitHub の固定コミットがありません: {e['source']}")
            return {"repo": m.group(1), "commit": m.group(2), "license": e["license"], "version": e["version"]}
    raise SystemExit(f"processors.toml に {proc} がありません")


def project(proc: str, repo: str) -> dict:
    """② の実プロジェクトを processors.toml から読む。uses に Processor が無ければ取り込まない。"""
    for e in registry().get("project", []):
        if e["repo"] == repo:
            if REGISTRY_ID[proc] not in e["uses"]:
                raise SystemExit(f"{repo} は processors.toml で {proc} を使うことになっていません")
            return e
    raise SystemExit(f"processors.toml の [[project]] に {repo} がありません。先に一覧へ足してください（ADR-019）")


def check_checkout(checkout: Path, commit: str):
    head = (checkout / ".git/HEAD").read_text().strip() if (checkout / ".git/HEAD").exists() else ""
    if head and not head.startswith("ref:") and head != commit:
        raise SystemExit(f"{checkout} は {head[:12]} です。固定コミット {commit[:12]} を checkout してください")


def copy_license(repo: str, checkout: Path):
    """取り込み元のライセンス文を licenses/ に写す（MIT は著作権表示と許諾文の同梱が要る）。"""
    for name in ("LICENSE", "LICENSE.txt", "LICENSE.TXT", "LICENSE.md", "COPYING"):
        f = checkout / name
        if f.exists():
            d = ROOT / "licenses"
            d.mkdir(exist_ok=True)
            (d / (repo.replace("/", "__") + ".txt")).write_text(f.read_text(encoding="utf-8", errors="replace"),
                                                               encoding="utf-8")
            return
    raise SystemExit(f"{repo}: ライセンスファイルが見つかりません")


def norm_hash(s: str) -> str:
    return hashlib.sha1(re.sub(r"\s+", " ", s).strip().encode()).hexdigest()[:12]


# ============================================================ ① cream.kt

def import_cream(checkout: Path):
    src = upstream("cream")
    check_checkout(checkout, src["commit"])
    copy_license(src["repo"], checkout)
    ids = IdMap()
    snap = checkout / "cream-ksp/src/test/resources/snapshots"
    seen: dict[str, Pattern] = {}
    for md in sorted(snap.rglob("*.md")):
        rel = md.relative_to(checkout).as_posix()
        text = md.read_text(encoding="utf-8")
        inputs = re.findall(r"^## Input[^\n]*\n+```kt\n(.*?)^```", text, re.M | re.S)
        if not inputs:
            continue
        test = md.relative_to(snap).parts[0]
        family = next((m.group(1) for part in md.relative_to(snap).parts
                       for m in [re.match(r"\d+--(\w+)$", part)] if m), "")
        family = {"excludes": "exclude"}.get(family, family)
        if test.endswith("InvalidUsageTest") or test in ("MultipleDiagnosticsTest",):
            family = "invalidUsage"
        elif test == "OptionsDiagnosticTest":
            family = "options"
        elif "/UseCase/" in rel:
            family = "useCase"
        opts = {}
        mo = re.search(r"^## KSP options\n+```kt\n(.*?)^```", text, re.M | re.S)
        if mo:
            for k, v, rest in re.findall(r'arg\("(\w+)",\s*"([^"]*)"\)(.*)', mo.group(1)):
                if "default" not in rest:
                    opts["cream." + k] = v
        exit_ok = re.search(r"^## Output:ExitCode\n+```kt\nOK\n", text, re.M) is not None
        compiler = re.search(r"^## Compiler output\n+```text\n(.*?)^```", text, re.M | re.S)
        msg = ""
        if compiler or not exit_ok:
            body = compiler.group(1) if compiler else ""
            m = re.search(r"e: \[ksp\] [^\n]*?:\d+: ([^\n]+)", body) or re.search(r"e: \[ksp\] ([^\n]+)", body)
            msg = m.group(1)[:200] if m else ""
            expect = "error"
        else:
            expect = "ok"
        files = []
        for i, code in enumerate(inputs):
            pkg = re.search(r"^package\s+([\w.]+)", code, re.M)
            name = re.search(r"\b(?:class|interface|object)\s+(\w+)", code)
            fname = (name.group(1) if name else f"Input{i}") + ".kt"
            files.append(SrcFile(fname if len(inputs) == 1 else f"{i:02d}_{fname}", code))
        key_body = "\n".join(inputs)
        h = norm_hash(key_body)
        if h in seen:
            p = seen[h]
            if opts and opts not in p.options and len(p.options) < 8:
                p.options.append(opts)
            if family and f"cream.family={family}" not in p.tags:
                p.tags.append(f"cream.family={family}")
            continue
        feature = re.sub(r"(Snapshot|InvalidUsage|Diagnostic|Diagnostics)Test$", "", test)
        title = f"{feature} / {family or 'misc'} / {md.stem}"
        key = f"test:{src['repo']}:{h}"
        p = Pattern(
            id=ids.get("cream", "test", key), title=title[:140], processors=["cream"], expect=expect,
            expect_message=msg, options=[opts] if opts else [],
            tags=[f"cream.family={family}"] if family else [],
            origin=f"test:{src['repo']}@{src['commit'][:12]}/{rel}", license=src["license"],
            status="candidate", files=files,
        )
        seen[h] = p
    # test モジュール（KMP の commonMain）。源 ② の代わり
    real: list[Pattern] = []
    tmod = checkout / "test/src/commonMain/kotlin"
    for kt in sorted(tmod.rglob("*.kt")):
        rel = kt.relative_to(checkout).as_posix()
        code = kt.read_text(encoding="utf-8")
        if not re.search(r"@(CopyTo|CopyFrom|CopyToChildren|CombineTo|CombineFrom|CopyMapping|CombineMapping|"
                         r"SealedCopy|ParentOptional|ChildOptionals)\b", code):
            continue
        key = f"real:{src['repo']}:{rel}"
        real.append(Pattern(
            id=ids.get("cream", "real", key), title=f"cream.kt の test モジュール / {kt.stem}", processors=["cream"],
            expect="ok", origin=f"real:{src['repo']}@{src['commit'][:12]}/{rel}", license=src["license"],
            status="candidate", files=[SrcFile(f"commonMain/{kt.relative_to(tmod).as_posix()}", code)],
            note="cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。"
                 "同じモジュールの他ファイルに依存する場合がある",
        ))
    pats = list(seen.values())
    keep, prefix = write_patterns("cream", "test", pats)
    prune("cream", prefix, keep, f"test:{src['repo']}")
    keep, prefix = write_patterns("cream", "real", real)
    prune("cream", prefix, keep, f"real:{src['repo']}")
    ids.save()
    print(f"cream: ① {len(pats)} パターン、② {len(real)} パターン（test モジュール）")


# ============================================================ ① Moshi

def kotlin_string_blocks(body: str):
    """kotlin("name", \"\"\"...\"\"\") / java(...) を順に取り出す。"""
    for m in re.finditer(r'\b(kotlin|java)\(\s*"([^"]+)",\s*"""(.*?)"""', body, re.S):
        yield m.group(1), m.group(2), dedent(m.group(3))


def dedent(s: str) -> str:
    lines = s.strip("\n").splitlines()
    ind = min((len(l) - len(l.lstrip()) for l in lines if l.strip()), default=0)
    return "\n".join(l[ind:] for l in lines) + "\n"


def split_test_functions(text: str):
    """@Test の付いた関数を名前と本文に分ける（次の @Test か private fun まで）。"""
    starts = [m for m in re.finditer(r"@Test\s+fun\s+(`[^`]+`|\w+)\s*\(", text)]
    for i, m in enumerate(starts):
        end = starts[i + 1].start() if i + 1 < len(starts) else len(text)
        tail = re.search(r"\n  private fun ", text[m.end():end])
        if tail:
            end = m.end() + tail.start()
        yield m.group(1).strip("`"), text[m.start():end]


DECL_RE = re.compile(
    r"^(?P<ind>[ \t]*)(?P<ann>(?:@[\w.:]+(?:\([^\n]*\))?\s*\n?[ \t]*)*)"
    r"(?P<mods>(?:(?:public|internal|private|protected|data|sealed|abstract|open|inner|value|enum|annotation|"
    r"fun|expect|actual)\s+)*)(?P<kind>class|interface|object|typealias)\s+(?P<name>\w+)", re.M)


def find_block_end(text: str, start: int) -> int:
    """宣言の終わり（波括弧・丸括弧の対応が取れた行末）を返す。文字列リテラルは大まかに飛ばす。"""
    depth = 0
    i = start
    opened = False
    n = len(text)
    while i < n:
        c = text[i]
        if text.startswith('"""', i):
            j = text.find('"""', i + 3)
            i = n if j < 0 else j + 3
            continue
        if c == '"':
            j = i + 1
            while j < n and text[j] != '"' and text[j] != "\n":
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
            continue
        if c in "({[<" and c != "<":
            depth += 1
            opened = True
        elif c in ")}]":
            depth -= 1
        elif c == "\n" and depth == 0:
            # 宣言が 1 行で終わる、または本体が閉じた
            nxt = text[i + 1:i + 200].lstrip(" \t")
            if opened or not nxt.startswith(("{", ":", "(", ".")):
                if not text[start:i].rstrip().endswith((",", ":", "=")):
                    return i
        i += 1
    return n


def extract_declarations(text: str):
    """ファイル中の宣言（入れ子も含む）を {名前: (本文, 字下げ, アノテーション)} にする。"""
    decls = {}
    for m in DECL_RE.finditer(text):
        name = m.group("name")
        end = find_block_end(text, m.start("mods") if m.group("mods") else m.start("kind"))
        body = text[m.start():end]
        if name not in decls:
            decls[name] = (body, m.group("ind"), m.group("ann") or "")
    return decls


def closure(name: str, decls: dict) -> list[str]:
    order, todo = [], [name]
    while todo:
        n = todo.pop()
        if n in order or n not in decls:
            continue
        order.append(n)
        body = decls[n][0]
        for other in decls:
            if other not in order and re.search(rf"\b{re.escape(other)}\b", body):
                todo.append(other)
    return order


def reindent(body: str, ind: str) -> str:
    out = []
    for l in body.splitlines():
        out.append(l[len(ind):] if l.startswith(ind) else l.lstrip() if not l.strip() else l)
    return "\n".join(out)


def import_moshi(checkout: Path):
    src = upstream("moshi")
    check_checkout(checkout, src["commit"])
    copy_license(src["repo"], checkout)
    ids = IdMap()
    pats: list[Pattern] = []
    base = f"test:{src['repo']}@{src['commit'][:12]}/"
    # (1) processor のコンパイルテスト
    ptest = checkout / "moshi-kotlin-codegen/src/test/java/com/squareup/moshi/kotlin/codegen/ksp/JsonClassSymbolProcessorTest.kt"
    rel = ptest.relative_to(checkout).as_posix()
    text = ptest.read_text(encoding="utf-8")
    for name, body in split_test_functions(text):
        blocks = list(kotlin_string_blocks(body))
        if not blocks:
            continue
        expect = "error" if "COMPILATION_ERROR" in body else "ok"
        msgs = re.findall(r'\.contains\(\s*"((?:[^"\\]|\\.)*)"', body)
        opts = {}
        for k, v in re.findall(r"(?:kspProcessorOptions|kspArgs)\[(\w+)\]\s*=\s*\"([^\"]*)\"", body):
            opts[{"OPTION_GENERATED": "moshi.generated", "OPTION_GENERATE_PROGUARD_RULES": "moshi.generateProguardRules",
                  "OPTION_INSTANTIATE_ANNOTATIONS": "moshi.instantiateAnnotations"}.get(k, k)] = v
        tags = []
        if re.search(r"NotSupported|Fails$|multipleErrors|privateClasses|localClasses", name):
            tags.append("moshi.shape=unsupported-kind")
        if name.startswith("private") or "nonPropertyConstructorParameter" in name:
            tags.append("moshi.shape=private-api")
        key = f"test:{src['repo']}:{rel}#{name}"
        pats.append(Pattern(
            id=ids.get("moshi", "test", key), title=f"processor テスト / {name}", processors=["moshi"],
            expect=expect, expect_message=msgs[0] if (msgs and expect == "error") else "",
            options=[opts] if opts else [], tags=tags, origin=f"{base}{rel}#{name}", license=src["license"],
            status="candidate", files=[SrcFile(fn, code) for _, fn, code in blocks],
        ))
    # (2) 実行時テストのクラス群: @JsonClass の付いた宣言ごとに、同じファイル内で参照している宣言を連れてくる
    rt = next(d for d in (checkout / "moshi-kotlin-tests", checkout / "kotlin/tests") if d.exists())
    runtime_files = sorted(rt.rglob("*.kt"))
    for kt in runtime_files:
        text = kt.read_text(encoding="utf-8")
        if "@JsonClass" not in text or "/build/" in kt.as_posix():
            continue
        rel = kt.relative_to(checkout).as_posix()
        imports = "\n".join(l for l in text.splitlines() if l.startswith("import "))
        pkg = (re.search(r"^package\s+([\w.]+)", text, re.M) or [None, "test"])[1]
        decls = extract_declarations(text)
        test_class = {n for n, (b, ind, ann) in decls.items() if ind == "" and n.endswith("Test")}
        for n, (body, ind, ann) in decls.items():
            if "@JsonClass" not in ann or n in test_class:
                continue
            names = [x for x in closure(n, decls) if x not in test_class]
            code_parts = []
            for x in reversed(names):
                b, i2, _ = decls[x]
                # 入れ子のうち、別に取り出した宣言の中に含まれるものは二重に書かない
                if any(x != y and decls[y][0].find(b) >= 0 for y in names):
                    continue
                code_parts.append(reindent(b, i2))
            joined = "\n\n".join(code_parts)
            used = "\n".join(l for l in imports.splitlines()
                             if re.search(rf"\b{re.escape(l.split('.')[-1].split(' as ')[-1].strip())}\b", joined))
            code = f"package {pkg}\n\n{used}\n\n" + joined + "\n"
            tags = []
            if re.search(r"\(\s*[^)]*=\s*[^)]", body):
                tags.append("moshi.shape=default-values")
            if re.search(r"class\s+\w+\s*<", body):
                tags.append("moshi.shape=generic-class")
            if re.search(r"\)\s*:\s*\w", body):
                tags.append("moshi.shape=inheritance")
            if body.count("val ") + body.count("var ") >= 12:
                tags.append("moshi.shape=many-properties")
            if re.search(r"\w\?\s*[,)=]", body):
                tags.append("moshi.shape=nullable-props")
            if ind:
                tags.append("moshi.shape=nested-adapter")
            key = f"test:{src['repo']}:{rel}#{n}"
            pats.append(Pattern(
                id=ids.get("moshi", "test", key), title=f"実行時テスト / {kt.stem} / {n}", processors=["moshi"],
                expect="ok", tags=sorted(set(tags)), origin=f"{base}{rel}#{n}", license=src["license"],
                status="candidate", files=[SrcFile(f"{n}.kt", code)],
                note="テストクラスの入れ子から取り出したものは最上位に移している" if ind else "",
            ))
    keep, prefix = write_patterns("moshi", "test", pats)
    prune("moshi", prefix, keep, f"test:{src['repo']}")
    ids.save()
    print(f"moshi: {len(pats)} パターン")


# ============================================================ ① Koin

def module_sources(mod: Path) -> list[SrcFile]:
    files = []
    for f in sorted(mod.rglob("*")):
        if f.suffix not in (".kt", ".java") or "/build/" in f.as_posix():
            continue
        relp = f.relative_to(mod).as_posix()
        m = re.match(r"src/(\w+)/(?:kotlin|java)/(.+)$", relp)
        if not m or m.group(1).lower().endswith("test"):
            continue
        files.append(SrcFile(f"{m.group(1)}/{m.group(2)}", f.read_text(encoding="utf-8")))
    return files


def import_koin(checkout: Path):
    src = upstream("koin")
    check_checkout(checkout, src["commit"])
    copy_license(src["repo"], checkout)
    ids = IdMap()
    pats = []
    base = f"test:{src['repo']}@{src['commit'][:12]}/"
    ex = checkout / "examples"
    for mod in sorted(p for p in ex.iterdir() if (p / "build.gradle.kts").exists() or (p / "build.gradle").exists()):
        files = module_sources(mod)
        if not any("org.koin.core.annotation" in f.content for f in files):
            continue
        rel = mod.relative_to(checkout).as_posix()
        gradle = (mod / "build.gradle.kts").read_text(encoding="utf-8") if (mod / "build.gradle.kts").exists() else ""
        opts = {}
        for k, v in re.findall(r'arg\("(KOIN_\w+)",\s*"([^"]*)"\)', gradle):
            if v not in ("false",):
                opts[k] = v
        tags = []
        if "cycle" in mod.name:
            tags.append("koin.other=cycle")
        if "android" in mod.name:
            files = [SrcFile(f.path.replace("main/", "androidMain/", 1) if f.path.startswith("main/") else f.path, f.content)
                     for f in files]
        procs = ["koin"]
        if re.search(r"ksp\(.*(moshi|room|hilt|dagger)", gradle, re.I):
            procs.append(re.search(r"ksp\(.*(moshi|room|hilt|dagger)", gradle, re.I).group(1).lower())
        key = f"test:{src['repo']}:{rel}"
        pats.append(Pattern(
            id=ids.get("koin", "test", key), title=f"examples / {mod.name}", processors=procs, expect="ok",
            options=[opts] if opts else [], tags=tags, origin=f"{base}{rel}", license=src["license"],
            status="candidate", files=files,
            note="サンプルのモジュール丸ごと（テストソースは除く）" + ("。定義が 2,000 件以上ある大きな入力" if mod.name == "compile-perf" else ""),
        ))
    keep, prefix = write_patterns("koin", "test", pats)
    prune("koin", prefix, keep, f"test:{src['repo']}")
    ids.save()
    print(f"koin: {len(pats)} パターン")


# ============================================================ ② 実プロジェクト

MARKERS = {
    "moshi": re.compile(r"@JsonClass\b"),
    "koin": re.compile(r"^import org\.koin\.core\.annotation\.", re.M),
    "cream": re.compile(r"^import me\.tbsten\.cream\.", re.M),
}
OTHER_PROCS = {
    "room": re.compile(r"^import androidx\.room\.(Entity|Dao|Database)\b", re.M),
    "dagger": re.compile(r"^import dagger\.(hilt\.|Module\b|Component\b|Provides\b)", re.M),  # Hilt も dagger として数える
    "moshi": re.compile(r"@JsonClass\b"),
    "koin": re.compile(r"^import org\.koin\.core\.annotation\.", re.M),
}
REAL_LIMIT = 30


def import_real(proc: str, repo: str, checkout: Path):
    src = project(proc, repo)
    check_checkout(checkout, src["commit"])
    copy_license(repo, checkout)
    ids = IdMap()
    base = f"real:{repo}@{src['commit'][:12]}/"
    cands = []
    for f in sorted(checkout.rglob("*")):
        if f.suffix not in (".kt", ".java") or "/build/" in f.as_posix():
            continue
        text = f.read_text(encoding="utf-8", errors="replace")
        if not MARKERS[proc].search(text):
            continue
        rel = f.relative_to(checkout).as_posix()
        m = re.match(r"(?:(.+)/)?src/(\w+)/(?:kotlin|java)/(.+)$", rel)
        if not m:
            continue
        module = (m.group(1) or "root").replace("/", ":")
        procs = [proc] + sorted(k for k, r in OTHER_PROCS.items() if k != proc and r.search(text))
        opts = {}
        gradle = checkout / (m.group(1) or "") / "build.gradle.kts"
        if gradle.exists():
            for k, v in re.findall(r'arg\(\s*"((?:KOIN_|moshi\.|cream\.)[\w.]+)"\s*,\s*"([^"]*)"\s*\)',
                                   gradle.read_text(encoding="utf-8")):
                opts[k] = v
        p = Pattern(id="", title=f"{repo} / {module} / {Path(rel).stem}", processors=procs, expect="ok",
                    options=[opts] if opts else [],
                    origin=f"{base}{rel}", license=src["license"], status="candidate",
                    files=[SrcFile(f"{module.split(':')[-1]}/{m.group(2)}/{m.group(3)}", text)],
                    note="ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので、K で確かめるまで candidate")
        cands.append((rel, p))
    # 網羅に効く順に REAL_LIMIT 件まで選ぶ（貪欲法。新しいタグの数が同じなら短いファイルを優先）
    chosen, covered = [], set()
    pool = [(rel, p, all_tags(p)) for rel, p in cands]
    while pool and len(chosen) < REAL_LIMIT:
        pool.sort(key=lambda t: (-len(t[2] - covered), sum(len(f.content) for f in t[1].files), t[0]))
        rel, p, tg = pool.pop(0)
        if chosen and not (tg - covered) and len(chosen) >= 10:
            break
        covered |= tg
        p.id = ids.get(proc, "real", f"real:{repo}:{rel}")
        chosen.append(p)
    keep, prefix = write_patterns(proc, "real", chosen)
    prune(proc, prefix, keep, f"real:{repo}@")
    ids.save()
    print(f"{proc} / {repo}: 候補 {len(cands)} ファイルから {len(chosen)} パターン")


# ============================================================ まとめて取り込む

def fetch(repo: str, commit: str, cache: Path) -> Path:
    import subprocess
    d = cache / repo.replace("/", "_")
    if not (d / ".git").exists():
        d.mkdir(parents=True, exist_ok=True)
        subprocess.run(["git", "init", "-q", str(d)], check=True)
        subprocess.run(["git", "-C", str(d), "remote", "add", "origin", f"https://github.com/{repo}.git"], check=True)
    head = subprocess.run(["git", "-C", str(d), "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip()
    if head != commit:
        subprocess.run(["git", "-C", str(d), "fetch", "-q", "--depth", "1", "origin", commit], check=True)
        subprocess.run(["git", "-C", str(d), "checkout", "-q", "FETCH_HEAD"], check=True)
    return d


def import_all(cache: Path):
    importers = {"cream": import_cream, "moshi": import_moshi, "koin": import_koin}
    for proc, fn in importers.items():
        u = upstream(proc)
        fn(fetch(u["repo"], u["commit"], cache))
    for e in registry().get("project", []):
        for proc, pid in REGISTRY_ID.items():
            if pid in e["uses"]:
                import_real(proc, e["repo"], fetch(e["repo"], e["commit"], cache))


# ============================================================ 索引と網羅表

def build(write: bool = True):
    pats = load_patterns()
    dims = compiled_dims()
    errors: list[str] = []
    seen = {}
    valid = {f"{d}={v['id']}" for d, dd in dims.items() for v in dd["values"]}
    index = []
    cov: dict[str, Counter] = defaultdict(Counter)
    cov_ids: dict[str, list[str]] = defaultdict(list)
    cov_verified: Counter = Counter()  # verified だけで数えた網羅（M1 以降の合格条件。ADR-020 の 6 項）
    for p in pats:
        if p.id in seen:
            errors.append(f"ID が重複しています: {p.id}（{seen[p.id]} と {p.path}）")
        seen[p.id] = p.path
        if p.path and p.path.stem != p.id:
            errors.append(f"{p.path}: ファイル名と PATTERN が違います")
        if not re.match(r"^(CMN|CRM|MSH|KOI)-[TRO]-\d{4}$", p.id):
            errors.append(f"{p.path}: ID の形が違います: {p.id}")
        for pr in p.processors:
            if pr not in KNOWN_PROCESSORS:
                errors.append(f"{p.id}: 未知の Processor 名です: {pr}（{', '.join(sorted(KNOWN_PROCESSORS))}）")
        if p.expect not in ("ok", "error", "warning", "compile-error"):
            errors.append(f"{p.id}: EXPECT が不正です: {p.expect}")
        if p.status not in ("candidate", "verified", "rejected"):
            errors.append(f"{p.id}: STATUS が不正です: {p.status}")
        if p.status == "rejected" and not p.note:
            errors.append(f"{p.id}: rejected には NOTE で理由が要ります")
        if p.source != "own" and not p.license:
            errors.append(f"{p.id}: 取り込んだパターンには LICENSE が要ります")
        for t in p.tags:
            if t not in valid:
                errors.append(f"{p.id}: 未定義のタグです: {t}")
        tags = sorted(all_tags(p) & valid)
        for t in (tags if p.status == "verified" else []):
            cov_verified[t] += 1
        for t in (tags if p.status != "rejected" else []):
            cov[t][p.source] += 1
            if len(cov_ids[t]) < 3:
                cov_ids[t].append(p.id)
        index.append({
            "id": p.id, "title": p.title, "owner": p.owner, "source": p.source,
            "processors": p.processors, "expect": p.expect, "expectMessage": p.expect_message,
            "options": p.options, "tags": tags, "status": p.status, "origin": p.origin,
            "shard": zlib.crc32(p.id.encode()) % SHARDS,
            "path": p.path.relative_to(ROOT).as_posix() if p.path else "",
            "files": [f.path for f in p.files],
        })
    gaps = []
    for dname, d in dims.items():
        for v in d["values"]:
            t = f"{dname}={v['id']}"
            if v.get("required") and sum(cov[t].values()) == 0:
                gaps.append(t)
    if write:
        (ROOT / "index.json").write_text(
            json.dumps({"schema": 1, "shards": SHARDS, "patterns": index}, ensure_ascii=False, indent=1) + "\n",
            encoding="utf-8")
        (ROOT / "coverage.md").write_text(render_coverage(pats, dims, cov, cov_ids, gaps, cov_verified), encoding="utf-8")
        (ROOT / "THIRD_PARTY.md").write_text(render_third_party(pats), encoding="utf-8")
    return pats, errors, gaps


def render_third_party(pats) -> str:
    rows = Counter()
    lic = {}
    for p in pats:
        if p.source == "own":
            continue
        m = re.match(r"(?:test|real):([\w.-]+/[\w.-]+)@([0-9a-f]+)/", p.origin)
        if m:
            rows[(m.group(1), m.group(2))] += 1
            lic[m.group(1)] = p.license
    L = ["# 入力パターンカタログに含む第三者のコード", "",
         "自動生成（`catalog.py index`）。`patterns/` の取り込みパターン（`*-T-*`・`*-R-*`）は、次のリポジトリのテスト入力・ソースの抜き出しを含む。",
         "各ファイルの先頭の `ORIGIN` に元のパスとコミット、`LICENSE` にライセンスを書いている。元のファイルにあった著作権表示はそのまま残している。"
         "ライセンス文は [`licenses/`](licenses/) にある。", "",
         "| リポジトリ | コミット | ライセンス | パターン数 | ライセンス文 |", "|---|---|---|---:|---|"]
    for (repo, commit), n in sorted(rows.items()):
        f = repo.replace("/", "__") + ".txt"
        L.append(f"| [{repo}](https://github.com/{repo}) | `{commit}` | {lic[repo]} | {n} | [{f}](licenses/{f}) |")
    return "\n".join(L) + "\n"


def render_coverage(pats, dims, cov, cov_ids, gaps, cov_verified) -> str:
    by = Counter((p.owner, p.source) for p in pats)
    owners = ["common", "cream", "moshi", "koin"]
    L = ["---", "title: 入力パターンカタログの網羅表", "project: analysisCodeGen",
         "status: 自動生成（quality/catalog/scripts/catalog.py index）。手で編集しない", "---", "",
         "# 入力パターンカタログの網羅表", "",
         "> 判断したいこと: 利用者が書きうる入力の空間を、カタログがどこまで覆っているか。穴はどこか。", "",
         "次元と値の定義は [dimensions.toml](dimensions.toml)、決め方は ADR-020。"
         "各セルは、その値を含むパターンの数（① テスト / ② 実プロジェクト / ③ 自作）。", "",
         "## 1. 出所ごとの件数", "",
         "| 対象 | ① テスト | ② 実プロジェクト | ③ 自作 | 計 | K で確認済み |", "|---|---:|---:|---:|---:|---:|"]
    for o in owners:
        n = [by[(o, s)] for s in ("test", "real", "own")]
        ver = sum(1 for p in pats if p.owner == o and p.status == "verified")
        L.append(f"| {o} | {n[0]} | {n[1]} | {n[2]} | {sum(n)} | {ver} |")
    tot = [sum(by[(o, s)] for o in owners) for s in ("test", "real", "own")]
    L.append(f"| **計** | {tot[0]} | {tot[1]} | {tot[2]} | {sum(tot)} | {sum(1 for p in pats if p.status == 'verified')} |")
    L += ["", "「K で確認済み」は、KSP2 でビルドして EXPECT どおりだったパターン（`:e2e-tests:verifyCatalog` と `catalog.py apply-k` が付ける。ADR-072）。", ""]
    req = [f"{d}={v['id']}" for d, dd in dims.items() for v in dd["values"] if v.get("required")]
    L += [f"## 2. 必須の値の網羅: {len(req) - len(gaps)} / {len(req)}", ""]
    if gaps:
        L += ["次の値はまだパターンがありません（③ で埋める）。", ""] + [f"- `{g}`" for g in gaps] + [""]
    else:
        L += ["必須の値はすべて 1 件以上のパターンで覆われています。", ""]
    weak = [f"{d}={v['id']}" for d, dd in dims.items() for v in dd["values"]
            if v.get("required") and 0 < sum(cov[f"{d}={v['id']}"].values()) < 3]
    if weak:
        L += ["1〜2 件しかない必須の値（薄いところ）: " + "、".join(f"`{w}`" for w in weak), ""]
    vgaps = [r for r in req if cov_verified[r] == 0]
    L += [f"K で確認済み（verified）だけで数えると: {len(req) - len(vgaps)} / {len(req)}（M1 以降の合格条件。ADR-020 の 6 項、ADR-072）", ""]
    if vgaps:
        L += ["verified のパターンがまだ無い必須の値: " + "、".join(f"`{g}`" for g in vgaps), ""]
    L += ["## 3. 次元ごとの網羅", ""]
    for dname, d in dims.items():
        L += [f"### {d['title']}（`{dname}`、対象: {d['scope']}）", "",
              "| 値 | 必須 | ① | ② | ③ | 計 | 例 |", "|---|:-:|---:|---:|---:|---:|---|"]
        for v in d["values"]:
            t = f"{dname}={v['id']}"
            c = cov[t]
            total = sum(c.values())
            mark = "✓" if v.get("required") else ""
            flag = " ⚠️" if v.get("required") and total == 0 else ""
            L.append(f"| `{v['id']}`{flag} | {mark} | {c['test']} | {c['real']} | {c['own']} | {total} | "
                     f"{' '.join(cov_ids[t])} |")
        L.append("")
    return "\n".join(L)


# ============================================================ main

def main(argv):
    if not argv:
        print(__doc__)
        return 2
    cmd = argv[0]
    if cmd == "index":
        pats, errors, gaps = build(True)
        print(f"{len(pats)} パターン、必須の穴 {len(gaps)} 件、エラー {len(errors)} 件")
        for e in errors:
            print("  " + e)
        return 0
    if cmd == "check":
        before = {n: (ROOT / n).read_text(encoding="utf-8") if (ROOT / n).exists() else ""
                  for n in ("index.json", "coverage.md")}
        pats, errors, gaps = build(True)
        for n, old in before.items():
            if (ROOT / n).read_text(encoding="utf-8") != old:
                errors.append(f"{n} が古いままです。`catalog.py index` を実行してコミットしてください")
        for g in gaps:
            errors.append(f"必須の値にパターンがありません: {g}")
        for e in errors:
            print("NG: " + e)
        print(f"{len(pats)} パターン、エラー {len(errors)} 件")
        return 1 if errors else 0
    if cmd == "import-cream":
        import_cream(Path(argv[1]))
    elif cmd == "import-moshi":
        import_moshi(Path(argv[1]))
    elif cmd == "import-koin":
        import_koin(Path(argv[1]))
    elif cmd == "import-all":
        import_all(Path(argv[1]))
    elif cmd == "import-real":
        import_real(argv[1], argv[2], Path(argv[3]))
    elif cmd == "apply-k":
        apply_k(Path(argv[1]))
        pats, errors, gaps = build(True)
        print(f"{len(pats)} パターン、必須の穴 {len(gaps)} 件、エラー {len(errors)} 件")
        for e in errors + [f"必須の値にパターンがありません: {g}" for g in gaps]:
            print("  " + e)
        return 1 if errors or gaps else 0
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
