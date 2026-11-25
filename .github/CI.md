# CI と定期実行

決定の理由は ADR-026（共有フォルダ `pm/adr/ADR-026-ci-and-schedules.md`）。ここには「何がいつ回るか」と、人が GitHub で設定することだけを書く。

## 何がいつ回るか

| ワークフロー | いつ | 中身 | 知らせ方 |
|---|---|---|---|
| `ci.yml` | PR・main への push | 軽い検査（script）、`./gradlew build`（test・ktlint を含む）を Kotlin 2 版で | PR のチェック。必須は `ci-ok` だけ |
| `nightly.yml` | 毎日 04:00 JST | 全 Kotlin 版の build、macOS、保留中ケースの実行 | issue `nightly-failure` / `unexpected-pass` |
| `kotlin-upstream.yml` | 毎日 08:00 JST | Maven Central の Kotlin と `gradle/kotlin-versions.txt` の差 | issue `kotlin-upstream` |
| `docs.yml` | docs などの PR・main への push | サイトのビルド、Dokka、内部リンク検査、Pages へのデプロイ | PR のチェック |
| `pbt-hourly.yml` | 毎時 | PBT × E2E（ACG-40 が追加。詳細は ADR-022） | ACG-40 の取り決め |
| `lic-01.yml` ほか | PR・push | 出所記録・版一覧・Processor 一覧の検査 | PR のチェック |

issue は同じラベルで 1 つだけ開き、続けて落ちたらコメントを足す。回復したら自動で閉じる。毎朝 09:00 の振り返りはこの issue を見る。

## テストに渡す値

`-P` で渡すと、テスト JVM に同じ名前の system property で届く（`build-logic/src/main/kotlin/acg.jvm.gradle.kts`）。

| property | 既定 | 意味 |
|---|---|---|
| `acg.kotlin` | 一覧の最新 stable | テスト対象の Kotlin 版。CI の matrix がこれを切り替える |
| `acg.runPending` | `false` | `true` で保留中のケースも実行し、通ったら `build/acg/unexpected-pass/*.txt` に `ID<TAB>テスト名` を書く（ACG-37） |

## テストを弱める変更の検出

`scripts/ci/test_guard.py` が PR の差分を見て、次で CI を落とす。

- 保留でなかったケースを保留に戻した（例外なし）
- 保留（PendingCase）以外の方法でテストを無効にした（`@Ignore`、`enabled = false`、`xtest`、`"!名前"` など）
- テストからケース ID が消えた。ADR を書いたうえで、PR 本文に `Case-Removal-ADR: ADR-<番号>` を書けば警告だけになる

## 検査を足すとき

script で済む検査は `ci.yml` の `checks` job に step を足す。Gradle のタスクなら `build` に依存させれば matrix の両方で回る。必須チェックは `ci-ok` が全 job をまとめるので、保護設定は変えなくてよい。

## 人が GitHub で設定すること

Agent の環境からはリポジトリ設定を変えられないため、てべすてん が一度だけ行う。

1. **ブランチ保護**: Settings → Branches（または Rules → Rulesets）で `main` に「Require status checks to pass」を付け、`ci-ok` を必須にする。「Require branches to be up to date」は任意
2. **GitHub Pages**: Settings → Pages → Build and deployment の Source を「GitHub Actions」にする。有効になるまで `docs.yml` はデプロイを飛ばして警告だけ出す
3. **Actions の権限**: Settings → Actions → General → Workflow permissions は既定（read）のままでよい。issue を書く job は自分で `issues: write` を宣言している
