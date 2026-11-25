# ケース ID 付きテストの置き場と補助（実装者向け）

決定の理由は ADR-038（共有フォルダ `pm/adr/ADR-038-pending-tests-batch1.md`）。書き方の決まりは [`.claude/rules/test-cases.md`](../../.claude/rules/test-cases.md)。

## 置き場

| 種別 | モジュール | パッケージ |
|---|---|---|
| E・P（Gradle TestKit） | `:e2e-tests` | `me.tbsten.analysiscodegen.e2e.<観点小文字>` |
| I・U（AA の型に触れる） | `:engine-tests` | `me.tbsten.analysiscodegen.enginetests.<観点小文字>` |

1 クラス 1 種別。同じ観点で E と I があればクラスを分ける。

## E2E の補助（`e2e.support`）

| 補助 | 用途 |
|---|---|
| `fixtureProject(Fixture.JVM) { }` | F-* を一時ディレクトリへ複製して動かす。`edit` / `write` は複製先だけを変える |
| `emptyProject { }` | F-* と同じ settings の空プロジェクト |
| `processorModule(...)` | その場で書いた Native Processor のモジュール |
| `generated(ss)` / `generatedForCompilation(unit)` | ADR-016 の出力ディレクトリ |
| `readProbe(file)` / `plannedTasks(...)` | TP-Probe の記録 / `--dry-run` の実行順 |

## 守ること

- 先に `./gradlew publishFixtureDeps` が要る。`processorModule` を使うケースを有効にするときは analysis-code-gen-processor-api も publish 対象に足す
- 診断は `e: ` / `w: ` で始まる 1 行、要素があれば `<相対パス>:<行>` を含む（ADR-038）
- Android・Apple・Windows のケースは CI の該当ランナーで動かす。クラウドの Agent 環境では動かない
