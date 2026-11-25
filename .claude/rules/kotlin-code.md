---
paths:
  - "**/*.kt"
  - "**/*.kts"
---

# Kotlin のコード

- パッケージの根は `me.tbsten.analysiscodegen.<モジュール名から - を除いたもの>`（例: `processorapi`）
- モジュールの `build.gradle.kts` は convention plugin（`acg.jvm` / `acg.library` / `acg.gradle-plugin`）を使い、共通設定を個別に書かない。バージョンは `gradle/libs.versions.toml` に集める
- 公開モジュールは explicitApi。公開範囲とモジュール境界は ADR-012。内部モジュールへの依存方向を逆にしない
- 整形は ktlint（ktlint_official、1 行 140 字）。push 前に `./gradlew ktlintCheck`（`build` に含まれる）
- テストは kotest の FreeSpec。テスト基底クラス（ACG-37）ができたらそれを継承する
- AA の版で形が変わる呼び出しは engine に直接書かず、`AaCompat` を通す（ADR-011、`docs/design/kotlin-multi-version.md`）
- 対応 Kotlin を変えるときは `gradle/kotlin-versions.txt` だけを編集する
- コメントは日本語で「なぜ」を書く。KSP2 から移植したファイルは `porting` ルールに従う
