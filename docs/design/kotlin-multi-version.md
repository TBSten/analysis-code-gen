# 複数 Kotlin バージョンの扱い（実装者向け）

決定の理由と代替案は ADR-011（共有フォルダ `pm/adr/ADR-011-kotlin-multi-version.md`）。ここには実装で守ることだけを書く。

## 全体の形

```mermaid
flowchart LR
    U["利用者の KGP 2.4.10"] --> G["analysis-code-gen-gradle-plugin"]
    G -->|"AA 一式を 2.4.10 で解決"| W["Worker（隔離 ClassLoader）"]
    W --> L["CompatLoader: 2.4.10 以下で最も近い実装"]
    L --> C["analysis-code-gen-aa-compat-k240"]
```

- 成果物は 1 つ。版は Kotlin から独立した `0.x.y`。対応範囲は docs の互換表に書く
- 実行する AA は利用者の Kotlin と同じ版。`analysisCodeGen { analysisApiVersion = "..." }` で上書きできる

## 版で形が変わるコードの置き場所

| 置き場所 | 何を書くか |
|---|---|
| `analysis-code-gen-aa-compat` | SPI `AaCompat` と `Factory(minVersion)`、`CompatLoader`、版の比較、小さなリフレクション shim |
| `analysis-code-gen-aa-compat-k<版>` | その版の AA に `compileOnly` で固定した実装。直前の版へ `by` で委譲し差分だけ上書き。`META-INF/services` に Factory を登録 |
| engine 本体 | 最新 stable の AA でコンパイル。版で形が変わる呼び出しを直接書かず `AaCompat` を通す |
| `ksp-bridge-compat` | Bridge 固有の差分（必要になってから作る） |

- モジュールを足すのは CI が落ちたときだけ。名前は `k240`・`k2420`・`k250_rc1` のように版をそのまま書く
- source set の切り替えは使わない
- 増分キャッシュのキーに「AA の版 + 選ばれた compat 実装」を入れる

## 対応版の一覧とテストの切り替え

- 一覧は `gradle/kotlin-versions.txt` だけ。検査と CI matrix は `python3 scripts/kotlin-versions.py check | matrix pr | matrix full | latest-stable`
- テストの版は `-Pacg.kotlin=<版>` 1 つで切り替える。engine テストの AA、TestKit の KGP、kct fork のコンパイラをこの版にそろえる。省略時は最新 stable
- PR の CI は baseline（2.4.0）と最新 stable、定期実行は一覧すべて

## 範囲外の Kotlin

| 利用者の Kotlin | Gradle plugin の振る舞い |
|---|---|
| 2.4.0 未満 | エラーで止め、互換表を案内 |
| 試した最新より新しい | 警告 1 回、最も近い compat 実装で続行 |
| 同じ版の AA が無い | `analysisApiVersion` の指定を促して止める |
