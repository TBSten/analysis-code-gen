# scripts/progress — 進捗レポートのコマンド

進捗レポート（`pm/progress-report-format.md`）の機械的な部分 `auto.json` を作る。決めた理由は ADR-032。

## 流れ

1. main の CI・夜間・毎時 PBT が終わると、`.github/workflows/progress-data.yml` が JUnit XML を `collect` でケースごとの結果にまとめ、`progress-data` branch の `runs/<ci|nightly|pbt>/` に積む
2. 2 時間ごとの定期実行が、main を checkout した場所で `report` を呼んで `auto.json` を書く
3. サポート Agent が `notes.json` を書き、進捗 Artifact は 2 つの JSON だけを読んで描く

## 定期実行で打つコマンド

```bash
git fetch origin main progress-data && git checkout --detach origin/main
python3 scripts/progress/progress_report.py report \
  --prev /mnt/project-files/pm/progress/auto.json \
  --out  /mnt/project-files/pm/progress/auto.json \
  --threads /tmp/threads.json        # 任意。無ければ前回の値を引き継ぐ
```

`--threads` は `[{"ticket":"ACG-30","url":"…","state":"作業中","hours":2.1}]`、`--bench` は `{"ratios":{"PERF-01":0.8}}`（KSP2 比）。

## 数え方（要点）

| 状態 | 条件 |
|---|---|
| passing | `case-status.csv` で enabled、かつ main の直近 3 回の CI すべてで pass |
| unconfirmed | fail は無いが 3 回の pass がそろっていない（有効にした直後・まだ走っていない）。通過に数えない |
| flaky | pass と fail が混ざる。通過に数えない |
| failing | 直近 3 回に fail があり pass が無い |
| pending / missing | `case-status.csv` のまま |

- 任意（M 列が `任意`）のケースは合格ラインの外なので数えない。ベンチマーク（種別 B）は正しさに数えない
- 成果物とケースの対応は `progress_report.py` の `DELIVERABLES`。一覧に区分が増えたらそこに足す（自己テストが漏れを落とす）
- 数字がおかしいときは `auto.json` を手で直さず、このコマンドを直す

## 自己テスト

```bash
python3 scripts/progress/test_progress_report.py
```
