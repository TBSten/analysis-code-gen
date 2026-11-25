# docs/

ドキュメントの置き場所と書き方の決まり。決定の理由は ADR-028（共有フォルダ `pm/adr/ADR-028-docs-structure-and-lint.md`）。

## 構成

| パス | 中身 | 分量チェック |
|---|---|---|
| `site/` | ドキュメントサイト（Astro Starlight、ACG-47 で作る）。人間レビューの対象 | あり |
| `site/src/content/docs/index.mdx` | Overview | page |
| `site/src/content/docs/start/` | Quick start・How to install | guide |
| `site/src/content/docs/guides/` | 重要な要素ごとの解説（1 要素 1 ページ）と、その詳細ページ | guide |
| `site/src/content/docs/testing/` | Testing | guide |
| `site/src/content/docs/migration/` | 既存 KSP plugin の移行方法 | guide |
| `site/src/content/docs/reference/api/` | API reference（Dokka の生成物） | 除外 |
| `site/src/content/docs/internal/` | Internal・Architecture | internal（緩め） |
| `design/` | 実装者向けのメモ（ADR の要約と守ること）。サイトには出さない | なし |
| `templates/guide.md` | ガイドページの雛形。CI で検査して雛形自体が基準を満たすことを保つ | guide |
| `doc-lint.toml` | 分量チェックの基準とプロファイル | — |

## 分量チェック

```sh
python3 scripts/doclint/doc_lint.py --stats          # 設定の include 全体
python3 scripts/doclint/doc_lint.py path/to/page.md  # 1 ページだけ
```

PR ごとに `.github/workflows/doc-lint.yml` が回り、違反があれば失敗する。

| プロファイル | 本文 | 段落 | H2 節 | コード | H2 数 | 型 |
|---|---|---|---|---|---|---|
| guide | 1,400 字 | 100 字 | 320 字 | 15 行 | 8 | 各 H2 にコード例、最後に「まとめ」（表）→「次のステップ」 |
| page | 1,400 字 | 100 字 | 320 字 | 15 行 | 8 | なし |
| internal | 2,800 字 | 100 字 | 320 字 | 25 行 | 12 | なし |

- 文字数は地の文だけを数える（コード・表・リスト・見出し・コンポーネントは除く）。空白は数えない
- 見出しは H3 まで。本文に H1 は書かない（タイトルは frontmatter の `title`）
- ガイド配下の詳細ページなど、型を求めないページは frontmatter に `docLint: page` と書く。分量の基準はそのまま
- 基準を超えたら、基準を緩めずにページを分けるか詳細ページ・Internal へ移す。基準を変えるときは `doc-lint.toml` と ADR-028 を一緒に直す
- 判断が要る論点はページの冒頭にまとめてから人間レビューに出す
