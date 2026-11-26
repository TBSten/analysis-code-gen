#!/usr/bin/env python3
"""KSP 公式テスト（KSPT）を取り込む（ADR-024、ACG-43）。

google/ksp の固定コミット（porting/ksp/UPSTREAM.properties）から次を写し、出所を記録する。

- kotlin-analysis-api/testData（テスト入力と期待値）
    -> ksp-compatibility-tests/src/test/resources/kspt/testData/
- テスト用 Processor（kotlin-analysis-api/src/test/.../processor と test-utils/src/main/.../processor）
    -> ksp-compatibility-tests/src/kspt/kotlin/com/google/devtools/ksp/processor/
- porting/ksp/port-map.csv に写したファイルの行を足す（status=ported）
- テストの一覧（ksp-compatibility-tests/src/test/resources/kspt/cases.csv）と、
  テストケース一覧（quality/test-case-inventory.md）の KSPT の表を作り直す

使い方
  git clone --filter=blob:none https://github.com/google/ksp.git /tmp/ksp
  git -C /tmp/ksp checkout <UPSTREAM.properties の commit>
  python3 porting/ksp/import_kspt.py --upstream /tmp/ksp
  python3 quality/scripts/gen_csv.py

決まり（ADR-024）
- testData は 1 文字も変えず、出所の行（ヘッダが無いものは定型のヘッダも）を**末尾**に足す。
  期待値に行番号・位置が出るテストがあるため、行をずらさない
- Processor はヘッダの直後に出所の 3 行を足す。期待値に FQN が出るため、パッケージは
  com.google.devtools.ksp.processor のまま（ADR-018 のパッケージ変更の例外）
- M の割り当てと除外は下の RULES / EXCLUDED で決める。手で cases.csv を直さない
"""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PORT_MAP = ROOT / "porting/ksp/port-map.csv"
MODULE = "ksp-compatibility-tests"
TESTDATA_DST = ROOT / MODULE / "src/test/resources/kspt/testData"
PROCESSOR_DST = ROOT / MODULE / "src/kspt/kotlin/com/google/devtools/ksp/processor"
CASES_CSV = ROOT / MODULE / "src/test/resources/kspt/cases.csv"
INVENTORY = ROOT / "quality/test-case-inventory.md"

SUITE = "kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/KSPUnitTestSuite.kt"
AA_CONFIGURED = "kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/test/AAConfiguredUnitTestSuite.kt"
TESTDATA_SRC = "kotlin-analysis-api/testData"
PROCESSOR_SRCS = (
    "kotlin-analysis-api/src/test/kotlin/com/google/devtools/ksp/processor",
    "test-utils/src/main/kotlin/com/google/devtools/ksp/processor",
)

MODIFIED_BY = "analysis-code-gen contributors, 2026"
NO_HEADER = """/*
 * Copyright Google LLC and the KSP authors
 * （上流のファイルにヘッダが無いため、google/ksp の LICENSE に基づき追記）
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */"""

# 取り込まないテスト（KSPUnitTestSuite のメソッド名 -> 理由）。一覧の 6.3 に載せる
EXCLUDED = {
    "testPlatformDeclaration": "KSP2 自身で @Disabled（通らない）",
    "testDeferredTypeRefs": "KSP2 自身で @Disabled（通らない）",
    "testEnumModifiers": "KSP2 自身が通らない（既知のバグ google/ksp#2271。例外を期待する形で固定）",
    "testFakeOverrideProperties": "KSP2 自身が通らない（既知のバグ google/ksp#2420。不一致を期待する形で固定）",
    "testParentOfJavaMethodOverridingKotlinProperty": "KSP2 自身が通らない（既知のバグ google/ksp#2498。不一致を期待する形で固定）",
    "testAllUseSiteTargetAppliedToAnnotationList": "KSP2（AA 構成）自身が通らない（既知のバグ google/ksp#2912）",
    "testFunctionKindsJavaInheritsKotlin": "KSP2（AA 構成）自身が通らない（既知のバグ google/ksp#2496。例外を期待する形で固定）",
    "testExpectDifferentOutput": "テスト基盤（期待値の書き分け）自体の自己テスト。KSP の挙動を見ていない",
    "testExpectDifferentOutputFailingOnCurrent": "テスト基盤の自己テスト（構成ごとにわざと失敗させる）",
    "testExpectDifferentOutputFailingOnBackingFields": "テスト基盤の自己テスト（構成ごとにわざと失敗させる）",
    "testExpectDifferentOutputFailingOnContextParameters": "テスト基盤の自己テスト（構成ごとにわざと失敗させる）",
    "testExpectDifferentOutputFailingOnNext": "テスト基盤の自己テスト（構成ごとにわざと失敗させる）",
}

