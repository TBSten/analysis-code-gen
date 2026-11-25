# PBT 基盤（実装者向け）

決定の理由は ADR-022（共有フォルダ `pm/adr/ADR-022-pbt-foundation.md`）。コードは `pbt/`、全体の設計は `quality/pbt-differential-design.md`。

## 性質を書く

`acgCheckAll` に名前（ケース ID を先頭に）・Arb・本体を渡す。seed・回帰・試行数はこの関数が決める。

```kotlin
class FooTest : PbtSpec({
    case("PBT-05", "ランダムな Processor 構成で必ず停止する") {
        // ⭐️ kotest の checkAll ではなく acgCheckAll を使う
        acgCheckAll("PBT-05 必ず停止する", Arb.processorSet()) { set ->
            run(set).terminated shouldBe true
        }
    }
})
```

PR では保存した回帰 seed と、名前から決まる固定 seed で `prIterations` 回だけ回る。毎時の定期実行（`pbt-hourly.yml`）では新しい seed で、持ち分の時間に収まるまで回る。

## 使える Arb

| Arb | 中身 |
|---|---|
| `Arb.projectSpec()` | ランダムな Kotlin / Java プロジェクト。必ずコンパイルできる |
| `Arb.processorSet()` / `Exhaustive.processorSets()` | Processor の組（36 通りの網羅層） |
| `Arb.options(set)` / `Pairwise.options(set)` | オプション（全ペア網羅） |
| `Arb.userProject(catalog, set)` / `catalog.shard(n)` | カタログから組み立てた入力 |
| `Arb.changeHistory()` | 増分ビルドの変更の列 |
| `Arb.kmpHierarchy()` | KMP の SourceSet の木 |

生成の仕方を変えたら `SOURCE_GENERATOR_VERSION` などの版を上げる。保存した seed が別の値を作るようになるため。

## 反例が出たら

例外のメッセージに再現コマンドが出る。直す前に、示された 1 行を `src/test/resources/pbt/regressions/<性質>.tsv` に足す。

```sh
./gradlew test -Pacg.pbt.only='PBT-01 生成したプロジェクトはコンパイルできる' -Pacg.pbt.seed=123 -Pacg.pbt.iterations=40
```

足した行は PR ごとに再生されるので、直るまで CI が落ちる。毎時の反例は issue（ラベル `pbt-counterexample`）に届く。
