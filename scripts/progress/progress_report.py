#!/usr/bin/env python3
"""進捗レポートの機械的な部分（auto.json）を作るコマンド（ACG-30、ADR-032）。

サブコマンド:
  collect  JUnit XML（CI の test-results）からケースごとの結果を集め、1 回分の実行記録 JSON を書く。
           CI（.github/workflows/progress-data.yml）が main の実行ごとに呼び、progress-data branch に積む
  report   テストケース一覧・ケースの状態・実行記録を集計し、auto.json を書く。定期実行（2 時間ごと）が呼ぶ

使い方（定期実行。リポジトリの main を checkout した場所で）:
  git fetch origin progress-data
  python3 scripts/progress/progress_report.py report \\
      --prev /mnt/project-files/pm/progress/auto.json \\
      --out  /mnt/project-files/pm/progress/auto.json \\
      [--threads threads.json] [--bench bench.json]

数字はこのコマンドだけが出す。おかしいと思ったら auto.json を手で直さず、このコマンドを直す。
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import io
import json
import re
import statistics
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

JST = dt.timezone(dt.timedelta(hours=9))
REPO = Path(__file__).resolve().parents[2]

# テスト名は "[ID] タイトル"（ADR-030）。ID の書式は CaseDeclaration.ID_PATTERN と同じ
CASE_NAME = re.compile(r"\[([A-Z]+(?:-[A-Z]+)*-[A-Z0-9]+[a-z]?)\]")

# 「直近 3 回の CI がすべて成功したものだけを通過とする」（pm/progress-report-format.md）
PASS_WINDOW = 3
# 定期 PBT が「24 時間続けて成功」とみなす最低回数（1 時間ごと。取りこぼしを 4 回まで許す）
PBT_MIN_RUNS_24H = 20
ENABLED = ("passing", "unconfirmed", "flaky", "failing")
# 進捗履歴に残す点の数（2 時間ごと × 約 3 日）
HISTORY_KEEP = 36

# 成果物 → ケース ID の対応（ADR-032）。上から順に最初に当たったものに入れる。
# 一覧にケースの区分が増えたら、ここに足す（自己テストが「どこにも入らないケース」を落とす）
DELIVERABLES: list[tuple[str, list[str]]] = [
    ("Processor 向け API", [r"API", r"DEF", r"LIF", r"HYB"]),
    ("解析・コード生成エンジン", [r"ANA", r"INC", r"RND", r"CCH", r"MOD", r"OUT", r"FAIL", r"TRM", r"DET", r"MEM", r"PERF"]),
    ("Gradle plugin", [r"GRD", r"KMP", r"GCC", r"CLI", r"VER"]),
    ("KSP Bridge", [r"BRG", r"BINC", r"BKMP", r"BIN", r"MIX", r"LIC"]),
    ("既存 Processor 移植 (Bridge)", [r"RW-[A-Z]+-B\d+", r"RW-CMB-0[13]", r"RW-ALL-[A-Z]\d+"]),
    ("既存 Processor 移植 (Rewrite)", [r"RW-[A-Z]+-N\d+", r"RW-CMB-02"]),
    ("E2E テストフレームワーク", [r"TST", r"PBT", r"XD"]),
    ("ドキュメントサイト", [r"DOC"]),
]


def deliverable_of(case_id: str) -> str | None:
    for name, patterns in DELIVERABLES:
        for p in patterns:
            # 区分だけの書き方（"API"）は "API-数字…" に当てる
            rx = p if "-" in p or "\\" in p else p + r"-[A-Z0-9]+[a-z]?"
            if re.fullmatch(rx, case_id):
                return name
    return None


# ---------------------------------------------------------------- collect


def collect_junit(dirs: list[Path]) -> dict[str, str]:
    """JUnit XML を読み、ケース ID → pass / fail / skip を返す。

    1 回の実行の中に同じケースが何度出ても（Kotlin の matrix・1 ケース複数テスト）、
    1 つでも失敗なら fail、失敗が無く 1 つでも通れば pass、全部 skip なら skip。
    """
    result: dict[str, str] = {}
    rank = {"skip": 0, "pass": 1, "fail": 2}
    for d in dirs:
        for f in sorted(d.rglob("*.xml")) if d.exists() else []:
            try:
                root = ET.parse(f).getroot()
            except ET.ParseError:
                continue
            for tc in root.iter("testcase"):
                m = CASE_NAME.search(tc.get("name", ""))
                if not m:
                    continue
                if tc.find("failure") is not None or tc.find("error") is not None:
                    st = "fail"
                elif tc.find("skipped") is not None:
                    st = "skip"
                else:
                    st = "pass"
                cid = m.group(1)
                if cid not in result or rank[st] > rank[result[cid]]:
                    result[cid] = st
    return dict(sorted(result.items()))


def cmd_collect(a: argparse.Namespace) -> int:
    cases = collect_junit([Path(p) for p in a.results])
    record = {
        "workflow": a.workflow,
        "runId": a.run_id,
        "sha": a.sha,
        "event": a.event,
        "conclusion": a.conclusion,
        "finishedAt": a.finished_at,
        "cases": cases,
    }
    Path(a.out).parent.mkdir(parents=True, exist_ok=True)
    Path(a.out).write_text(json.dumps(record, ensure_ascii=False, indent=0) + "\n", encoding="utf-8")
    counts = {k: sum(1 for v in cases.values() if v == k) for k in ("pass", "fail", "skip")}
    print(f"{a.out}: ケース {len(cases)} 件（{counts}）")
    return 0


# ---------------------------------------------------------------- report: 入力


@dataclass
class Case:
    id: str
    milestone: str  # M1 / M2 / M3 / 任意
    kind: str  # U / I / E / D / P / B
    status: str  # enabled / pending / missing（case-status.csv）


def read_cases(inventory: Path, status: Path) -> list[Case]:
    st = {r["id"]: r for r in csv.DictReader(io.StringIO(status.read_text(encoding="utf-8")))}
    out = []
    for r in csv.DictReader(io.StringIO(inventory.read_text(encoding="utf-8"))):
        m = re.search(r"M[123]", r["milestone"])
        out.append(Case(r["id"], m.group(0) if m else "任意", r["kind"].strip()[:1], st.get(r["id"], {}).get("status", "missing")))
    return out


def load_runs_from_dir(d: Path) -> list[dict]:
    return [json.loads(f.read_text(encoding="utf-8")) for f in sorted(d.rglob("*.json"))] if d.exists() else []


def load_runs_from_ref(ref: str, repo: Path) -> list[dict]:
    """progress-data branch（ref）の runs/ 以下の JSON を checkout せずに読む。"""
    ls = subprocess.run(["git", "-C", str(repo), "ls-tree", "-r", "--name-only", ref, "--", "runs"], capture_output=True, text=True)
    if ls.returncode != 0:
        print(f"警告: {ref} を読めない（git fetch origin progress-data を先に実行）。CI の結果なしで集計する", file=sys.stderr)
        return []
    runs = []
    for name in ls.stdout.split():
        if name.endswith(".json"):
            blob = subprocess.run(["git", "-C", str(repo), "show", f"{ref}:{name}"], capture_output=True, text=True, check=True)
            runs.append(json.loads(blob.stdout))
    return runs


def parse_time(s: str) -> dt.datetime:
    return dt.datetime.fromisoformat(s.replace("Z", "+00:00"))


# ---------------------------------------------------------------- report: 集計


def case_states(cases: list[Case], ci_runs: list[dict]) -> dict[str, str]:
    """ケース → passing / unconfirmed / flaky / failing / pending / missing。

    passing は「有効」かつ直近 PASS_WINDOW 回の main の CI すべてで pass。
    pass と fail が混ざれば flaky、fail だけなら failing。fail が無いのに 3 回そろわない（有効にした
    直後で、まだ CI で走っていない・前の実行にそのケースが無い）なら unconfirmed。passing 以外は通過に数えない。
    """
    recent = sorted(ci_runs, key=lambda r: r["finishedAt"])[-PASS_WINDOW:]
    out = {}
    for c in cases:
        if c.status != "enabled":
            out[c.id] = c.status
            continue
        res = [r["cases"].get(c.id) for r in recent]
        if res and all(x == "pass" for x in res):
            out[c.id] = "passing"
        elif any(x == "fail" for x in res):
            out[c.id] = "flaky" if any(x == "pass" for x in res) else "failing"
        else:
            out[c.id] = "unconfirmed"
    return out


def pbt_summary(pbt_runs: list[dict], now: dt.datetime) -> dict:
    window = [r for r in pbt_runs if now - parse_time(r["finishedAt"]) <= dt.timedelta(hours=24)]
    failed = [r for r in window if r["conclusion"] != "success"]
    return {
        "runs24h": len(window),
        "failed24h": len(failed),
        "failedRuns": [r["runId"] for r in failed],
        "green24h": len(window) >= PBT_MIN_RUNS_24H and not failed,
    }


def correctness(cs: list[Case], state: dict[str, str], pbt_green: bool) -> float:
    """品質レベルの「正しさ」（0〜10）。pm/progress-report-format.md の表を線形に補う（ADR-032）。

    ベンチマーク（種別 B）は性能の側で見るので、正しさには数えない。
    """
    cs = [c for c in cs if c.kind != "B" and c.milestone != "任意"]
    if not any(state[c.id] in ENABLED for c in cs):
        return 0.0

    def rate(ms: str) -> float:
        xs = [c for c in cs if c.milestone == ms]
        return sum(state[c.id] == "passing" for c in xs) / len(xs) if xs else 1.0

    r1, r2, r3 = rate("M1"), rate("M2"), rate("M3")
    if r1 < 1:
        return 1.0 if r1 < 0.5 else 1 + 2 * (r1 - 0.5) / 0.5
    if r2 < 1:
        return 3 + 2 * r2
    if r3 < 0.9:
        return 5 + 3 * r3 / 0.9
    if r3 < 1:
        return 8 + (r3 - 0.9) / 0.1  # 8.0〜9.0
    return 10.0 if pbt_green else 9.0  # 全ケース通過でも定期 PBT が 24 時間緑でなければ 9.0 で止める


def performance(c: float, ratios: list[float]) -> float:
    """品質レベルの「性能」（0〜10）。補い方は仮（ベンチマーク基盤の ADR で決め直す）。"""
    if c < 3 or not ratios:
        return 0.0
    faster = sum(r < 1 for r in ratios) / len(ratios)
    if faster == 1:
        return 10.0
    if faster >= 0.9:
        return 8 + 2 * (faster - 0.9) / 0.1
    return max(0.0, min(8.0, 4 + (1 - statistics.median(ratios)) * 8))


# ---------------------------------------------------------------- report: 出力


def fmt_now(t: dt.datetime) -> str:
    t = t.astimezone(JST)
    return f"{t.month}/{t.day} {t:%H:%M}"


def build_report(
    prev: dict,
    cases: list[Case],
    runs: list[dict],
    now: dt.datetime,
    threads: list[dict] | None,
    ratios: list[float],
) -> dict:
    ci_runs = [r for r in runs if r.get("workflow") == "ci" and r.get("event") in ("push", "workflow_dispatch")]
    pbt_runs = [r for r in runs if r.get("workflow") == "pbt"]
    state = case_states(cases, ci_runs)
    pbt = pbt_summary(pbt_runs, now)
    counted = [c for c in cases if c.milestone != "任意"]  # 任意のケースは合格ラインの外
    passed = {c.id for c in counted if state[c.id] == "passing"}

    # マイルストーンの予定（start / guide / p50 / p80）・phases・goOffset・projectStart は人が決める枠。
    # てべすてん の指示なしに変えない（2026-11-26）。コマンドは前回の値をそのまま引き継ぎ、通過数だけを書く
    milestones = []
    for m in prev.get("milestones", []):
        mine = [c for c in counted if c.milestone == m["id"]]
        milestones.append({**m, "total": len(mine), "passed": sum(c.id in passed for c in mine)})

    deliverables = []
    for name, _ in DELIVERABLES:
        cs = [c for c in cases if deliverable_of(c.id) == name]
        cv = round(correctness(cs, state, pbt["green24h"]), 1)
        mine = [c for c in cs if c.milestone != "任意"]
        deliverables.append({
            "name": name,
            "c": cv,
            "p": round(performance(cv, ratios), 1),
            "passed": sum(c.id in passed for c in mine),
            "total": len(mine),
        })

    def ids(s: str) -> list[str]:
        return sorted(c.id for c in counted if state[c.id] == s)

    prev_cases = prev.get("cases", {})
    prev_enabled = {i for k in ENABLED for i in prev_cases.get(k, [])}
    prev_passing = set(prev_cases.get("passing", []))
    enabled_now = {c.id for c in counted if state[c.id] in ENABLED}
    case_block = {
        "total": len(counted),
        "passing": ids("passing"),
        "unconfirmed": ids("unconfirmed"),
        "flaky": ids("flaky"),
        "failing": ids("failing"),
        "pending": len(ids("pending")),
        "missing": len(ids("missing")),
        "ciRuns": [r["runId"] for r in sorted(ci_runs, key=lambda r: r["finishedAt"])[-PASS_WINDOW:]],
        # 前回からの変化（「前回からの差分」の材料。節としては出さない）
        "newlyPassing": sorted(passed - prev_passing),
        "noLongerPassing": sorted(prev_passing - passed),
        "backToPending": sorted(prev_enabled - enabled_now),
    }

    threads = threads if threads is not None else prev.get("threads", [])
    hours = round(sum(t.get("hours", 0) for t in threads), 1)
    avg_c = round(sum(d["c"] for d in deliverables) / len(deliverables), 1) if deliverables else 0.0
    median = round(statistics.median(ratios), 2) if ratios else None

    hist = json.loads(json.dumps(prev.get("history") or {"labels": [], "series": []}))
    names = [f"通過したケース（全 {len(counted)}）", "正しさの平均（/10）", "KSP2 比（中央値、低いほど速い）", "稼働時間の累計"]
    units, goals = ["件", "", "×", "h"], [len(counted), 10, 1, None]
    while len(hist["series"]) < 4:
        i = len(hist["series"])
        hist["series"].append({"name": names[i], "unit": units[i], "goal": goals[i], "values": [None] * len(hist["labels"])})
    hist["series"][0].update(name=names[0], goal=len(counted))
    last_day = next((lbl.split(" ")[0] for lbl in reversed(hist["labels"]) if "/" in lbl), "")
    t = now.astimezone(JST)
    day = f"{t.month}/{t.day}"
    hist["labels"].append(f"{t:%H}" if day == last_day else f"{day} {t:%H}")
    for s, v in zip(hist["series"], [len(passed), avg_c, median, hours]):
        s["values"].append(v)
    hist["labels"] = hist["labels"][-HISTORY_KEEP:]
    for s in hist["series"]:
        s["values"] = s["values"][-HISTORY_KEEP:]

    prev_passed = (prev.get("history") or {}).get("series", [{}])[0].get("values", [None])[-1:] or [None]
    flat = prev.get("streak", {}).get("passedFlat", 0) + 1 if prev_passed[0] == len(passed) else 0

    out = dict(prev)
    out.pop("sample", None)
    start = prev.get("projectStart")
    if start:
        d0 = dt.datetime.fromisoformat(start).replace(tzinfo=JST)
        out["todayOffset"] = round((now - d0).total_seconds() / 86400, 2)
    out.update(
        sample=False,
        previous=prev.get("updated"),
        updated=fmt_now(now),
        milestones=milestones,
        deliverables=deliverables,
        history=hist,
        threads=threads,
        cases=case_block,
        pbt=pbt,
        streak={"passedFlat": flat},
        generatedBy="scripts/progress/progress_report.py",
    )
    return out


def cmd_report(a: argparse.Namespace) -> int:
    repo = Path(a.repo)
    prev = json.loads(Path(a.prev).read_text(encoding="utf-8")) if a.prev and Path(a.prev).exists() else {}
    cases = read_cases(repo / "quality/test-cases.csv", repo / "quality/case-status.csv")
    runs = load_runs_from_dir(Path(a.runs_dir)) if a.runs_dir else load_runs_from_ref(a.data_ref, repo)
    now = dt.datetime.fromisoformat(a.now) if a.now else dt.datetime.now(JST)
    if now.tzinfo is None:
        now = now.replace(tzinfo=JST)
    threads = json.loads(Path(a.threads).read_text(encoding="utf-8")) if a.threads else None
    ratios = list(json.loads(Path(a.bench).read_text(encoding="utf-8")).get("ratios", {}).values()) if a.bench else []
    out = build_report(prev, cases, runs, now, threads, ratios)
    Path(a.out).write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    c = out["cases"]
    print(
        f"{a.out}: 通過 {len(c['passing'])}/{c['total']}（確認中 {len(c['unconfirmed'])}・flaky {len(c['flaky'])}・失敗 {len(c['failing'])}・"
        f"保留 {c['pending']}・未作成 {c['missing']}）、CI {len(c['ciRuns'])} 回分、PBT 24h {out['pbt']['runs24h']} 回、"
        f"ベンチ {len(ratios)} 件"
    )
    return 0


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    c = sub.add_parser("collect", help="JUnit XML → 1 回分の実行記録 JSON")
    c.add_argument("--results", nargs="+", required=True, help="JUnit XML を含むディレクトリ（再帰で探す）")
    c.add_argument("--workflow", required=True, help="ci / pbt / nightly")
    c.add_argument("--run-id", required=True)
    c.add_argument("--sha", required=True)
    c.add_argument("--event", required=True)
    c.add_argument("--conclusion", required=True)
    c.add_argument("--finished-at", required=True, help="ISO 8601")
    c.add_argument("--out", required=True)
    c.set_defaults(func=cmd_collect)

    r = sub.add_parser("report", help="auto.json を書く")
    r.add_argument("--prev", help="前回の auto.json（マイルストーン・フェーズなどの枠と履歴を引き継ぐ）")
    r.add_argument("--out", required=True)
    r.add_argument("--repo", default=str(REPO), help="リポジトリのルート（quality/ を読む）")
    r.add_argument("--data-ref", default="origin/progress-data", help="実行記録を積んだ branch")
    r.add_argument("--runs-dir", help="data-ref の代わりに実行記録 JSON のディレクトリを読む")
    r.add_argument("--threads", help='[{"ticket":"ACG-30","url":"…","state":"作業中","hours":2.1}]')
    r.add_argument("--bench", help='{"ratios": {"PERF-01": 0.8}}（KSP2 比）')
    r.add_argument("--now", help="ISO 8601（既定は現在時刻、タイムゾーンなしは JST）")
    r.set_defaults(func=cmd_report)

    a = p.parse_args(argv)
    return a.func(a)


if __name__ == "__main__":
    sys.exit(main())
