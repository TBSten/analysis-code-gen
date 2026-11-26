# ガイドページの決まり（ADR-033）

- ガイドは `docs/site/src/content/docs/guides/` に 1 要素 1 ページ。足したら `astro.config.mjs` のサイドバーにも足す。使い方の基本は「ガイド」、知らなくても書ける難しめの話は「Deep dive」、KSP 互換の話は「KSP から移行する」（ADR-047）
- コード例は `fixtures/docs/guides/`（共有の Gradle ビルド）から写し、コードブロックに `title="<相対パス>"` を付ける。`python3 scripts/docsnippets/check_snippets.py` で一致を確かめる
- title の無いコードブロックは、出力・エラーの例、やってはいけない例、短い断片だけ
- API・DSL の名前は analysis-code-gen-processor-api / analysis-code-gen-gradle-plugin / analysis-code-gen-ksp-bridge-api のシグネチャ（ADR-012〜017）に合わせる。名前を変えたらガイドとフィクスチャも直す
- 分量は `python3 scripts/doclint/doc_lint.py --stats`（guide プロファイル）
