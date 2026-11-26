# ファイルの置き場所（katachi、ACG-50、ADR-041）

どの種類のファイルをどこに置くかは `architecture-test/` の `architecture { }` 定義が唯一の一覧。定義に無いファイルは `:architecture-test:test`（`./gradlew build` と PR の CI に含まれる）で落ちる。

## 決まり

- **新しい種類のファイル・モジュール・ディレクトリを足す PR は、同じ PR で定義も直す**
- 違反を消すために定義を緩めない。何でも受ける Role（`"**" / "*"` だけのもの）や、無関係な Role へのパスの追加は禁止。そのファイルを説明できる Role を足すか、置き場所を直す
- 1 Role = 1 種類のファイル。Role の `summary` 1 文で中身を説明できなくなったら分ける。1 つの Role に違う拡張子のファイルを混ぜない（Kotlin とその設定ファイル・README などは別の Role にする）
- layout はモジュールのパスから書く（パッケージが入れ子のモジュールがあるため）
- 名前で種類が分かるように置く。テストは `*Test.kt` / `*Tests.kt`、テストの補助は `support/` か `*Support.kt`、定義のファイルは `*Group.kt`
- e2e-tests・engine-tests に新しい観点のパッケージを足すときは `PathSupport.kt` の `CASE_ASPECTS` に足す
- 独立した Gradle ビルド（フィクスチャ・PoC）の Gradle のファイルは `independentBuildFiles()` で書く。katachi の `gradle()` はルートのビルドだけを受け持つ

## 中身の規則（ktFileContent・fileConstraint）

- 製品・基盤のモジュールは「パッケージ = ディレクトリ」と、import の許可リストを持つ。許可リストは ADR-012 の依存方向そのもの（例: ksp-bridge はエンジンと engine-spi だけでやり取りする、incremental-engine と gradle-plugin は AA を import しない、e2e-tests は AA の型に触れない）。落ちたら依存の向きを直す。リストを広げるのは ADR を書いてから
- ケーステストのファイルには `UnitSpec` などケースの基底クラスを継承したクラスが要る
- 上流から写したファイル（`testing-kct`・KSPT）は `// Ported-From:` が要る。モジュールの `build.gradle.kts` は `id("acg.*")` が要る

## コマンド

```bash
./gradlew :architecture-test:test --console=plain -Dkatachi.maxViolations=all   # 検査（違反を全部出す）
./gradlew :architecture-test:katachiDocs   # Role ごとの説明を architecture-test/build/katachi/docs に生成
katachi lint                               # Role の分け方の点検（失敗にはしない）
```

定義の直し方・違反の直し方は katachi skill（`.claude/skills/katachi`、`add-and-update` / `fix-violations`）に従う。skill は `gh skill install TBSten/katachi katachi --agent claude-code --pin <版>` で入れたもので、手で直さず katachi の版と一緒に `gh skill update` で上げる。katachi の版上げは skill の upgrade 手順で行い、scaffold が書いた `architecture-test/build.gradle.kts` は手で書き換えない。
