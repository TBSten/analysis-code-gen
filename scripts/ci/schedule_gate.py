#!/usr/bin/env python3
"""schedule で起きた実行を、直前に代わりの起動（workflow_dispatch）が済んでいれば飛ばす（ADR-059）。

GitHub の schedule が不安定なため、Claude の定期タスクが workflow_dispatch で代わりに起こす。
schedule が遅れて後から起きたときに同じ中身を 2 度流さないよう、各ワークフローの最初の job でこれを呼ぶ。

  schedule_gate.py --workflow nightly.yml --hours 12   # GITHUB_OUTPUT に skip=true|false を書く

schedule 以外の event（PR・push・dispatch）では常に skip=false。cancelled の実行は「済んだ」に数えない。
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime, timedelta, timezone


def parse_time(text: str) -> datetime:
    return datetime.fromisoformat(text.replace("Z", "+00:00"))


def should_skip(runs: list[dict], current_id: int, now: datetime, hours: float) -> dict | None:
    """[hours] 時間以内に作られた、自分以外の cancelled でない schedule / dispatch の実行を返す（無ければ None）。"""
    since = now - timedelta(hours=hours)
    for r in runs:
        if r.get("id") == current_id:
            continue
        if r.get("event") not in ("schedule", "workflow_dispatch"):
            continue
        if r.get("conclusion") == "cancelled":
            continue
        if parse_time(r["created_at"]) >= since:
            return r
    return None


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--workflow", required=True, help="ワークフローのファイル名（例: nightly.yml）")
    p.add_argument("--hours", type=float, required=True)
    args = p.parse_args(argv)

    skip = False
    if os.environ.get("GITHUB_EVENT_NAME") == "schedule":
        repo = os.environ["GITHUB_REPOSITORY"]
        out = subprocess.run(
            ["gh", "api", f"repos/{repo}/actions/workflows/{args.workflow}/runs?per_page=30"],
            check=True, capture_output=True, text=True,
        ).stdout
        hit = should_skip(json.loads(out)["workflow_runs"], int(os.environ["GITHUB_RUN_ID"]), datetime.now(timezone.utc), args.hours)
        if hit:
            skip = True
            print(f"{args.hours} 時間以内に実行済み（{hit['event']}、{hit['created_at']}、{hit.get('html_url')}）。この schedule は飛ばす")
    print(f"skip={str(skip).lower()}")
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as f:
            f.write(f"skip={str(skip).lower()}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
