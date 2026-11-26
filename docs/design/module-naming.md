# モジュールとパッケージの名前（ADR-042・044）

公開を見据えた名前の決まり。理由は ADR-042（共有フォルダ `pm/adr/`）。

## 守ること

- group は `me.tbsten.analysis.code.gen`。plugin ID は `me.tbsten.analysis.code.gen` の 1 つだけ（KSP の Processor があれば Bridge を自動で有効にする。ADR-050）
- 公開しうるモジュールは、モジュール名・ディレクトリ名・artifactId をそろえて `analysis-code-gen-<役割>` にする（例: `analysis-code-gen-processor-api`、パスは `:analysis-code-gen-processor-api`）
- 公開しない検査用・テスト用のモジュール（`e2e-tests`・`engine-tests`・`differential`・`pbt`・`quality`・`test-foundation`・`ksp-compatibility-tests`・`benchmarks`・`test-processors:*`）は短い名前のまま
- 内部専用のモジュールを足すときは `settings.gradle.kts` の `internalOnlyModules` に足す。足さないと設定時に失敗する
- パッケージは group と同じ根の `me.tbsten.analysis.code.gen.<短い名前>`（`analysis-code-gen-processor-api` → `processorapi`。ADR-044）

## 古い名前の branch を直す

main を取り込んだあと `python3 scripts/acg51_rename_modules.py`（モジュール名）と `python3 scripts/acg51_rename_packages.py`（パッケージ名）を流すと、ディレクトリの移動と参照の書き換えをまとめて行う。何度流しても同じ結果になる。
