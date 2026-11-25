# 実用 Processor のサンプルとベンチ（実装者向け）

決定の理由は ADR-037（共有フォルダ `pm/adr/ADR-037-pending-tests-batch3.md`）。

保留テスト（`e2e-tests` の `e2e.rw`・`perf`・`mem`）がこの形を前提にしている。

## サンプル

- 置き場は `fixtures/real-world/<id 小文字>/<サンプル>/`（`jvm`・`kmp`・`kmp-multi`・`android`・`hilt-android`、複数適用は `cmb/…`）
- ビルドスクリプトは `build.gradle.kts`（Native）・`build.bridge.gradle.kts`（Bridge）・`build.ksp.gradle.kts`（KSP2）。`-Pacg.fixture.engine=native|bridge|ksp` で切り替える
- Processor の版は `-Pacg.processor.<ID>.version` で受け取る。直書きしない（ADR-019）
- 生成コードを使う実行時テストを `src/test` に置く。Native 版は KSP2 と同じ件数のテストを通す
- `src/main/kotlin` は 1 ファイル 1 宣言・ファイル名 = 宣言名
- 上流のテストスイートは `fixtures/real-world/init/ksp-to-bridge.init.gradle.kts` で KSP を Bridge に差し替えて流す

## ベンチ

- テストが渡す初期化スクリプトが `acgCodegen`（生成タスクの集約）を足す。生成タスクの名前は KSP2 が `ksp*`、本体が `analysisCodeGen*`（ADR-016）から外さない
- 判定は KSP2 の中央値以下。結果は `build/acg/bench/results.tsv`、退行検出は `scripts/bench/check_regression.py`

## バージョン

- AGP の対応表は `gradle/agp-versions.txt`、Kotlin は `gradle/kotlin-versions.txt` を `scripts/kotlin-versions.py add <版>` で直す