# 一覧 6 章の区分。上から順に最初に当てはまったものを使う（name はメソッド名から test を除いたもの）
M3_JVM = re.compile(r"SignatureMapper|MangledNames|JvmName|JavaToKotlinMapper|JvmModifiersCompanionObject|JvmStaticAnnotation")
M3_FEATURES = re.compile(r"BackingField|ContextParameter|ChangingBehavior|RegisterProcessorForNewFeatures")
M3_INTERNAL = re.compile(r"^PluginProblemReporter$")
M2_INCREMENTAL = re.compile(r"^Record")
M2_ROUNDS = re.compile(r"^(Multipleround|Deferred\w*|ExitCode|PackageProviderForGenerated)$")
# G0 項目 2（ADR-056）: Java のファイルを含んでいても KSP の入口として M1 に置く基本のテスト（レビューで名指しされたもの）
M1_BASIC = re.compile(r"^(Hello|ClassKinds|Visibilities)$")
# 名前に Java を含むテストは Java 連携が主題なので、Java のファイルが無くても M2
M2_JAVA_NAMED = re.compile(r"Java")


def classify(name: str, test_data: str, text: str, annotations: list[str]) -> tuple[str, str]:
    if test_data.startswith("native/") or "TARGET_BACKEND: NATIVE" in text:
        return "M3", "Kotlin/Native"
    if M3_FEATURES.search(name):
        return "M3", "試験的な API 機能（backing field・context parameter）"
    if M3_JVM.search(name):
        return "M3", "JVM シグネチャ・名前の細部"
    if any(a.startswith("@Negative") for a in annotations):
        return "M3", "エッジケース（不正な入力）"
    if M3_INTERNAL.search(name):
        return "M3", "エッジケース（Processor の例外の報告）"
    if M1_BASIC.search(name):
        return "M1", "シンボル・アノテーション・型の基本（Java の宣言を含む）"
    if M2_INCREMENTAL.search(name):
        return "M2", "増分（lookup の記録）"
    if M2_ROUNDS.search(name):
        return "M2", "マルチラウンド・deferred"
    if re.search(r"^//\s*FILE:\s*\S+\.java\s*$", text, re.M):
        return "M2", "Java ソース"
    if re.search(r"^//\s*MODULE:", text, re.M):
        return "M2", "ライブラリ（バイナリの宣言）"
    if M2_JAVA_NAMED.search(name):
        return "M2", "Java 連携"
    return "M1", "シンボル・アノテーション・型の基本"


# Processor を写すときの置き換え（上流ファイル名 -> [(old, new)], Modifications に書く文）
PATCHES = {
    "PluginProblemReporterProcessor.kt": (
        [("import com.intellij.diagnostic.PluginException", "import ksp.com.intellij.diagnostic.PluginException")],
        "symbol-processing-aa-embeddable が IntelliJ を ksp. 接頭辞へ移しているため import を変更",
    ),
    "FakeOverrideProcessor.kt": (
        [("import com.intellij.util.containers.addIfNotNull\n", "")],
        "IntelliJ の addIfNotNull（embeddable では Kotlin から見えない）の import を外し、同名の private 関数をファイル末尾に足した",
    ),
}
PATCH_APPENDIX = {
    "FakeOverrideProcessor.kt": "\nprivate fun <T : Any> MutableCollection<T>.addIfNotNull(element: T?) {\n    if (element != null) add(element)\n}\n",
}


