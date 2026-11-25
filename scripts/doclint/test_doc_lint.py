#!/usr/bin/env python3
"""doc_lint.py の自己テスト。python3 scripts/doclint/test_doc_lint.py で動く。"""

from __future__ import annotations

import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
sys.path.insert(0, str(HERE))
import doc_lint as dl  # noqa: E402

CONFIG = dl.load_config(REPO / "docs" / "doc-lint.toml")
GUIDE = "docs/site/src/content/docs/guides/x.md"
PAGE = "docs/site/src/content/docs/index.mdx"
INTERNAL = "docs/site/src/content/docs/internal/x.md"


def rules(rel: str, text: str) -> list[str]:
    vs, _, _ = dl.check(rel, textwrap.dedent(text), CONFIG)
    return sorted(v.rule for v in vs)


def ja(n: int) -> str:
    return "あ" * n


def guide(*sections: str) -> str:
    body = "\n\n".join(sections)
    return f"---\ntitle: t\n---\n\n導入。\n\n{body}\n\n## まとめ\n\n| a | b |\n|---|---|\n\n## 次のステップ\n\n- [x](./x)\n"


SEC = "## 節\n\n紹介。\n\n```kotlin\nval x = 1\n```\n\n説明。"


class CountTest(unittest.TestCase):
    def test_excludes_code_table_list_heading_and_components(self) -> None:
        doc = dl.parse(textwrap.dedent("""\
            ---
            title: タイトルは数えない
            ---
            import { LinkCard } from '@astrojs/starlight/components';

            ## 見出しは数えない

            本文です。

            ```kotlin
            val ignored = "コードは数えない"
            ```

            | 表は | 数えない |
            |---|---|

            - リストも数えない
              続きの行も数えない

            <LinkCard
              title="カードは数えない"
              href="/x/"
            />
            <!-- コメントは
            数えない -->
            """))
        self.assertEqual(doc.chars, 5)  # 「本文です。」

    def test_inline_markup_counts_visible_text_only(self) -> None:
        doc = dl.parse("[リンク](https://example.com/very/long) と `code` と **強調**。\n")
        self.assertEqual(doc.chars, len("リンクとcodeと強調。"))

    def test_whitespace_including_fullwidth_is_ignored(self) -> None:
        self.assertEqual(dl.parse("あ い　う\n").chars, 3)

    def test_paragraph_spans_lines_until_blank(self) -> None:
        doc = dl.parse("一行目。\n二行目。\n\n別の段落。\n")
        self.assertEqual([p.chars for p in doc.paragraphs], [8, 5])
        self.assertEqual([p.line for p in doc.paragraphs], [1, 4])

    def test_aside_content_counts_as_prose(self) -> None:
        doc = dl.parse(":::note\n注意書き。\n:::\n\n<Aside>\n補足。\n</Aside>\n")
        self.assertEqual(doc.chars, len("注意書き。補足。"))

    def test_h3_belongs_to_h2_section(self) -> None:
        doc = dl.parse("## A\n\nあ。\n\n### B\n\nい。\n\n```\nx\n```\n")
        self.assertEqual([(s.title, s.chars, s.code_blocks) for s in doc.h2_sections], [("A", 4, 1)])

    def test_nested_fence_with_longer_marker(self) -> None:
        doc = dl.parse("````md\n```kotlin\nx\n```\n````\n")
        self.assertEqual([c.lines for c in doc.code_blocks], [3])
        self.assertEqual(doc.chars, 0)


