---
name: handoff
description: 作業を中断する前の引き継ぎ手順。クールタイム（03:00 JST）の前の 02:45、作業の区切り、長いビルド待ちで止まるとき、「引き継いで」「止める前に」と言われたときに使う。
---

# 止まる前に引き継ぐ

別の Agent が resume-work だけで続きを拾える状態にする。

1. **コードを push する**: 作業中でも自分の branch に commit して push（main には入れない）
   ```bash
   git add -A && git commit -m "wip: <何の途中か> [ACG-n]" && git push -u origin HEAD
   ```
2. **チケットの `## 次の一手` を書き直す**（Notion）。次の 3 つを必ず入れる
   - いまどこまで終わったか（commit / PR / 通ったケース ID）
   - 再開したら最初にやること（打つコマンド、開くファイル、確かめる CI）
   - 詰まっている点と、試してだめだったこと
3. **Next 列を更新する**: 作業中のまま止めるなら `~HH:MM 終了見込み`（再開後の見込み）。待ちに入るならステータスを `保留` / `人間:判断待ち` にして Next を空にする
4. **決めたことが ADR に残っているか確かめる**: 決め打ちしたのに ADR が無ければ先に書く（skill `add-adr`）

## 書き方の例

```markdown
## 次の一手
- 済み: GRD-01〜03 の保留テストを追加し push（branch claude/xxx、PR #12 は CI 待ち）
- 次: PR #12 の CI を見る。緑なら squash マージ → GRD-04 に着手（e2e-tests/src/test/.../GradleIntegrationTest.kt）
- 詰まり: AGP 9 の TestKit で KMP Android target の解決が遅い。--offline は効かなかった
```
