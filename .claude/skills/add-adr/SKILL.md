---
name: add-adr
description: 設計・仕様の決め打ちを ADR として記録する手順。人間に聞かずに細部を決めたとき、既存の ADR を覆すとき、「ADR を書く」「決定を記録」と言われたときに使う。
---

# ADR を足す

書式とルールの正は `.claude/rules/adr.md`。共有フォルダはクラウドでは `/mnt/project-files/`（末尾に `/` を付けて参照し、フォルダ全体を検索しない）。

1. **番号を確保する**: `pm/p1-plan.md` に割り当てがあればそれ。無ければ
   ```bash
   ls /mnt/project-files/pm/adr/            # 使われている番号を見る
   printf '# ADR-0NN: （執筆中）\n' > /mnt/project-files/pm/adr/ADR-0NN-<slug>.md
   sleep 10 && ls /mnt/project-files/pm/adr/ | grep ADR-0NN   # 重複していたら別の番号へ
   ```
2. **本文を書く**: Context / Decision / Alternatives / Consequences。決定は実装者が守ることが分かる箇条書きで
3. **索引に 1 行足す**: `pm/28-adr.md` を**編集の直前に読み直し**、索引表の先頭行に `| ADR-0NN | <決定を 1 文> | 採用 | [adr/...](./adr/...) |` を足す。10 秒後に読み直して自分の行が残っているか確かめる
4. **覆す場合**: 旧 ADR の状態を `廃止（後継: ADR-0NN）` に変え、索引の状態列も直す
5. **実装者が毎回守ることがあれば** `docs/design/` の該当メモに短く足す（リポジトリ側、PR に含める）
6. **チケットに結論とリンクを書く**（背景情報のトグル）
