# テスト用 Processor（TP-*）

E2E・PBT・三者差分で繰り返し使う、振る舞いが分かっている Processor。決定は ADR-027。

| モジュール | 中身 |
|---|---|
| `core` | 解析結果のモデルと生成するコード（AA にも KSP にも依存しない） |
| `ksp` | KSP 版。三者差分の K 側と、フィクスチャを KSP2 で確かめるのに使う |
| `native` | Native API 版（本命）。AA に依存するのでクラウドの Agent 環境ではビルドできず、CI で確かめる |

両方の版は解析だけが違い、生成は `core` を共有する。同じ入力なら生成物は 1 文字も違わない（TP-Body を除く）。

## 選び方とオプション

1 つの JAR に全部入っていて、`tp.processors` で選ぶ（カンマ区切り。省略時は TP-Bad と TP-Probe 以外）。

| オプション | 意味 |
|---|---|
| `tp.processors` | `Mapper,Registry,...`。`all` は TP-Bad 以外の全部 |
| `tp.registry.package` / `tp.registry.name` | TP-Registry の出力先（既定 `me.tbsten.analysis.code.gen.tp.generated.TpRegistry`） |
| `tp.bad.modes` | TP-Bad の壊れ方（下表） |
| `tp.probe.out` | TP-Probe が JSON Lines を追記するファイル |
| `tp.probe.log` | TP-Probe がラウンド 1 で出すログ。`warn:文;error:文` |

## 9 つの Processor

アノテーションは成果物にせず、フィクスチャのソースに `TpAnnotations.kt` を置く（正は `core` の `TpAnnotationsSource`）。

| ID | 対象 | 生成物 | 主なケース |
|---|---|---|---|
| TP-Mapper | `@Mapped` クラス | `<Class>Mapper`（isolating） | INC-03 |
| TP-Registry | `@Registered` クラス | `TpRegistry`（aggregating、ラウンド 1 だけ、0 件なら作らない） | KMP-13・INC-04 |
| TP-Chain | `@Chain(depth)` | `<Origin>Chain<n>` を 1 ラウンド 1 段 | RND-01・02 |
| TP-Actual | `@GenerateActual expect fun f(): String` | `actual fun f() = "<platform>"` | KMP-03 |
| TP-Body | `@CallGraph` 関数 | `<Name>CallGraph`（呼び出し先の FQN）。Native 版だけ | ANA-01・INC-07 |
| TP-Defer | `@DeferUntil("FQN")` | 解決できるまで defer、できたら `<Class>Resolved` | DEF-01・05 |
| TP-Bad | `@BadTarget` など | 下表 | TRM・FAIL・API-04・06 |
| TP-Resource | `@GenerateJava` | Java・リソース・`META-INF/services`・次のラウンドに Kotlin | API-05・RND-06 |
| TP-Probe | なし | 見えたファイル集合・ラウンド・ライフサイクルを記録 | API-01〜12・RND-03・04 |

TP-Bad のモード（`tp.bad.modes`）: `infinite-rounds`・`stuck-defer`・`stuck-defer-progress`・`collide`（TP-Mapper と同じパス）・`duplicate`・`throw`・`write-outside`・`log-error`。

## 型とプラットフォームの表記

型は `kotlin.collections.List<kotlin.String>?` の形（typealias は展開、関数型も `kotlin.Function1<A, R>`）。プラットフォームは `common`・`jvm`・`js`・`wasm`・`native`。
