# fixtures/docs/testing

docs サイトの「テスト」（`docs/site/src/content/docs/testing/`）のコード例の出所。
ページのコード例は、このディレクトリのファイルの一部をそのまま写したもので、`scripts/docsnippets/check_snippets.py` が一致を検査する（ADR-031）。

- `hello-processor` はクイックスタートと同じ Processor。`src/test` にテストを置いた
- `./gradlew :hello-processor:test` がすべて通る
- `native(...)` は testing にまだ無い（ADR-021。analysis-code-gen-processor-api の中身が入ったら足す）。名前と形は ADR-036 で決めた

このディレクトリはリポジトリのビルドには入らない。実装（M1・M2）後に E2E（TST-01〜06・DOC-03）がこのプロジェクトをビルド・テストする。
