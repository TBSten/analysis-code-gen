# analysis-code-gen

Kotlin Analysis API を直接使うコード生成基盤（KSP2 の代替を目指す）。現在は P1（検証・設計・テストケース整備）で、実装はまだ入っていない。

## モジュール

| モジュール | 公開 | 役割 |
|---|---|---|
| `processor-api` | 公開 | Native Processor 向け API |
| `gradle-plugin` | 公開 | KGP / AGP 統合 |
| `testing` | 公開 | kct fork を使ったテストフレームワーク |
| `ksp-bridge-api` | 公開 | KSP Bridge の公開部分（Hybrid Processor など） |
| `ksp-bridge-gradle-plugin` | 公開 | KSP Bridge の Gradle plugin |
| `project-model` | 内部 | SourceSet / Compilation / 依存のモデル |
| `engine-spi` | 内部 | Bridge などが共通実行基盤に参加する SPI |
| `analysis-engine` | 内部 | AA 環境・Module Graph・キャッシュ無効化 |
| `incremental-engine` | 内部 | 増分処理 |
| `processing-engine` | 内部 | ラウンド実行・生成物管理 |
| `ksp-bridge` | 内部 | KSP API の AA 実装（KSP2 から移植） |
| `cli` | 内部 | Gradle に依存しない実行 |
| `benchmarks` / `e2e-tests` / `ksp-compatibility-tests` | テスト | 性能・E2E・KSP 互換 |
| `pbt` | テスト | PBT 基盤（Arb・縮小・seed・試行数の自動調整、ADR-022） |
| `test-processors:core` / `:ksp` / `:native` | テスト | テスト用 Processor TP-*（`test-processors/README.md`） |

境界は ADR-012〜017 で確定する（設計文書 16 章の案が元）。

## ビルド

JDK 21 で `./gradlew build`。生成物は JVM 17 向け。共通設定は `build-logic/` の convention plugin（`acg.jvm` / `acg.library` / `acg.gradle-plugin`）、バージョンは `gradle/libs.versions.toml`。

## CI

PR ごとの CI、夜間の全 Kotlin 版マトリクス、上流 Kotlin の検知、docs のデプロイがある。中身と、ブランチ保護・Pages などの GitHub 側の設定手順は [.github/CI.md](.github/CI.md)。

## 開発の進め方

- 変更は branch → PR → CI が緑ならマージ（AI Agent が自分でマージしてよい）
- テストを弱める変更は禁止。Agent 向けのルールは `CLAUDE.md` と `.claude/rules/`、繰り返す手順は `.claude/skills/`
- ライセンス: Apache License 2.0