class RuleTest(unittest.TestCase):
    def test_template_passes(self) -> None:
        rel = "docs/templates/guide.md"
        self.assertEqual(rules(rel, (REPO / rel).read_text(encoding="utf-8")), [])

    def test_page_limit(self) -> None:
        text = "\n\n".join(f"## 節{i}\n\n" + "\n\n".join([ja(99)] * 3) for i in range(5))
        self.assertIn("DL001", rules(PAGE, text))  # 99*3*5 = 1485 > 1400

    def test_paragraph_limit_boundary(self) -> None:
        self.assertEqual(rules(PAGE, ja(100)), [])
        self.assertEqual(rules(PAGE, ja(101)), ["DL002"])

    def test_section_limit_boundary_and_lead(self) -> None:
        ok = "## A\n\n" + "\n\n".join([ja(80)] * 4)
        self.assertEqual(rules(PAGE, ok), [])
        self.assertEqual(rules(PAGE, ok + "\n\n" + ja(1)), ["DL003"])
        self.assertEqual(rules(PAGE, "\n\n".join([ja(81)] * 4)), ["DL003"])  # 導入部

    def test_code_lines_boundary(self) -> None:
        def block(n: int) -> str:
            return "```\n" + "x\n" * n + "```\n"
        self.assertEqual(rules(PAGE, block(15)), [])
        self.assertEqual(rules(PAGE, block(16)), ["DL004"])
        self.assertEqual(rules(INTERNAL, block(25)), [])

    def test_h2_count_and_depth(self) -> None:
        self.assertEqual(rules(PAGE, "\n\n".join(f"## {i}" for i in range(8))), [])
        self.assertEqual(rules(PAGE, "\n\n".join(f"## {i}" for i in range(9))), ["DL005"])
        self.assertEqual(rules(PAGE, "## a\n\n### b\n\n#### c\n"), ["DL006"])
        self.assertEqual(rules(PAGE, "# title\n"), ["DL007"])

    def test_unclosed_fence(self) -> None:
        self.assertEqual(rules(PAGE, "```\nx\n"), ["DL011"])

    def test_guide_shape_ok(self) -> None:
        self.assertEqual(rules(GUIDE, guide(SEC, SEC)), [])

    def test_guide_section_without_code(self) -> None:
        self.assertEqual(rules(GUIDE, guide(SEC, "## 文だけ\n\n説明。")), ["DL008"])

    def test_guide_code_component_counts(self) -> None:
        self.assertEqual(rules(GUIDE, guide('## 部品\n\n<Code code="x" lang="kotlin" />')), [])

    def test_guide_must_end_with_summary_then_next(self) -> None:
        text = f"{SEC}\n\n## 次のステップ\n\n- x\n\n## まとめ\n\n| a |\n"
        self.assertEqual(rules(GUIDE, text), ["DL009"])
        self.assertEqual(rules(GUIDE, SEC), ["DL009"])

    def test_guide_summary_needs_table(self) -> None:
        text = f"{SEC}\n\n## Summary\n\n表が無い。\n\n## Next steps\n\n- x\n"
        self.assertEqual(rules(GUIDE, text), ["DL010"])

    def test_non_guide_skips_shape(self) -> None:
        self.assertEqual(rules(PAGE, "## 文だけ\n\n説明。"), [])

    def test_frontmatter_override(self) -> None:
        text = "---\ntitle: t\ndocLint: page\n---\n\n## 文だけ\n\n説明。\n"
        self.assertEqual(rules(GUIDE, text), [])
        self.assertEqual(rules(GUIDE, text.replace("page", "nope")), sorted(["DL012", "DL008", "DL009"]))


class GlobTest(unittest.TestCase):
    def test_glob(self) -> None:
        self.assertTrue(dl.glob_match("a/**/*.md", "a/x.md"))
        self.assertTrue(dl.glob_match("a/**/*.md", "a/b/c/x.md"))
        self.assertFalse(dl.glob_match("a/*.md", "a/b/x.md"))
        self.assertTrue(dl.glob_match("**", "anything/at/all"))

    def test_profile_order(self) -> None:
        self.assertEqual(CONFIG.profile_for(GUIDE).name, "guide")
        self.assertEqual(CONFIG.profile_for(INTERNAL).name, "internal")
        self.assertEqual(CONFIG.profile_for(PAGE).name, "page")


class CliTest(unittest.TestCase):
    def run_cli(self, root: Path, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(HERE / "doc_lint.py"), "--root", str(root),
             "--config", str(REPO / "docs" / "doc-lint.toml"), *args],
            capture_output=True, text=True,
        )

    def test_exit_codes_and_github_format(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.assertEqual(self.run_cli(root).returncode, 0)  # 対象なし
            d = root / "docs/site/src/content/docs/guides"
            d.mkdir(parents=True)
            (d / "ok.md").write_text(guide(SEC), encoding="utf-8")
            self.assertEqual(self.run_cli(root).returncode, 0)
            (d / "ng.md").write_text(ja(101) + "\n", encoding="utf-8")
            (root / "docs/site/src/content/docs/reference/api").mkdir(parents=True)
            (root / "docs/site/src/content/docs/reference/api/gen.md").write_text(ja(999), encoding="utf-8")
            r = self.run_cli(root, "--format", "github")
            self.assertEqual(r.returncode, 1)
            self.assertIn("::error file=docs/site/src/content/docs/guides/ng.md,line=1,title=doc-lint DL002::", r.stdout)
            self.assertNotIn("reference/api", r.stdout)


if __name__ == "__main__":
    unittest.main()
