#!/usr/bin/env python3
"""定期実行の実行履歴（ADR-058）。

定期実行（毎時 PBT・Nightly など）が終わるたびに 1 行の JSON を `history/runs.jsonl`（progress-data branch）に追記し、
「24 時間で何回動いたか・何回落ちたか・どれくらい遅れたか」を数値で確かめられるようにする。

使い方:
  # 実行 1 回分を追記する（run-history.yml が呼ぶ。run / jobs は GitHub API の JSON）
  run_history.py record --run run.json --jobs jobs.json [--metrics run-metrics.json] --out history/runs.jsonl
  # 毎時 PBT の数値（試行数など）を run-metrics.json にする（pbt-hourly.yml が呼ぶ）
  run_history.py pbt-metrics --before <前の timing> --after <後の timing> --results <dir> \
      --kotlin 2.4.21 --shard 7 --out run-metrics.json
  # 直近 N 時間のまとめ（期待回数との比較）を表で出す
  run_history.py summary --history history/runs.jsonl [--hours 24] [--now 2026-11-28T05:00:00Z]
"""

from __future__ import annotations

import argparse
import json
import statistics
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from pathlib import Path

# 1 日あたりに期待する回数（ADR-026 の予定）。summary が「足りているか」を見る基準
EXPECTED_PER_DAY = {
    "PBT hourly": 24.0,
    "Nightly": 1.0,
    "kspt": 1.0,
    "Kotlin upstream": 1.0,
    "fixtures": 1.0 / 7,
}


def parse_time(text: str | None) -> datetime | None:
    if not text:
        return None
    return datetime.fromisoformat(text.replace("Z", "+00:00"))


def seconds_between(a: str | None, b: str | None) -> int | None:
    ta, tb = parse_time(a), parse_time(b)
    if ta is None or tb is None:
        return None
    return int((tb - ta).total_seconds())


def make_record(run: dict, jobs: list[dict], metrics: dict | None) -> dict:
    """GitHub API の run と jobs から履歴の 1 行を作る。"""
    job_conclusions: dict[str, int] = {}
    for j in jobs:
        key = j.get("conclusion") or j.get("status") or "unknown"
        job_conclusions[key] = job_conclusions.get(key, 0) + 1
    # 打ち切られた・失敗した job の名前（cancelled の原因を追うため）
    bad_jobs = sorted(j.get("name", "") for j in jobs if j.get("conclusion") not in (None, "success", "skipped"))
    started = run.get("run_started_at") or run.get("created_at")
    # schedule_gate（ADR-059）が中身を飛ばした実行: gate 以外の job がすべて skipped
    others = [j for j in jobs if j.get("name") != "gate"]
    gated = bool(others) and all(j.get("conclusion") == "skipped" for j in others)
    return {
        "gated": gated,
        "workflow": run.get("name"),
        "run_id": run.get("id"),
        "run_number": run.get("run_number"),
        "attempt": run.get("run_attempt", 1),
        "event": run.get("event"),
        "conclusion": run.get("conclusion"),
        "created_at": run.get("created_at"),
        "started_at": started,
        "finished_at": run.get("updated_at"),
        "queue_s": seconds_between(run.get("created_at"), started),
        "duration_s": seconds_between(started, run.get("updated_at")),
        "head_sha": (run.get("head_sha") or "")[:12],
        "jobs": job_conclusions,
        "bad_jobs": bad_jobs,
        "metrics": metrics or {},
        "url": run.get("html_url"),
    }


