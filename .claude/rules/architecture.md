# ファイルの置き場所（katachi、ACG-50、ADR-041）

どの種類のファイルをどこに置くかは `architecture-test/` の `architecture { }` 定義が唯一の一覧。定義に無いファイルは `:architecture-test:test`（`./gradlew build` と PR の CI に含まれる）で落ちる。

## 決まり

- **新しい種類のファイル・モジュール・ディレクトリを足す PR は、同じ PR で定義も直す**
- 違反を消すために定義を緩めない。何でも受ける Role（`"**" / "*"` だけのもの）や、無関係な Role へのパスの追加は禁止。そのファイルを説明できる Role を足すか、置き場所を直す
- 1 Role = 1 種類のファイル。Role の `summary` 1 文で中身を説明できなくなったら分ける
- layout はモジュールのパスから書く（パッケージが入れ子のモジュールがあるため）
- e2e-tests・engine-tests に新しい観点のパッケージを足すときは `Paths.kt` の `CASE_ASPECTS` に足す
- 独立した Gradle ビルド（フィクスチャ・PoC）の Gradle のファイルは `independentBuildFiles()` で書く。katachi の `gradle()` はルートのビルドだけを受け持つ

## コマンド

```bash
./gradlew :architecture-test:test --console=plain -Dkatachi.maxViolations=all   # 検査（違反を全部出す）
./gradlew :architecture-test:katachiDocs   # Role ごとの説明を architecture-test/build/katachi/docs に生成
katachi lint                               # Role の分け方の点検（失敗にはしない）
```

定義の直し方・違反の直し方は katachi skill（`add-and-update` / `fix-violations`）に従う。katachi の版上げは skill の upgrade 手順で行い、scaffold が書いた `architecture-test/build.gradle.kts` は手で書き換えない。
