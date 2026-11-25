#!/usr/bin/env python3
"""test_guard.py の自己テスト。python3 scripts/ci/test_test_guard.py"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import test_guard as g  # noqa: E402

DIFF = """diff --git a/e2e-tests/src/test/kotlin/A.kt b/e2e-tests/src/test/kotlin/A.kt
+++ b/e2e-tests/src/test/kotlin/A.kt
@@ -10,0 +11,2 @@
+    xcase("INC-14", "t") { }
+    case("INC-15", "t", pending = PendingCase(Milestone.M2)) { }
diff --git a/x/src/main/kotlin/B.kt b/x/src/main/kotlin/B.kt
+++ b/x/src/main/kotlin/B.kt
@@ -1,0 +1,1 @@
+val enabled = false
"""


class DisableTest(unittest.TestCase):
    def check(self, line: str) -> list[str]:
        return g.find_disables([("a/src/test/kotlin/A.kt", 1, line)])

    def test_detects(self):
        for line in [
            "@Ignore", '@Disabled("x")', '"a".config(enabled = false) {}', 'xtest("a") {}', 'xcase("INC-1", "t") {}',
            '"!INC-14 a" { }', '"!group" - {', "assumeTrue(false)", "@Ignored class A : AcgSpec({})",
        ]:
            self.assertTrue(self.check(line), line)

    def test_allows_normal_and_pending(self):
        for line in [
            'case("INC-14", "タイトル") { }',
            'case("INC-14", "タイトル", pending = PendingCase(Milestone.M2, PendingReason.Adr(31))) { }',
            "val enabled = true", "// @Ignore は使わない", 'box("!")',
        ]:
            self.assertFalse(self.check(line), line)

    def test_diff_parsing_only_test_sources(self):
        lines = g.added_lines(DIFF)
        self.assertEqual([(f, n) for f, n, _ in lines], [("e2e-tests/src/test/kotlin/A.kt", 11), ("e2e-tests/src/test/kotlin/A.kt", 12)])
        self.assertEqual(len(g.find_disables(lines)), 1)

    def test_test_path(self):
        for p in ["e2e-tests/src/test/kotlin/A.kt", "x/src/integrationTest/java/B.java", "testing/src/testFixtures/kotlin/C.kt"]:
            self.assertTrue(g.TEST_PATH_RE.search(p), p)
        self.assertFalse(g.TEST_PATH_RE.search("x/src/main/kotlin/A.kt"))


if __name__ == "__main__":
    unittest.main()
