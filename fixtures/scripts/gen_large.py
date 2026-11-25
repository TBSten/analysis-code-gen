#!/usr/bin/env python3
"""F-LARGE（大規模フィクスチャ）を生成する。ADR-027

使い方:
    python3 fixtures/scripts/gen_large.py --files 1000
    python3 fixtures/scripts/gen_large.py --files 5000 --deps 300
    python3 fixtures/scripts/gen_large.py --files 1000 --out build/fixtures/my-large

既定の出力先は build/fixtures/large-<files>[-deps<deps>]（git 管理外）。生成物は他のフィクスチャと同じく
build.gradle.kts（公開 DSL）と build.ksp.gradle.kts（KSP2）を持ち、-Pacg.fixture.engine=ksp で切り替える。

中身（同じ引数なら毎回同じ内容。乱数は使わない）:
- 50 ファイルずつのパッケージ p000, p001, ...
- 各ファイル: @Mapped の data class 1 つ（前のファイルのクラスを参照する）と、生成物を使う関数・本体を持つ関数
- 10 ファイルに 1 つ @Registered、100 ファイルに 1 つ @CallGraph
- --deps 300: fixtures/large/deps-300.txt の依存（AWS SDK v2 のサービスモジュール）を implementation に足し、
  10 ファイルに 1 つはその依存の型を参照する
"""

from __future__ import annotations

import argparse
import os
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANNOTATIONS = ROOT / "fixtures/projects/jvm/src/main/kotlin/me/tbsten/analysiscodegen/tp/TpAnnotations.kt"
BUILD_SUPPORT = ROOT / "fixtures/projects/build-support"
DEPS_FILE = ROOT / "fixtures/large/deps-300.txt"
FILES_PER_PACKAGE = 50
BASE_PACKAGE = "fixture.large"


def read_deps(count: int) -> list[str]:
    if count == 0:
        return []
    deps = [line.strip() for line in DEPS_FILE.read_text().splitlines() if line.strip() and not line.startswith("#")]
    if count > len(deps):
        sys.exit(f"--deps は {len(deps)} 以下（{DEPS_FILE.relative_to(ROOT)} の行数）")
    return deps[:count]



def package_of(i: int) -> str:
    return f"{BASE_PACKAGE}.p{i // FILES_PER_PACKAGE:03d}"


def model_name(i: int) -> str:
    return f"Model{i:05d}"


def kotlin_file(i: int, deps: list[str]) -> str:
    pkg = package_of(i)
    name = model_name(i)
    lines = [f"package {pkg}", "", "import me.tbsten.analysiscodegen.tp.Mapped"]
    registered = i % 10 == 0
    call_graph = i % 100 == 0
    if registered:
        lines.append("import me.tbsten.analysiscodegen.tp.Registered")
    if call_graph:
        lines.append("import me.tbsten.analysiscodegen.tp.CallGraph")
    lines.append("")
    props = ["    val id: Int", "    val name: String", "    val tags: List<String>"]
    if i > 0:
        prev = i - 1
        props.append(f"    val previous: {package_of(prev)}.{model_name(prev)}?")
    if deps and i % 10 == 5:
        # 依存の jar に含まれるパッケージの型を参照して、クラスパスの解決を必要にする
        dep = deps[(i // 10) % len(deps)]
        props.append(f"    val sdkVersion: software.amazon.awssdk.core.SdkBytes? = null, // {dep}")
    lines.append("@Mapped")
    if registered:
        lines.append("@Registered")
    lines.append(f"data class {name}(")
    lines.extend(p + ("," if not p.endswith(",") and "//" not in p else "") for p in props)
    lines.append(")")
    lines.append("")
    lines.append(f"fun describe{i:05d}(value: {name}): String = {name}Mapper.toMap(value).toString() + weight{i:05d}(value.id)")
    lines.append("")
    if call_graph:
        lines.append("@CallGraph")
    lines.append(f"fun weight{i:05d}(x: Int): Int {{")
    lines.append("    var total = 0")
    lines.append("    for (k in 0 until x % 7) total += k * x")
    lines.append("    return total + listOf(x, total).maxOf { it }")
    lines.append("}")
    return "\n".join(lines) + "\n"


def settings(out: Path, name: str) -> str:
    rel = os.path.relpath(BUILD_SUPPORT, out)
    return f"""// F-LARGE（fixtures/scripts/gen_large.py が生成）。ADR-027
pluginManagement {{
    includeBuild("{rel}")
}}

plugins {{
    id("acg.fixture-settings")
}}

rootProject.name = "{name}"
"""


def build_files(deps: list[str]) -> tuple[str, str]:
    dep_lines = "".join(f'    implementation("{d}")\n' for d in deps)
    acg = f"""plugins {{
    alias(libs.plugins.kotlin.jvm)
    id("me.tbsten.analysiscodegen")
}}

analysisCodeGen {{
    arg("tp.processors", "Mapper,Registry,Body")
    sourceSets {{
        main {{ processor(acg.testProcessors.native) }}
    }}
}}

dependencies {{
{dep_lines}}}
"""
    ksp = f"""plugins {{
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}}

ksp {{
    arg("tp.processors", "Mapper,Registry")
}}

dependencies {{
    ksp(acg.testProcessors.ksp)
{dep_lines}}}
"""
    return acg, ksp


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--files", type=int, required=True, help="Kotlin ファイル数（1,000 / 5,000）")
    parser.add_argument("--deps", type=int, default=0, help="外部依存の数（0 / 300）")
    parser.add_argument("--out", type=Path, help="出力先（既定 build/fixtures/large-<files>[-deps<deps>]）")
    args = parser.parse_args()
    if args.files < 1:
        sys.exit("--files は 1 以上")

    name = f"large-{args.files}" + (f"-deps{args.deps}" if args.deps else "")
    out = (args.out or ROOT / "build/fixtures" / name).resolve()
    if out.exists():
        shutil.rmtree(out)
    deps = read_deps(args.deps)

    src = out / "src/main/kotlin"
    ann_dir = src / "me/tbsten/analysiscodegen/tp"
    ann_dir.mkdir(parents=True)
    shutil.copy(ANNOTATIONS, ann_dir / ANNOTATIONS.name)
    for i in range(args.files):
        pkg_dir = src / package_of(i).replace(".", "/")
        pkg_dir.mkdir(parents=True, exist_ok=True)
        (pkg_dir / f"{model_name(i)}.kt").write_text(kotlin_file(i, deps))

    (out / "settings.gradle.kts").write_text(settings(out, f"fixture-{name}"))
    acg, ksp = build_files(deps)
    (out / "build.gradle.kts").write_text(acg)
    (out / "build.ksp.gradle.kts").write_text(ksp)
    (out / "gradle.properties").write_text(
        "org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8\norg.gradle.configuration-cache=true\nkotlin.code.style=official\n"
    )
    print(f"{out}（Kotlin {args.files} ファイル、依存 {len(deps)} 個）")


if __name__ == "__main__":
    main()
