"""check_snippets.py のテスト。python3 -m unittest discover -s scripts/docsnippets"""

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import check_snippets  # noqa: E402

PAGE = """---
title: t
fixture: fx
---

```kotlin title="a.kt"
fun a() = 1
```

```sh
./gradlew run
```
"""


class CheckSnippetsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root / "fx").mkdir()
        self.page = self.root / "page.mdx"

    def tearDown(self):
        self.tmp.cleanup()

    def run_check(self, page_text, file_text):
        self.page.write_text(page_text, encoding="utf-8")
        if file_text is not None:
            (self.root / "fx" / "a.kt").write_text(file_text, encoding="utf-8")
        return check_snippets.check(self.page, self.root)

    def test_一部が一致すれば通る(self):
        self.assertEqual(self.run_check(PAGE, "package x\n\nfun a() = 1   \n\nfun b() = 2\n"), [])

    def test_字下げを浅くした切り出しも通る(self):
        nested = "class C {\n    fun a() = 1\n}\n"
        self.assertEqual(self.run_check(PAGE, nested), [])

    def test_字下げの相対関係が違えば失敗する(self):
        page = PAGE.replace("fun a() = 1", "fun a() =\n1")
        self.assertEqual(len(self.run_check(page, "fun a() =\n    1\n")), 1)

    def test_中身が違えば失敗する(self):
        errors = self.run_check(PAGE, "fun a() = 2\n")
        self.assertEqual(len(errors), 1)
        self.assertIn("一致しません", errors[0])

    def test_ファイルが無ければ失敗する(self):
        self.assertIn("ありません", self.run_check(PAGE, None)[0])

    def test_fixture_の無いページは検査しない(self):
        self.assertEqual(self.run_check(PAGE.replace("fixture: fx\n", ""), "zzz"), [])


if __name__ == "__main__":
    unittest.main()
