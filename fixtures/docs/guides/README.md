# fixtures/docs/guides

docs サイトの「ガイド」（`docs/site/src/content/docs/guides/*.mdx`）のコード例の出所（ADR-031・ADR-033）。
ガイドの全ページがこの 1 つの Gradle ビルドを共有し、ページのコード例はここのファイルの一部をそのまま写したもの。`scripts/docsnippets/check_snippets.py` が一致を検査する。

| モジュール | 主に使うページ |
|---|---|
| `greeting-processor` | Processor と Provider・増分処理・対応バージョン |
| `builder-processor` | ラウンドとファイル集合 |
| `registry-processor` | 生成と出力の管理・増分処理・KMP |
| `deferred-processor` | Deferred |
| `hybrid-processor` | Hybrid Processor（KSP Processor） |
| `app` | Gradle DSL（`maxRounds`・`unresolvedDeferred`・`worker`） |
| `kmp-lib` | SourceSet First と KMP・Gradle DSL |
| `bridge-app` | KSP Bridge |
| `versions-app` | 対応バージョン（`analysisApiVersion`） |

期待する結果（実装後に E2E が確かめる）:

- `./gradlew :app:run` で `Hi, Greeter!` と `Alice` が出る
- `:kmp-lib` の `commonMain` に `ModelBuilder`、`iosMain` に `ModelGreeting` が生成され、Compilation ごとに `Routes` が生成される
- `:bridge-app` で Moshi の `UserJsonAdapter` と `UserGreeting`・`UserDescription` が同じビルドで生成される

このディレクトリはリポジトリのビルドには入らない。実装（M1）後に E2E（DOC-02・03）がこのプロジェクトをビルド・実行する。
