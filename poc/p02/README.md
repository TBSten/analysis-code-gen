# PoC P-02: ラウンド間で AA の解析環境を維持する

ACG-32。問い（pm/24 の P-02）: Standalone Analysis API（AA）の環境をラウンド間で維持し、生成物の追加だけを差分で反映して正しく再解析できるか。結論は ADR-010。**このコードは本番に流用しない。**

## 何を比べるか

| 方式 | ラウンド間でやること |
|---|---|
| KEEP（維持） | 生成物を VFS から読み、content scope・宣言 / パッケージ provider に足す。`publishGlobalSourceModuleStateModificationEvent` と `dropPsiCaches` でソースのキャッシュだけ捨てる（KSP2 と同じ手順） |
| REBUILD（再構築） | 環境を捨て、生成物ディレクトリを source root に足して作り直す |
| NO_INVALIDATE（対照） | KEEP からキャッシュ無効化だけを抜く。結果が変われば、検証が無効化の有無に敏感だと分かる |

入力は 200 ファイルのユーザーコードで、まだ無い生成物を参照している。擬似 Processor が 2 ラウンドかけて生成し、3 ラウンド目で止まる（`Scenario.kt`）。生成物には「新しいパッケージ」「既存パッケージへの追加」「既存クラスの上位型」「拡張関数」を含む。

各ラウンドの解析結果を決まった順のテキストにして（`Dumper.kt`）、KEEP と REBUILD で一致するかを見る。時間は「ラウンド間の処理 + 再解析」の中央値を比べる。

## 動かし方

JDK 21 と python3 が要る。

```sh
cd poc/p02
gradle run --args="--files 200 --warmup 3 --iterations 10 --libs realistic"
```

| 引数 | 意味 |
|---|---|
| `--files` | ユーザーコードのファイル数 |
| `--libs` | `minimal`（stdlib + JDK）か `realistic`（よくあるアプリ相当の依存 50 前後の JAR） |
| `--keepalive` | `true`（既定）で IntelliJ Application をプロセス内で使い回す。REBUILD に有利な条件 |
| `--out` | 結果の出力先。`report-*.md` と `dumps/` |

## AA の入手方法（PoC だけの回避策）

クラウドの作業環境から JetBrains の AA リポジトリ（`redirector.kotlinlang.org` など）に届かないため、Maven Central にある KSP2 の `symbol-processing-aa-embeddable` を使う。この JAR は AA を `ksp.` 接頭辞で relocate しているのに Kotlin メタデータは元の名前のままなので、`scripts/unrelocate.py` で relocate を戻してから使う。`.kotlin_module` が無く top-level 関数を参照できないため、`analyze` などは reflection で呼ぶ。

`AaEnv.kt` は google/ksp `78f7882`（Apache License 2.0）の `KotlinSymbolProcessing.createAASession` を参考に JVM 向けに書き直したもの。`com.google.devtools.ksp.standalone.*` のクラスは JAR のものをそのまま使っている。
