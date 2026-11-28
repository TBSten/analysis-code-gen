# フィクスチャ

| 置き場 | 中身 |
|---|---|
| `processors/` | 移植・検証する既存 Processor の一覧（ADR-019） |
| `projects/` | 共通フィクスチャ F-*（ADR-027） |
| `real-world/` | 実用 Processor のサンプル（RW の E2E、ADR-037・070） |
| `large/`・`scripts/gen_large.py` | F-LARGE の生成 |
| `docs/` | ドキュメントのコード例の出所（ADR-031） |

## 共通フィクスチャ

| 名前 | 置き場 | 内容 |
|---|---|---|
| F-JVM | `projects/jvm` | Kotlin/JVM 単一モジュール（main + test） |
| F-JVM-MULTI | `projects/jvm-multi` | `:lib` と `:app` |
| F-JAVA | `projects/java` | Kotlin + Java 混在 |
| F-KMP | `projects/kmp` | jvm・android・iosArm64・iosSimulatorArm64・linuxX64・js・wasmJs |
| F-KMP-CUSTOM | `projects/kmp-custom` | `dependsOn` を手で組んだ階層 |
| F-AND | `projects/android` | アプリ + ライブラリ、free/paid × debug/release、unit test と androidTest |
| F-LARGE | 生成する | 1,000 ファイル版と、5,000 ファイル + 依存 300 個版 |

各フィクスチャは独立した Gradle ビルドで、ビルドスクリプトを 2 つ持つ。

- `build.gradle.kts`: 公開 DSL（`analysisCodeGen {}`）と TP の Native 版。既定
- `build.ksp.gradle.kts`: 同じソースを KSP2 と TP の KSP 版でビルドする。`-Pacg.fixture.engine=ksp` で切り替わる

## 実用 Processor のサンプル

`real-world/<Processor>/<サンプル>`（`crm/jvm`・`msh/jvm`・`kin/jvm`・`koi/jvm`・`cmb/koin-cream`）。スクリプトは 3 つで、`-Pacg.fixture.engine` で切り替える。

- `build.gradle.kts`: N（Native 書き直し版）。まだ無い Processor は、ケース ID を書いたエラーで止まる
- `build.bridge.gradle.kts`: B（Bridge + 元の Processor）。`engine=bridge`
- `build.ksp.gradle.kts`: K（KSP2 + 元の Processor）。`engine=ksp`

Processor の座標は version catalog `rw`、版は `processors.toml`（`-Pacg.processor.<ID>.version` で上書き）。

## 動かし方

```bash
./gradlew publishFixtureDeps                     # TP と plugin を build/fixture-repo へ
./gradlew -p fixtures/projects/jvm -Pacg.fixture.engine=ksp build
./gradlew -p fixtures/real-world/koi/jvm -Pacg.fixture.engine=bridge test
python3 fixtures/scripts/gen_large.py --files 1000   # build/fixtures/large-1000 に生成
```

共通の settings は `projects/build-support` の `acg.fixture-settings`（リポジトリ・version catalog・切り替え）。Android と Google Maven・iOS・linuxX64 を使うものはクラウドの Agent 環境では取得・ビルドできないため、CI（`.github/workflows/fixtures.yml`）で確かめる。
