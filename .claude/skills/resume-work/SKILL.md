---
name: resume-work
description: 止まった作業を続きから再開する手順。スレッドやセッションを新しく始めたとき、クールタイム明け、「続きから」「再開して」と言われたときに使う。
---

# 作業を再開する

ルールの正は `.claude/rules/resume-and-handoff.md`。この順で現在地を拾う。

1. **担当チケットを特定する**: スレッド名の `ACG-n`、または指示に書かれたチケット。Notion で開き、ステータス・Next 列・`## 次の一手`・`## 判断待ち` / `## 保留` 節を読む
2. **成果の現在地を見る**
   ```bash
   git fetch origin
   git log --oneline origin/main -10
   git branch -r | grep claude/          # 同じチケットの WIP branch が残っていないか
   ```
   チケットの `## Pull request` に PR があれば状態（open / merged / CI）を確かめる
3. **テストケースの通過状況を見る**: `quality/test-cases.csv`（または共有フォルダ `quality/`）と main の CI。担当ケース ID のうち保留のものが残りの仕事
4. **ADR を読む**: チケットの背景情報にある ADR と、`pm/28-adr.md` の索引で関係しそうなもの
5. **着手する**: チケットを `AI:*` にし、Next 列に `~HH:MM 終了見込み` を書いてから手を動かす

## 迷ったとき

- 次の一手が曖昧・古い: 2〜4 の結果から自分で決め、チケットに書き直してから進める
- 他スレッドの作業と重なりそう: `pm/p1-plan.md` の波と担当を見て、依存先のチケットの状態を確かめる
