# quality/

テストケース一覧と、テストとの整合をとるためのファイル置き場。2026-11-26 に共有フォルダから移した（ACG-37）。以後はここが正。

| ファイル | 中身 | 誰が書く |
|---|---|---|
| `test-case-inventory.md` | テストケース一覧（正）。合格ライン・判定方法・変え方の規則も 3 章にある | 人と Agent。変えたら `python3 quality/scripts/gen_csv.py` |
| `test-cases.csv` | 一覧から生成した表。手で直さない（CI が再生成して差分を見る） | `scripts/gen_csv.py` |
| `case-status.csv` | ケースごとの状態（enabled / pending / missing）。進捗レポートが読む | `./gradlew :quality:updateCaseStatus` |
| `case-exceptions.csv` | 有効にしたケースを保留に戻す・消すときの例外（ADR 必須） | 人の了承を得た Agent |
| `case-backlog.md` | 実装中に気づいたケースの候補置き場（数えない） | Agent |
| `pbt-differential-design.md` | PBT × 三者差分の設計 | Agent |

テストの書き方とチェックの中身は [`.claude/rules/test-cases.md`](../.claude/rules/test-cases.md)、決めた理由は ADR-030。