def sh(*args: str, cwd: Path | None = None) -> str:
    return subprocess.run(args, cwd=cwd, check=True, capture_output=True, text=True).stdout


def read_props() -> dict[str, str]:
    props = {}
    for line in (ROOT / "porting/ksp/UPSTREAM.properties").read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    return props


def parse_suite(path: Path) -> list[dict]:
    """KSPUnitTestSuite のテストメソッド（名前・注釈・呼び出し・testData のパス）を読む。"""
    lines = path.read_text().split("\n")
    tests, ann, i = [], [], 0
    while i < len(lines):
        s = lines[i].strip()
        if s.startswith("@"):
            a = s
            while a.count("(") > a.count(")"):
                i += 1
                a += " " + lines[i].strip()
            ann.append(a)
        else:
            m = re.match(r"(abstract )?fun (test\w+)\(", s)
            if m:
                body = []
                if not m.group(1):
                    j, depth = i + 1, 1
                    while depth > 0:
                        body.append(lines[j])
                        depth += lines[j].count("{") - lines[j].count("}")
                        j += 1
                meta = next((re.search(r'"(.+)"', a).group(1) for a in ann if a.startswith("@TestMetadata")), None)
                calls = re.findall(r'(runKSPTest|runFailingKSPTest|runThrowingKSPTest)\("([^"]+)"', "\n".join(body))
                tests.append(dict(method=m.group(2), abstract=bool(m.group(1)), ann=ann, calls=calls, meta=meta))
                ann = []
            elif s and not s.startswith(("//", "*", "/*")):
                ann = []
        i += 1
    return tests


def resolve_calls(tests: list[dict], aa_configured: Path) -> None:
    """abstract のテストは AA 構成（AAConfiguredUnitTestSuiteBase）の override の呼び出しを使う。"""
    overrides = {t["method"]: t for t in parse_suite_overrides(aa_configured)}
    for t in tests:
        if t["abstract"] and t["method"] in overrides:
            t["calls"] = overrides[t["method"]]["calls"]


def parse_suite_overrides(path: Path) -> list[dict]:
    text = path.read_text().replace("override fun", "fun")
    tmp = path.with_suffix(".tmp")
    try:
        tmp.write_text(text)
        return parse_suite(tmp)
    finally:
        tmp.unlink()


def provenance(sha: str, upstream_path: str, modifications: str) -> str:
    return (
        f"// Ported-From: google/ksp@{sha}:{upstream_path}\n"
        f"// Modified-By: {MODIFIED_BY}\n"
        f"// Modifications: {modifications}\n"
    )


def copy_test_data(up: Path, sha: str, rel: str) -> tuple[str, int, str]:
    src = up / TESTDATA_SRC / rel
    text = src.read_text()
    has_header = "Copyright" in text
    tail = "" if text.endswith("\n") else "\n"
    if not has_header:
        tail += NO_HEADER + "\n"
    tail += provenance(
        sha,
        f"{TESTDATA_SRC}/{rel}",
        "なし（行番号を変えないよう、出所の行を末尾に追記しただけ）" if has_header
        else "なし（上流にヘッダが無いため、行番号を変えないよう定型のヘッダと出所の行を末尾に追記しただけ）",
    )
    dst = TESTDATA_DST / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(text + tail)
    return f"{TESTDATA_SRC}/{rel}", src.stat().st_size, copyright_of(text)


def copyright_of(text: str) -> str:
    head = "\n".join(text.splitlines()[:5])
    owners = []
    if "Google LLC" in head:
        owners.append("Google LLC")
    if "JetBrains" in head:
        owners.append("JetBrains")
    return ";".join(owners) or "none"


def declared_names(text: str) -> set[str]:
    return set(re.findall(r"^(?:(?:private|internal|public|open|abstract|sealed|data|enum|annotation)\s+)*"
                          r"(?:class|object|interface|fun|val)\s+(?:<[^>]*>\s*)?(?:\w+\.)?(\w+)", text, re.M))


