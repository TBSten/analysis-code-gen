# fixtures/docs/quick-start

docs サイトの「クイックスタート」（`docs/site/src/content/docs/start/quick-start.mdx`）のコード例の出所。
ページのコード例は、このディレクトリのファイルの一部をそのまま写したもので、`scripts/docsnippets/check_snippets.py` が一致を検査する（ADR-031）。

- `./gradlew :app:run` で `Hello, Greeter!` と出る
- `app/build/generated/analysisCodeGen/main/kotlin/com/example/GreeterHello.kt` が `expected/com/example/GreeterHello.kt` と同じになる

このディレクトリはリポジトリのビルドには入らない。実装（M1）後に E2E（DOC-02）がこのプロジェクトをビルド・実行する。
