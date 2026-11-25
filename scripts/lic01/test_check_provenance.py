#!/usr/bin/env python3
"""check_provenance.py の自己テスト。python3 scripts/lic01/test_check_provenance.py で動く。"""

from __future__ import annotations

import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_provenance as cp  # noqa: E402

UP_PATH = "kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/FileCache.kt"
TARGET = "analysis-code-gen-analysis-engine/src/main/kotlin/example/FileCache.kt"

UPSTREAM_HEADER = textwrap.dedent("""\
    /*
     * Copyright 2022 Google LLC
     * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     */
    """)


def make_upstream_repo(tmp: Path) -> tuple[Path, str]:
    up = tmp / "upstream"
    (up / Path(UP_PATH).parent).mkdir(parents=True)
    (up / UP_PATH).write_text(UPSTREAM_HEADER + "package com.google.devtools.ksp.impl\n\nclass FileCache\n")
    env = ["-c", "user.name=t", "-c", "user.email=t@example.com"]
    subprocess.run(["git", "init", "-q", str(up)], check=True)
    subprocess.run(["git", "-C", str(up), "add", "."], check=True)
    subprocess.run(["git", "-C", str(up), *env, "commit", "-q", "-m", "init"], check=True)
    sha = subprocess.run(["git", "-C", str(up), "rev-parse", "HEAD"], check=True,
                         capture_output=True, text=True).stdout.strip()
    return up, sha


class Fixture:
    def __init__(self, root: Path, sha: str) -> None:
        self.root = root
        self.sha = sha
        (root / "porting/ksp").mkdir(parents=True)
        self.write("porting/ksp/UPSTREAM.properties", textwrap.dedent(f"""\
            id=ksp
            repository=https://github.com/google/ksp
            tag=2.3.14
            commit={sha}
            previous_commits=
            notice_marker=Kotlin Symbol Processing (KSP)
            """))
        self.write("NOTICE", f"Kotlin Symbol Processing (KSP) 2.3.14 {sha} Apache License\n")
        self.set_map("planned", "")

    def write(self, rel: str, text: str) -> None:
        p = self.root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text, encoding="utf-8")

    def set_map(self, status: str, targets: str, strategy: str = "as-is") -> None:
        header = ",".join(cp.PORT_MAP_COLUMNS)
        self.write("porting/ksp/port-map.csv",
                   f"{header}\n{UP_PATH},100,Google LLC;JetBrains,analysis-code-gen-analysis-engine,{strategy},P2,{status},{targets},\n")

    def ported_file(self, *, sha: str | None = None, header: str = UPSTREAM_HEADER,
                    modified_by: bool = True, modifications: str | None = "パッケージ名を変更") -> None:
        lines = [header.rstrip("\n"),
                 f"// Ported-From: google/ksp@{sha or self.sha}:{UP_PATH}"]
        if modified_by:
            lines.append("// Modified-By: analysis-code-gen contributors, 2026")
        if modifications is not None:
            lines.append(f"// Modifications: {modifications}")
        lines += ["", "package example", "", "class FileCache", ""]
        self.write(TARGET, "\n".join(lines))
        self.set_map("ported", TARGET)


class CheckProvenanceTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = Path(self._tmp.name)
        self.upstream, self.sha = make_upstream_repo(self.tmp)
        self.root = self.tmp / "repo"
        self.root.mkdir()
        self.fx = Fixture(self.root, self.sha)

    def tearDown(self) -> None:
        self._tmp.cleanup()

    def run_check(self, with_upstream: bool = False) -> list[str]:
        dirs = {"ksp": self.upstream} if with_upstream else {}
        return cp.run(self.root, dirs, cp.DEFAULT_EXCLUDES).errors

    def assertRule(self, errors: list[str], rule: str, needle: str = "") -> None:
        hits = [e for e in errors if e.startswith(f"[{rule}]") and needle in e]
        self.assertTrue(hits, f"[{rule}] {needle!r} の違反が出ていない: {errors}")

    # --- 通る場合 ---
    def test_planned_only_passes(self) -> None:
        self.assertEqual(self.run_check(), [])

    def test_correct_ported_file_passes_including_upstream_check(self) -> None:
        self.fx.ported_file()
        self.assertEqual(self.run_check(with_upstream=True), [])

    def test_header_without_upstream_copyright_line_is_ok_when_added(self) -> None:
        # 上流にヘッダが無いファイルは README の定型ヘッダを足す
        header = "/*\n * Copyright Google LLC and the KSP authors\n * Licensed under the Apache License, Version 2.0\n */"
        self.fx.ported_file(header=header)
        self.assertEqual(self.run_check(), [])

    # --- R1 ---
    def test_missing_notice(self) -> None:
        (self.root / "NOTICE").unlink()
        self.assertRule(self.run_check(), "R1", "NOTICE が無い")

    def test_notice_without_commit(self) -> None:
        self.fx.write("NOTICE", "Kotlin Symbol Processing (KSP) 2.3.14 Apache License\n")
        self.assertRule(self.run_check(), "R1", "commit")

    # --- R2 ---
    def test_missing_copyright(self) -> None:
        self.fx.ported_file(header="/*\n * Licensed under the Apache License, Version 2.0\n */")
        self.assertRule(self.run_check(), "R2", "著作権表示")

    def test_missing_license(self) -> None:
        self.fx.ported_file(header="/*\n * Copyright 2022 Google LLC\n */")
        self.assertRule(self.run_check(), "R2", "ライセンス表示")

    def test_missing_modified_by(self) -> None:
        self.fx.ported_file(modified_by=False)
        self.assertRule(self.run_check(), "R2", "Modified-By")

    def test_missing_or_empty_modifications(self) -> None:
        self.fx.ported_file(modifications=None)
        self.assertRule(self.run_check(), "R2", "Modifications 行が無い")
        self.fx.ported_file(modifications="")
        self.assertRule(self.run_check(), "R2", "Modifications の内容が空")

    def test_unpinned_commit(self) -> None:
        self.fx.ported_file(sha="b" * 40)
        self.assertRule(self.run_check(), "R2", "固定コミットではない")

    def test_previous_commit_is_allowed(self) -> None:
        old = "c" * 40
        props = self.root / "porting/ksp/UPSTREAM.properties"
        props.write_text(props.read_text().replace("previous_commits=", f"previous_commits={old}"))
        self.fx.ported_file(sha=old)
        self.assertEqual(self.run_check(), [])

    def test_tag_instead_of_sha(self) -> None:
        self.fx.ported_file(sha="2.3.14")
        self.assertRule(self.run_check(), "R2", "40 桁")

    def test_malformed_ported_from(self) -> None:
        self.fx.write(TARGET, UPSTREAM_HEADER + "// Ported-From: ksp FileCache.kt\n")
        self.assertRule(self.run_check(), "R2", "書式")

    def test_unknown_upstream(self) -> None:
        self.fx.write(TARGET, UPSTREAM_HEADER + f"// Ported-From: square/moshi@{self.sha}:{UP_PATH}\n"
                      "// Modified-By: x\n// Modifications: y\n")
        self.assertRule(self.run_check(), "R2", "登録されていない上流")

    # --- R3 ---
    def test_port_map_not_marked_ported(self) -> None:
        self.fx.ported_file()
        self.fx.set_map("planned", "")
        self.assertRule(self.run_check(), "R3", "ported になっていない")

    def test_port_map_target_missing_file(self) -> None:
        self.fx.set_map("ported", "nowhere/Foo.kt")
        self.assertRule(self.run_check(), "R3", "ファイルが無い")

    def test_port_map_target_without_marker(self) -> None:
        self.fx.write("other/Foo.kt", "package example\n")
        self.fx.set_map("ported", "other/Foo.kt")
        self.assertRule(self.run_check(), "R3", "Ported-From 行が無い")

    def test_port_map_bad_enum(self) -> None:
        self.fx.set_map("done", "", strategy="copy")
        errors = self.run_check()
        self.assertRule(errors, "R3", "strategy が不正")
        self.assertRule(errors, "R3", "status が不正")

    def test_port_map_reference_cannot_be_ported(self) -> None:
        self.fx.ported_file()
        self.fx.set_map("ported", TARGET, strategy="reference")
        self.assertRule(self.run_check(), "R3", "ported にしない")

    # --- R4 ---
    def test_unrecorded_copy_by_copyright(self) -> None:
        self.fx.write("core/src/Copied.kt", UPSTREAM_HEADER + "package example\n")
        self.assertRule(self.run_check(), "R4", "core/src/Copied.kt")

    def test_unrecorded_copy_by_package(self) -> None:
        self.fx.write("core/src/Copied.java", "package com.google.devtools.ksp.impl;\nclass X {}\n")
        self.assertRule(self.run_check(), "R4", "Copied.java")

    def test_import_of_ksp_api_is_not_a_copy(self) -> None:
        self.fx.write("analysis-code-gen-ksp-bridge/src/Adapter.kt",
                      "package example\nimport com.google.devtools.ksp.processing.SymbolProcessor\n")
        self.assertEqual(self.run_check(), [])

    def test_build_dir_is_ignored(self) -> None:
        self.fx.write("core/build/generated/Copied.kt", UPSTREAM_HEADER)
        self.assertEqual(self.run_check(), [])

    # --- R5 ---
    def test_upstream_copyright_dropped(self) -> None:
        header = "/*\n * Copyright 2022 Google LLC\n * Licensed under the Apache License, Version 2.0\n */"
        self.fx.ported_file(header=header)  # JetBrains の行を落とした
        self.assertEqual(self.run_check(), [])
        self.assertRule(self.run_check(with_upstream=True), "R5", "JetBrains")

    def test_upstream_path_missing(self) -> None:
        self.fx.ported_file()
        text = (self.root / TARGET).read_text().replace(UP_PATH, UP_PATH.replace("FileCache", "Nope"))
        (self.root / TARGET).write_text(text)
        self.fx.write("porting/ksp/port-map.csv",
                      ",".join(cp.PORT_MAP_COLUMNS) + "\n"
                      + f"{UP_PATH.replace('FileCache', 'Nope')},1,Google LLC,analysis-code-gen-analysis-engine,as-is,P2,ported,{TARGET},\n")
        self.assertRule(self.run_check(with_upstream=True), "R5", "無い")

    # --- MPL-2.0 の上流（kotlin-compile-testing、ADR-021） ---
    def _add_mpl_upstream(self) -> str:
        self.fx.write("porting/kct/UPSTREAM.properties", textwrap.dedent(f"""\
            id=kct
            repository=https://github.com/ZacSweers/kotlin-compile-testing
            license=MPL-2.0
            tag=0.14.0
            commit={self.sha}
            previous_commits=
            notice_marker=kotlin-compile-testing
            """))
        self.fx.write("NOTICE", f"Kotlin Symbol Processing (KSP) 2.3.14 kotlin-compile-testing 0.14.0 {self.sha} Apache License\n")
        target = "analysis-code-gen-testing-kct/src/main/kotlin/example/SourceFile.kt"
        up_path = "core/src/main/kotlin/com/tschuchort/compiletesting/SourceFile.kt"
        header = ",".join(cp.PORT_MAP_COLUMNS)
        self.fx.write("porting/kct/port-map.csv", f"{header}\n{up_path},1,,analysis-code-gen-testing-kct,as-is,P1,ported,{target},\n")
        return target

    def _mpl_file(self, target: str, license_lines: str) -> None:
        self.fx.write(target, license_lines + textwrap.dedent(f"""\
            // Copyright kotlin-compile-testing contributors
            // Ported-From: ZacSweers/kotlin-compile-testing@{self.sha}:core/src/main/kotlin/com/tschuchort/compiletesting/SourceFile.kt
            // Modified-By: analysis-code-gen contributors
            // Modifications: パッケージを移動
            package example
            """))

    def test_mpl_upstream_accepts_mpl_header(self) -> None:
        target = self._add_mpl_upstream()
        self._mpl_file(target, "// This Source Code Form is subject to the terms of the Mozilla Public\n"
                               "// License, v. 2.0. If a copy of the MPL was not distributed with this\n")
        self.assertEqual(self.run_check(), [])

    def test_mpl_upstream_rejects_apache_only_header(self) -> None:
        target = self._add_mpl_upstream()
        self._mpl_file(target, "// Licensed under the Apache License, Version 2.0\n")
        self.assertRule(self.run_check(), "R2", "MPL-2.0")

    def test_unrecorded_kct_copy(self) -> None:
        self.fx.write("analysis-code-gen-testing/src/main/kotlin/Copied.kt", "package com.tschuchort.compiletesting\n\nclass X\n")
        self.assertRule(self.run_check(), "R4", "Ported-From 行が無い")

    # --- 実リポジトリ ---
    def test_real_repository_passes(self) -> None:
        repo_root = Path(__file__).resolve().parents[2]
        self.assertEqual(cp.run(repo_root, {}, cp.DEFAULT_EXCLUDES).errors, [])


if __name__ == "__main__":
    unittest.main(verbosity=1)