def processor_closure(up: Path, roots: set[str]) -> dict[str, Path]:
    """roots（Processor のクラス名）を含むファイルと、それが参照する同じパッケージのファイルを集める。"""
    files: dict[str, Path] = {}
    for d in PROCESSOR_SRCS:
        for f in sorted((up / d).glob("*.kt")):
            if f.name in files:
                raise SystemExit(f"同名のファイルが 2 つある: {f.name}")
            files[f.name] = f
    names = {n: declared_names(f.read_text()) for n, f in files.items()}
    picked: dict[str, Path] = {}
    queue = [n for n, ds in names.items() if ds & roots]
    missing = roots - set().union(*[names[n] for n in queue])
    if missing:
        raise SystemExit(f"Processor が見つからない: {sorted(missing)}")
    while queue:
        n = queue.pop()
        if n in picked:
            continue
        picked[n] = files[n]
        idents = set(re.findall(r"\b[A-Za-z_]\w*\b", files[n].read_text()))
        for other, ds in names.items():
            if other not in picked and ds & idents:
                queue.append(other)
    return picked


def copy_processor(sha: str, up: Path, src: Path) -> tuple[str, int, str, str]:
    rel = src.relative_to(up).as_posix()
    text = src.read_text()
    mods, strategy = "なし（そのまま）", "as-is"
    if src.name in PATCHES:
        repl, mods = PATCHES[src.name]
        for old, new in repl:
            if old not in text:
                raise SystemExit(f"{rel}: 置き換え元が無い: {old!r}")
            text = text.replace(old, new)
        text += PATCH_APPENDIX.get(src.name, "")
        strategy = "adapt"
    prov = provenance(sha, rel, mods)
    m = re.match(r"(/\*.*?\*/\n)", text, re.S)
    if m and "Copyright" in m.group(1):
        out = m.group(1) + prov + text[m.end():]
    else:
        out = NO_HEADER + "\n" + prov + text
    dst = PROCESSOR_DST / src.name
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(out)
    return rel, src.stat().st_size, copyright_of(src.read_text()), strategy


