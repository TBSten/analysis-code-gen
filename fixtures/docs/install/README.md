# fixtures/docs/install

docs サイトの「インストール」（`docs/site/src/content/docs/start/install.mdx`）のコード例の出所（ADR-031）。
ページのコード例は、このディレクトリのファイルの一部をそのまま写したもので、`scripts/docsnippets/check_snippets.py` が一致を検査する。

| モジュール | 見せていること |
|---|---|
| `jvm-app` | Kotlin/JVM に plugin を適用し、Processor を足す |
| `kmp-lib` | KMP の `commonMain` にだけ Processor を付ける |
| `bridge-app` | KSP Bridge で Moshi（KSP Processor）を動かす |
| `hello-processor` | Processor を作る側の依存。ソースは `../quick-start/hello-processor` と同じものを E2E が写す |

このディレクトリはリポジトリのビルドには入らない。実装（M1）後に E2E（DOC-02）が `./gradlew build` で検査する。