def cmd_record(args: argparse.Namespace) -> int:
    run = json.loads(Path(args.run).read_text(encoding="utf-8"))
    jobs_doc = json.loads(Path(args.jobs).read_text(encoding="utf-8")) if args.jobs else {}
    jobs = jobs_doc.get("jobs", jobs_doc if isinstance(jobs_doc, list) else [])
    metrics = None
    if args.metrics and Path(args.metrics).is_file():
        metrics = json.loads(Path(args.metrics).read_text(encoding="utf-8"))
    record = make_record(run, jobs, metrics)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    # 同じ run（と再実行の回）を 2 度積まない
    if out.is_file():
        for line in out.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            old = json.loads(line)
            if old.get("run_id") == record["run_id"] and old.get("attempt") == record["attempt"]:
                print(f"記録済み: {record['run_id']}")
                return 0
    with out.open("a", encoding="utf-8") as f:
        f.write(json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n")
    print(json.dumps(record, ensure_ascii=False))
    return 0


def read_timing(d: Path) -> dict[str, int]:
    """TimingStore（pbt/run/Stores.kt）の `<名前>\\t1 試行の ns\\t累計試行数\\t...` から累計試行数を読む。"""
    samples: dict[str, int] = {}
    for f in sorted(d.glob("*.tsv")) if d.is_dir() else []:
        cols = f.read_text(encoding="utf-8").strip().split("\t")
        if len(cols) >= 3 and cols[2].isdigit():
            samples[cols[0]] = int(cols[2])
    return samples


def junit_counts(root: Path) -> dict:
    tests = failures = skipped = 0
    seconds = 0.0
    for f in sorted(root.rglob("*.xml")) if root.is_dir() else []:
        try:
            suite = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        if suite.tag != "testsuite":
            continue
        tests += int(suite.get("tests", 0))
        failures += int(suite.get("failures", 0)) + int(suite.get("errors", 0))
        skipped += int(suite.get("skipped", 0))
        seconds += float(suite.get("time", 0) or 0)
    return {"tests": tests, "failures": failures, "skipped": skipped, "test_s": round(seconds)}


def cmd_pbt_metrics(args: argparse.Namespace) -> int:
    before, after = read_timing(Path(args.before)), read_timing(Path(args.after))
    trials = {k: v - before.get(k, 0) for k, v in after.items() if v - before.get(k, 0) > 0}
    metrics = {
        "kotlin": args.kotlin,
        "shard": args.shard,
        "properties": len(trials),
        "trials": sum(trials.values()),
        "trials_by_property": dict(sorted(trials.items())),
        "counterexample": bool(args.counterexample),
        **junit_counts(Path(args.results)),
    }
    Path(args.out).write_text(json.dumps(metrics, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(metrics, ensure_ascii=False))
    return 0


def summarize(records: list[dict], now: datetime, hours: int) -> list[dict]:
    since = now - timedelta(hours=hours)
    rows = []
    names = sorted({r.get("workflow") for r in records if r.get("workflow")} | set(EXPECTED_PER_DAY))
    for name in names:
        window = [r for r in records if r.get("workflow") == name and (parse_time(r.get("created_at")) or now) >= since]
        rs = [r for r in window if not r.get("gated")]
        durations = [r["duration_s"] for r in rs if isinstance(r.get("duration_s"), int)]
        events: dict[str, int] = {}
        conclusions: dict[str, int] = {}
        for r in rs:
            events[r.get("event") or "?"] = events.get(r.get("event") or "?", 0) + 1
            conclusions[r.get("conclusion") or "?"] = conclusions.get(r.get("conclusion") or "?", 0) + 1
        expected = EXPECTED_PER_DAY.get(name)
        rows.append(
            {
                "workflow": name,
                "runs": len(rs),
                "gated": len(window) - len(rs),
                "expected": round(expected * hours / 24, 1) if expected is not None else None,
                "success": conclusions.get("success", 0),
                "conclusions": conclusions,
                "events": events,
                "median_duration_s": int(statistics.median(durations)) if durations else None,
                "trials": sum(int(r.get("metrics", {}).get("trials", 0) or 0) for r in rs),
            }
        )
    return rows


def cmd_summary(args: argparse.Namespace) -> int:
    path = Path(args.history)
    records = [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()] if path.is_file() else []
    now = parse_time(args.now) if args.now else datetime.now(timezone.utc)
    rows = summarize(records, now, args.hours)
    if args.json:
        print(json.dumps(rows, ensure_ascii=False, indent=2))
        return 0
    print(f"直近 {args.hours} 時間（〜{now.strftime('%Y-%m-%dT%H:%MZ')}）")
    print("| ワークフロー | 回数 / 期待 | 成功 | 結果 | 起動 | 所要の中央値 | PBT 試行数 | 重複で飛ばした |")
    print("|---|---|---|---|---|---|---|---|")
    for r in rows:
        exp = "-" if r["expected"] is None else r["expected"]
        dur = "-" if r["median_duration_s"] is None else f"{r['median_duration_s'] // 60} 分"
        fmt = lambda d: " ".join(f"{k}:{v}" for k, v in sorted(d.items())) or "-"
        print(f"| {r['workflow']} | {r['runs']} / {exp} | {r['success']} | {fmt(r['conclusions'])} | {fmt(r['events'])} | {dur} | {r['trials'] or '-'} | {r['gated']} |")
    return 0


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    r = sub.add_parser("record", help="実行 1 回分を履歴に追記する")
    r.add_argument("--run", required=True)
    r.add_argument("--jobs")
    r.add_argument("--metrics")
    r.add_argument("--out", required=True)
    r.set_defaults(func=cmd_record)

    m = sub.add_parser("pbt-metrics", help="毎時 PBT の数値を run-metrics.json にする")
    m.add_argument("--before", required=True)
    m.add_argument("--after", required=True)
    m.add_argument("--results", required=True)
    m.add_argument("--kotlin", default="")
    m.add_argument("--shard", default="")
    m.add_argument("--counterexample", action="store_true")
    m.add_argument("--out", required=True)
    m.set_defaults(func=cmd_pbt_metrics)

    s = sub.add_parser("summary", help="直近 N 時間のまとめ")
    s.add_argument("--history", required=True)
    s.add_argument("--hours", type=int, default=24)
    s.add_argument("--now")
    s.add_argument("--json", action="store_true")
    s.set_defaults(func=cmd_summary)

    args = p.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
