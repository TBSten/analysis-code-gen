# 入力パターンカタログ

利用者が書きうる入力コードを、パターンとして集めた置き場。三者差分テスト（K / B / N）の `Arb<UserProject>` と、決定的な網羅層の `Exhaustive` はここから作る。決定の理由は ADR-020、使い方の詳細は [docs/design/input-pattern-catalog.md](../../docs/design/input-pattern-catalog.md)。

| ファイル | 中身 | 手で書くか |
|---|---|---|
| `patterns/<対象>/<ID>.kt` | パターン本体（1 パターン 1 ファイル） | ③ 自作だけ手で書く。①② は取り込み script が書く |
| `dimensions.toml` | 入力値空間の次元と値、自動タグの正規表現、必須かどうか | 手で書く |
| `index.json` | 全パターンの索引（タグ・シャード・期待結果）。kotest の読み込み口 | 自動生成 |
| `coverage.md` | 次元ごとの網羅表（G0 レビューで見てもらう表） | 自動生成 |
| `THIRD_PARTY.md`・`licenses/` | 取り込んだ第三者コードの一覧とライセンス文 | 自動生成 |
| `id-map.tsv` | 出所 → ID の対応（取り込み直しても ID を変えない） | 自動生成 |
| `scripts/catalog.py` | 取り込み・索引・網羅表・検査 | |

取り込み元の版とコミット、源 ② の実プロジェクトの一覧は [`fixtures/processors/processors.toml`](../../fixtures/processors/processors.toml)（ADR-019）だけに書く。

## パターンの形

KSP の testData と同じく、コメントのヘッダーと `// FILE:` 区切りで書く。

```kotlin
// PATTERN: KOI-O-0007
// TITLE: モジュールを Gradle モジュールに分けて includes でつなぐ
// PROCESSORS: koin
// EXPECT: ok
// OPTIONS: KOIN_CONFIG_CHECK=true
// TAGS: koin.module=default-module
// ORIGIN: own
// STATUS: candidate
// FILE: core/main/com/example/core/CoreModule.kt
package com.example.core
...
// FILE: app/main/com/example/app/AppModule.kt
package com.example.app
...
```

| ヘッダー | 意味 |
|---|---|
| `PATTERN` | `<対象>-<出所>-<4 桁>`。対象は CMN（共通の背景コード）・CRM・MSH・KOI、出所は T（① テスト）・R（② 実プロジェクト）・O（③ 自作） |
| `PROCESSORS` | 適用する Processor（`cream`・`moshi`・`koin`）。`*` はどの組み合わせにも混ぜる背景コード |
| `EXPECT` | `ok` / `error`（Processor がエラー）/ `warning` / `compile-error`（利用者コードのコンパイルエラー） |
| `EXPECT_MESSAGE` | エラーの文言の一部（あれば） |
| `OPTIONS` | 元のテストが使ったオプションの組。複数行可。無ければ `Arb<Options>` に任せる |
| `TAGS` | 自動タグで拾えない次元の値（`dimensions.toml` の `tag_only` の値など） |
| `ORIGIN` | `own`、または `test:` / `real:` + `<owner/repo>@<コミット>/<パス>` |
| `STATUS` | `candidate`（未確認）/ `verified`（K で EXPECT どおり）/ `rejected`（K で EXPECT と違った。`NOTE` に理由） |

`FILE` の値は `<Gradle モジュール>/<ソースセット>/<パス>`、`<ソースセット>/<パス>`、ファイル名だけ、のどれか。モジュールを省くと単一モジュール、ソースセットを省くと `main`。

## よく使うコマンド

```sh
python3 quality/catalog/scripts/catalog.py check              # 形式・ID・必須の網羅・生成物が最新か（CI）
python3 quality/catalog/scripts/catalog.py index              # index.json・coverage.md・THIRD_PARTY.md を作り直す
python3 quality/catalog/scripts/catalog.py import-all ~/.cache/acg-catalog   # 固定コミットを取得して ①② を取り込み直す
```

③ を足すときは、`coverage.md` の「薄いところ」や ⚠️ の値を見て、その値を含む最小のパターンを書き、`index` を実行してコミットする。
