# 三者差分ハーネス（実装者向け）

決定の理由と代替案は ADR-023（共有フォルダ `pm/adr/ADR-023-differential-harness.md`）、全体の設計は [`quality/pbt-differential-design.md`](../../quality/pbt-differential-design.md)。コードは内部モジュール `:differential`。

## 使い方

```kotlin
val input = DiffInput(
    name = "moshi-nullable-default",
    sources = listOf(DiffSource("com/example/User.kt", "...")),
    processors = listOf(DiffProcessor("MSH", "1.15.2", ksp = { JsonClassSymbolProcessorProvider() })),
    options = mapOf("moshi.generated" to "javax.annotation.Generated"),
)
val result = DifferentialHarness.default().run(input)
result.assertRequiredMatch()          // 必須の観点で K と違えば差分付きで落ちる
result.requireCompared(EngineKind.B)  // B が動いて比べられたことまで求めるとき
```

1 実行系だけ動かすときは `harness.runEngine(EngineKind.K, input)`。

## 守ること

- K は基準。B / N のランナーを足すときも `EngineRunner` の決まりに従う: 動かせないときは例外ではなく `EngineRun.Unavailable(理由)`、生成物のキーは `kotlin/…`・`java/…`・`resources/…`、Logger の出力は `fromProcessor = true`
- KSP の Processor は `ProcessorRecorder` で包んで記録する（B も同じ）。包んで振る舞いを変えない
- 比べる強さは `ComparisonPolicy.forBridge / forNative(マイルストーン)` を使う。必須の観点を独自に減らさない（R-01）
- 生成物に日時などの埋め込みを見つけたら `EmbeddedValueRule` を足し、XD-16 に「消す」「残す」の両方の例を足す
- K 結果キャッシュの鍵に効く情報（`RunOutput` の形、記録の仕方）を変えたら `KCacheKey.FORMAT_VERSION` を上げる
- Processor の版は `fixtures/processors/processors.toml` から読む（ADR-019）
- KMP の SourceSet と Android は同じプロセス内の K では動かない（`Unavailable`）。Gradle フィクスチャのランナーで扱う
