# ケース ID 付きテストの書き方（ACG-37、ADR-030）

テストケース一覧（`quality/test-case-inventory.md`）の各ケースは、ID 付きのテストとして書く。進捗は `quality/case-status.csv` と CI の結果で数える。

## 書き方

```kotlin
class IncrementalAddFileTest : E2ESpec({
    // ⭐️ 未実装の間は pending を付ける（skip される）
    case("INC-01", "ファイルを 1 つ足すと、そのファイルの生成物だけが増える", pending = PendingCase(Milestone.M1)) {
        // 期待する結果（アサーション）まで書く
    }
})
```

- 基底クラスは種別で選ぶ: `UnitSpec`(U) / `IntegrationSpec`(I) / `E2ESpec`(E) / `DiffSpec`(D) / `PbtSpec`(P) / `BenchSpec`(B)。一覧の種別に含まれないとチェックが落ちる
- テストモジュールの `build.gradle.kts` に `id("acg.tests")` を付け、`quality/build.gradle.kts` の `acgCases` にそのモジュールを足す
- `case` は spec 直下にだけ書く（コンテナの中に書かない）。spec 本体で重い処理をしない。チェックはテストを実行せず spec を生成して宣言を集めるため
- テスト名は `[ID] タイトル`、kotest Tag は ID・`kind-E`・保留なら `pending` / `pending-M1` が付く
- 1 ケースに複数のテストを書いてよい。1 本でも保留ならケースは保留
- ID の無い補助テストは素の FreeSpec と同じく `"名前" { }` で書ける（進捗に数えない）

## 保留の決まり

- 理由は 2 種類だけ: `PendingCase(Milestone.M2)`（未実装）と `PendingCase(Milestone.M2, PendingReason.Adr(31))`（ADR 付き）
- マイルストーンは一覧の M 列の最初の値と同じにする（`M2 / M3` なら M2、`任意` は `Milestone.Optional`）
- **保留 → 有効の一方向だけ。** 有効にしたケースを保留に戻す・テストを消すと `checkCaseIds` が落ちる。仕様変更でどうしても必要なときだけ、ADR を書いててべすてんに報告し、`quality/case-exceptions.csv` に `id,adr,note` を足す
- `@Ignored`・`enabled = false`・`x` 接頭辞など PendingCase 以外の無効化は使わない（`checkCaseIds` と CI の test_guard が検出する）
- 保留を外す PR でアサーションを変えない

## コマンド

| やりたいこと | コマンド |
|---|---|
| 一覧を変えた | `python3 quality/scripts/gen_csv.py`（CI が再生成して差分を見る） |
| ケースを足した・保留を外した | `./gradlew :quality:updateCaseStatus` して `quality/case-status.csv` もコミット |
| 整合だけ見る | `./gradlew :quality:checkCaseIds`（`./gradlew build` にも含まれる） |
| 保留中のケースも実行する（夜間） | `./gradlew test -Pacg.runPending=true` → `build/acg/unexpected-pass/` に予想外に通ったもの |

`checkCaseIds` が落とすもの: 一覧に無い ID・種別違い・マイルストーン違い・有効→保留や消失・`case-status.csv` の更新忘れ・生成できない spec。テストの無いケースも落とす（`acg.caseCheck.requireAllCases=true`、`gradle.properties`。2026-11-26 から）。一覧にケースを足すときは、同じ PR で保留テストも足す。
