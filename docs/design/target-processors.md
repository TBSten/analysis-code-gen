# 移植・検証する既存 Processor（実装者向け）

決定の理由と代替案は ADR-019（共有フォルダ `pm/adr/ADR-019-target-processors.md`）。版とコミットの唯一の一覧は [`fixtures/processors/processors.toml`](../../fixtures/processors/processors.toml) で、`python3 scripts/processors.py check` が CI で検査する。

## 対象

| ID | Processor | 版 | Bridge | Native | KMP |
|---|---|---|---|---|---|
| CRM | cream.kt | 0.10.0 | 必須 | 必須 | ○ |
| MSH | Moshi codegen | 1.15.2 | 必須 | 必須 | × |
| KOI | Koin Annotations（KSP 版） | 2.3.1 | 必須 | 必須 | ○ |
| KIN | kotlin-inject | 0.9.0 | 必須 | 任意 | ○ |
| DAG | Dagger / Hilt | 2.60.1 | 必須 | 任意 | × |
| ROM | Room | 2.8.5 | 必須 | なし | ○ |

ID はテストケース ID（`RW-CRM-B1` など）と入力カタログのパターン ID（`KOI-R-007` など）の接頭辞と同じ。

## 守ること

- フィクスチャ・E2E・差分ハーネスで Processor の版を書くときは `processors.toml` から読む。版を直書きしない
- 版を上げるのは意図して行う（毎朝の上流確認でチケットを切る）。上げたら ADR-019 の表も直し、入力カタログを取り込み直す
- Native 版はコード生成部分（KotlinPoet での組み立て）を固定コミットの元コードから流用する。写したファイルには ADR-018 と同じ書式で出所を書く（`Ported-From: <owner>/<repo>@<SHA>:<パス>`）
- 源 ② の実プロジェクトは固定コミットで取得し、Processor の版を固定版に揃えてからビルドする。揃えられないものは外す
- Room・Hilt・AGP は Google Maven が要る。クラウドの Agent 環境からは届かないので、それらの E2E は GitHub Actions で回す

## 注意が要る Processor

- **Moshi 1.15.2** は KSP2 の既知の不具合（#1870・#1874）を含む。K が失敗する入力は、B も同じように失敗することを確かめる（XD-08）。「わざと誤った入力」とは別に記録する
- **Koin の KSP 版** は上流で非推奨（Koin Compiler Plugin へ移行）。固定版のまま比べ続ける
- **Dagger / Room** は XProcessing 経由で KSP API をほぼ全域使う。Bridge の未実装 API はここで最初に見つかりやすい
