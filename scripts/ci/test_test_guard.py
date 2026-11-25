#!/usr/bin/env python3
"""test_guard.py の自己テスト。python3 scripts/ci/test_test_guard.py"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import test_guard as g  # noqa: E402


def snap(text: str) -> g.Snapshot:
    s = g.Snapshot()
    g.scan_text(text, s)
    return s


class ScanTest(unittest.TestCase):
    def test_annotation_style(self):
        s = snap('@PendingCase(id = "INC-14", milestone = M2)\nfun x() {}\n"RND-01 rounds" { }')
        self.assertEqual(s.pending_ids, {"INC-14"})
        self.assertEqual(s.all_ids, {"INC-14", "RND-01"})

    def test_freespec_call_style(self):
        s = snap('"BRG-03a 説明" pendingCase(M3, reason = NotImplemented) {}\npendingCase("XD-17") { }')
        self.assertEqual(s.pending_ids, {"XD-17"})
        self.assertIn("BRG-03a", s.all_ids)

    def test_ids_are_restricted(self):
        s = snap('charset("UTF-8") "ISO-8859" "RW-CRM-B1 moshi" "NEW-01"')
        self.assertEqual(s.all_ids, {"RW-CRM-B1"})


class JudgeTest(unittest.TestCase):
    def test_repending_is_error(self):
        base = snap('"INC-14 a" { }')
        head = snap('pendingCase("INC-14") { }')
        errors, _ = g.judge(base, head, [], "")
        self.assertTrue(any("INC-14" in e and "保留に戻" in e for e in errors))

    def test_unpending_is_ok(self):
        errors, warnings = g.judge(snap('pendingCase("INC-14") {}'), snap('"INC-14 a" { }'), [], "")
        self.assertEqual((errors, warnings), ([], []))

    def test_new_pending_case_is_ok(self):
        errors, _ = g.judge(snap(""), snap('pendingCase("GRD-99") {}'), [], "")
        self.assertEqual(errors, [])

    def test_removal_needs_adr(self):
        errors, _ = g.judge(snap('"INC-14" {}'), snap(""), [], "")
        self.assertEqual(len(errors), 1)
        errors, warnings = g.judge(snap('"INC-14" {}'), snap(""), [], "本文\nCase-Removal-ADR: ADR-031\n")
        self.assertEqual((len(errors), len(warnings)), (0, 1))


class DisableTest(unittest.TestCase):
    def check(self, line: str) -> list[str]:
        return g.find_disables([("a/src/test/kotlin/A.kt", 1, line)])

    def test_detects(self):
        for line in [
            "@Ignore", "@Disabled(\"x\")", '"a".config(enabled = false) {}', 'xtest("a") {}',
            '"!INC-14 a" { }', '"!group" - {', "assumeTrue(false)", "@Ignored class A",
        ]:
            self.assertTrue(self.check(line), line)

    def test_ignores_normal(self):
        for line in ['"INC-14 a" { }', "val enabled = true", "// @Ignore は使わない", 'box("!") ']:
            self.assertFalse(self.check(line), line)

    def test_test_path(self):
        for p in ["e2e-tests/src/test/kotlin/A.kt", "x/src/integrationTest/java/B.java", "testing/src/testFixtures/kotlin/C.kt"]:
            self.assertTrue(g.TEST_PATH_RE.search(p), p)
        self.assertFalse(g.TEST_PATH_RE.search("x/src/main/kotlin/A.kt"))


if __name__ == "__main__":
    unittest.main()