def update_port_map(rows_new: list[dict]) -> None:
    with PORT_MAP.open(newline="") as f:
        reader = csv.DictReader(f)
        cols = reader.fieldnames
        rows = [r for r in reader if not r["target_paths"].startswith(f"{MODULE}/")
                and not (r["target_module"] == MODULE and r["phase"] == "P1")]
    rows += sorted(rows_new, key=lambda r: r["upstream_path"])
    with PORT_MAP.open("w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=cols, lineterminator="\n")
        w.writeheader()
        w.writerows(rows)


def case_id(method: str) -> str:
    return "KSPT-" + method.removeprefix("test")


def write_cases(cases: list[dict]) -> None:
    CASES_CSV.parent.mkdir(parents=True, exist_ok=True)
    with CASES_CSV.open("w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["id", "method", "test_data", "milestone", "category", "mode"], lineterminator="\n")
        w.writeheader()
        w.writerows({k: c[k] for k in w.fieldnames} for c in cases)


def write_inventory(cases: list[dict], excluded: list[dict], tag: str, sha: str) -> None:
    lines = [
        f"取り込み元: google/ksp {tag}（`{sha[:12]}`）の `{SUITE}`（AA 構成）。"
        f"取り込み {len(cases)} 件・除外 {len(excluded)} 件。この節は `porting/ksp/import_kspt.py` が書く（手で直さない）。",
        "",
        "### 6.1 取り込んだテスト（KSPT）",
        "",
        "| ID | ケース | 種別 | M | 備考 |",
        "|---|---|---|---|---|",
    ]
    for c in cases:
        note = c["category"] + ("・例外を期待" if c["mode"] == "throwing" else "")
        lines.append(f"| {c['id']} | KSP 公式テスト `{c['method']}`（`{c['test_data']}`）が、4 つの API 機能の組すべてで期待どおりの出力になる "
                     f"| D | {c['milestone']} | {note} |")
    lines += ["", "### 6.2 取り込まなかったテスト", "", "| 上流のテスト | 理由 |", "|---|---|"]
    for e in excluded:
        lines.append(f"| `{e['method']}` | {e['reason']} |")
    block = "\n".join(lines)
    md = INVENTORY.read_text()
    new, n = re.subn(r"<!-- kspt:start -->.*<!-- kspt:end -->", f"<!-- kspt:start -->\n{block}\n<!-- kspt:end -->", md, flags=re.S)
    if n != 1:
        raise SystemExit("test-case-inventory.md に <!-- kspt:start --> / <!-- kspt:end --> が無い")
    INVENTORY.write_text(new)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--upstream", required=True, type=Path, help="google/ksp の checkout（固定コミット）")
    args = ap.parse_args()
    up: Path = args.upstream.resolve()
    props = read_props()
    sha = props["commit"]
    head = sh("git", "rev-parse", "HEAD", cwd=up).strip()
    if head != sha:
        raise SystemExit(f"上流の checkout が固定コミットではない: {head}（期待 {sha}）")

    tests = parse_suite(up / SUITE)
    resolve_calls(tests, up / AA_CONFIGURED)
    unknown = set(EXCLUDED) - {t["method"] for t in tests}
    if unknown:
        raise SystemExit(f"EXCLUDED に上流に無いテストがある: {sorted(unknown)}")

    cases, excluded = [], []
    for t in tests:
        if t["method"] in EXCLUDED:
            excluded.append(dict(method=t["method"], reason=EXCLUDED[t["method"]]))
            continue
        kinds = {c[0] for c in t["calls"]}
        paths = {c[1] for c in t["calls"]}
        if len(paths) != 1 or not kinds <= {"runKSPTest", "runThrowingKSPTest"} or len(kinds) != 1:
            raise SystemExit(f"{t['method']}: 呼び出しの形が想定外（EXCLUDED に足すか script を直す）: {t['calls']}")
        test_data = paths.pop()
        text = (up / TESTDATA_SRC / test_data).read_text()
        milestone, category = classify(t["method"].removeprefix("test"), test_data, text, t["ann"])
        mode = "throwing" if kinds == {"runThrowingKSPTest"} else "positive"
        cases.append(dict(id=case_id(t["method"]), method=t["method"], test_data=test_data,
                          milestone=milestone, category=category, mode=mode, text=text))

    # 前回の取り込みを消してから写し直す（上流で消えたファイルを残さない）
    for d in (TESTDATA_DST, PROCESSOR_DST):
        if d.exists():
            for f in sorted(d.rglob("*"), reverse=True):
                f.unlink() if f.is_file() else f.rmdir()

    port_rows = []
    for rel in sorted({c["test_data"] for c in cases}):
        path, size, owner = copy_test_data(up, sha, rel)
        port_rows.append(dict(upstream_path=path, size_bytes=size, upstream_copyright=owner, target_module=MODULE,
                              strategy="as-is", phase="P1", status="ported",
                              target_paths=(TESTDATA_DST / rel).relative_to(ROOT).as_posix(),
                              notes="KSPT の testData（ADR-024）。出所の行は末尾"))
    roots = {re.search(r"//\s*TEST PROCESSOR:\s*(\w+)", c["text"]).group(1) for c in cases}
    roots.add("AbstractTestProcessor")
    for name, src in sorted(processor_closure(up, roots).items()):
        rel, size, owner, strategy = copy_processor(sha, up, src)
        port_rows.append(dict(upstream_path=rel, size_bytes=size, upstream_copyright=owner, target_module=MODULE,
                              strategy=strategy, phase="P1", status="ported",
                              target_paths=(PROCESSOR_DST / name).relative_to(ROOT).as_posix(),
                              notes="KSPT のテスト用 Processor（ADR-024）。期待値に FQN が出るためパッケージは上流のまま"))
    update_port_map(port_rows)
    write_cases(cases)
    write_inventory(cases, excluded, props["tag"], sha)
    by_m = {m: sum(1 for c in cases if c["milestone"] == m) for m in ("M1", "M2", "M3")}
    print(f"KSPT: 取り込み {len(cases)} 件 {by_m}・除外 {len(excluded)} 件・"
          f"testData {len({c['test_data'] for c in cases})} 件・Processor {len(port_rows) - len({c['test_data'] for c in cases})} 件")
    return 0


if __name__ == "__main__":
    sys.exit(main())
