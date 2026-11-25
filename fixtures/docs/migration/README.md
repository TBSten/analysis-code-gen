# fixtures/docs/migration

docs サイトの「既存の KSP Processor を移行する」（`docs/site/src/content/docs/migration/`）のコード例の出所。
ページのコード例は、このディレクトリのファイルの一部をそのまま写したもので、`scripts/docsnippets/check_snippets.py` が一致を検査する（ADR-031）。

| モジュール | 段階 |
|---|---|
| `ksp-app` | 移行前。公式 KSP で `hello-ksp-processor` を動かす |
| `app` | 1. Bridge。同じ Processor を KSP Bridge で動かす |
| `hello-ksp-processor` | 移行前と 1. の Processor（KSP API だけ） |
| `hello-hybrid-processor` | 2. Hybrid。`withAnalysis` で初期化式を読む |
| `hello-native-processor` | 3. Native。`AnalysisCodegenProcessor` に書き換えたもの |

期待する結果（DOC-05 の E2E がこの順に確かめる）:

- `./gradlew :ksp-app:run` と `./gradlew :app:run` が同じ `Hello, Greeter!` を出し、生成物が一致する
- `:hello-ksp-processor:test`（KSP2 と Bridge の比較）が通る
- `app` の依存を `hello-hybrid-processor` に替えると `Hi, Greeter!`、`hello-native-processor`（plugin は `me.tbsten.analysis.code.gen`）に替えても `Hi, Greeter!`

このディレクトリはリポジトリのビルドには入らない。実装（M3）後に E2E（DOC-05）がこのプロジェクトをビルド・実行する。
