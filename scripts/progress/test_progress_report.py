#!/usr/bin/env python3
"""progress_report.py の自己テスト。python3 scripts/progress/test_progress_report.py で動く。"""

from __future__ import annotations

import datetime as dt
import json
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import progress_report as pr  # noqa: E402

NOW = dt.datetime(2026, 11, 26, 9, 7, tzinfo=pr.JST)


def run(run_id: str, cases: dict[str, str], hours_ago: float = 1, workflow: str = "ci", conclusion: str = "success", event: str = "push") -> dict:
    t = (NOW - dt.timedelta(hours=hours_ago)).astimezone(dt.timezone.utc)
    return {"workflow": workflow, "runId": run_id, "sha": "x", "event": event, "conclusion": conclusion, "finishedAt": t.isoformat(), "cases": cases}


def case(cid: str, ms: str = "M1", status: str = "enabled", kind: str = "E") -> pr.Case:
    return pr.Case(cid, ms, kind, status)


class CollectTest(unittest.TestCase):
    def test_junit_status_per_case(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            (d / "a").mkdir()
            (d / "a" / "TEST-a.xml").write_text(textwrap.dedent("""\
                <testsuite>
                  <testcase name="[GRD-01] 登録される" />
                  <testcase name="[GRD-02] 出力先"><failure message="x"/></testcase>
                  <testcase name="[GRD-03] 循環なし"><skipped/></testcase>
                  <testcase name="[RW-CRM-B1] cream" />
                  <testcase name="[INC-14a] 枝番" />
                  <testcase name="補助テスト" />
                </testsuite>"""), encoding="utf-8")
            # 別の Kotlin 版（matrix）では GRD-01 が落ちた → 1 つでも落ちたら fail
            (d / "b.xml").write_text('<testsuite><testcase name="[GRD-01] 登録される"><error/></testcase>'
                                     '<testcase name="[RW-CRM-B1] cream"><skipped/></testcase></testsuite>', encoding="utf-8")
            (d / "broken.xml").write_text("<testsuite>", encoding="utf-8")
            got = pr.collect_junit([d])
        self.assertEqual(got, {"GRD-01": "fail", "GRD-02": "fail", "GRD-03": "skip", "INC-14a": "pass", "RW-CRM-B1": "pass"})

    def test_collect_command_writes_record(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            (d / "r.xml").write_text('<testsuite><testcase name="[API-01] x"/></testsuite>', encoding="utf-8")
            out = d / "o" / "run.json"
            pr.main(["collect", "--results", str(d), "--workflow", "ci", "--run-id", "9", "--sha", "abc", "--event", "push",
                     "--conclusion", "success", "--finished-at", "2026-11-26T00:00:00Z", "--out", str(out)])
            rec = json.loads(out.read_text(encoding="utf-8"))
        self.assertEqual(rec["cases"], {"API-01": "pass"})
        self.assertEqual(rec["runId"], "9")


class StateTest(unittest.TestCase):
    def test_pass_only_when_last_three_runs_pass(self) -> None:
        cases = [case("A-01"), case("A-02"), case("A-03"), case("A-04", status="pending"), case("A-05", status="missing"), case("A-06"), case("A-07")]
        runs = [
            run("1", {"A-01": "fail", "A-02": "pass"}, hours_ago=4),  # 4 回前は窓の外
            run("2", {"A-01": "pass", "A-02": "pass", "A-03": "fail"}, hours_ago=3),
            run("3", {"A-01": "pass", "A-02": "fail", "A-03": "fail"}, hours_ago=2),
            run("4", {"A-01": "pass", "A-02": "pass", "A-03": "fail", "A-04": "pass", "A-07": "pass"}, hours_ago=1),  # A-07 は有効にした直後
        ]
        got = pr.case_states(cases, runs)
        self.assertEqual(got, {"A-01": "passing", "A-02": "flaky", "A-03": "failing", "A-04": "pending", "A-05": "missing", "A-06": "unconfirmed", "A-07": "unconfirmed"})

    def test_no_runs_means_nothing_passes(self) -> None:
        self.assertEqual(pr.case_states([case("A-01")], []), {"A-01": "unconfirmed"})


class CorrectnessTest(unittest.TestCase):
    def score(self, m1: tuple[int, int], m2: tuple[int, int] = (0, 0), m3: tuple[int, int] = (0, 0), pbt: bool = False, bench: int = 0) -> float:
        cases, state = [], {}
        for ms, (ok, total) in (("M1", m1), ("M2", m2), ("M3", m3)):
            for i in range(total):
                c = case(f"{ms}-{i}", ms)
                cases.append(c)
                state[c.id] = "passing" if i < ok else "failing"
        for i in range(bench):  # 落ちているベンチマークは正しさに響かない
            c = case(f"B-{i}", "M1", kind="B")
            cases.append(c)
            state[c.id] = "failing"
        return round(pr.correctness(cases, state, pbt), 2)

    def test_table(self) -> None:
        self.assertEqual(self.score((0, 4)), 1.0)  # 有効だが M1 が 50% 未満
        self.assertEqual(self.score((3, 4)), 2.0)  # 50% → 1.0、100% → 3.0 を補う
        self.assertEqual(self.score((4, 4), (1, 2)), 4.0)  # 仕様の例: M1 100%・M2 50% で 4.0
        self.assertEqual(self.score((4, 4), (2, 2), (9, 20)), 6.5)
        self.assertEqual(self.score((4, 4), (2, 2), (19, 20)), 8.5)
        self.assertEqual(self.score((4, 4), (2, 2), (20, 20)), 9.0)  # PBT が 24 時間緑でない
        self.assertEqual(self.score((4, 4), (2, 2), (20, 20), pbt=True, bench=3), 10.0)

    def test_nothing_enabled_is_zero(self) -> None:
        self.assertEqual(pr.correctness([case("A-01", status="pending")], {"A-01": "pending"}, True), 0.0)

    def test_performance(self) -> None:
        self.assertEqual(pr.performance(2.9, [0.5]), 0.0)
        self.assertEqual(pr.performance(5, []), 0.0)
        self.assertEqual(pr.performance(5, [1.0, 1.0, 1.0]), 4.0)
        self.assertEqual(pr.performance(5, [0.5] * 10), 10.0)
        self.assertAlmostEqual(pr.performance(5, [0.5] * 9 + [1.2]), 8.0)


class DeliverableTest(unittest.TestCase):
    def test_every_inventory_case_has_one_deliverable(self) -> None:
        cases = pr.read_cases(pr.REPO / "quality/test-cases.csv", pr.REPO / "quality/case-status.csv")
        self.assertGreater(len(cases), 200)
        missing = [c.id for c in cases if pr.deliverable_of(c.id) is None]
        self.assertEqual(missing, [], "DELIVERABLES に区分を足す")

    def test_bridge_and_rewrite_split(self) -> None:
        self.assertEqual(pr.deliverable_of("RW-CRM-B1"), "既存 Processor 移植 (Bridge)")
        self.assertEqual(pr.deliverable_of("RW-CRM-N2"), "既存 Processor 移植 (Rewrite)")
        self.assertEqual(pr.deliverable_of("RW-CMB-02"), "既存 Processor 移植 (Rewrite)")
        self.assertEqual(pr.deliverable_of("RW-ALL-P1"), "既存 Processor 移植 (Bridge)")
        self.assertEqual(pr.deliverable_of("BINC-01"), "KSP Bridge")
        self.assertEqual(pr.deliverable_of("BIN-01"), "KSP Bridge")
        self.assertEqual(pr.deliverable_of("INC-14a"), "解析・コード生成エンジン")


class ReportTest(unittest.TestCase):
    PREV = {
        "updated": "11/26 07:07",
        "projectStart": "2026-11-24",
        "milestones": [{"id": "M1", "name": "最小限動く", "passed": 0, "total": 1}],
        "history": {"labels": ["11/26 07"], "series": [{"name": "x", "unit": "件", "goal": 1, "values": [2]}]},
        "threads": [{"ticket": "ACG-30", "hours": 1.5}],
        "phases": [{"id": "P1"}],
        "cases": {"passing": ["API-01", "API-02"], "flaky": [], "failing": ["API-03"]},
        "streak": {"passedFlat": 2},
    }

    def test_report(self) -> None:
        cases = [case("API-01"), case("API-02"), case("API-03", status="pending"), case("API-04"), case("API-05", "任意")]
        runs = [run(str(i), {"API-01": "pass", "API-02": "fail", "API-04": "pass"}, hours_ago=i) for i in (1, 2, 3)]
        runs.append(run("pr", {"API-02": "pass"}, event="pull_request"))  # PR の実行は数えない
        runs += [run(f"p{i}", {}, hours_ago=i, workflow="pbt") for i in range(22)]
        runs.append(run("old", {}, hours_ago=30, workflow="pbt", conclusion="failure"))  # 24 時間より前
        out = pr.build_report(self.PREV, cases, runs, NOW, None, [])

        self.assertEqual(out["updated"], "11/26 09:07")
        self.assertEqual(out["previous"], "11/26 07:07")
        self.assertEqual(out["todayOffset"], 2.38)
        self.assertEqual(out["phases"], [{"id": "P1"}])  # フェーズなどの枠は前回から引き継ぐ
        self.assertEqual(out["milestones"][0]["passed"], 2)
        self.assertEqual(out["milestones"][0]["total"], 4)  # 任意は数えない
        c = out["cases"]
        self.assertEqual(c["passing"], ["API-01", "API-04"])
        self.assertEqual(c["failing"], ["API-02"])
        self.assertEqual(c["pending"], 1)
        self.assertEqual(c["ciRuns"], ["3", "2", "1"])
        self.assertEqual(c["newlyPassing"], ["API-04"])
        self.assertEqual(c["noLongerPassing"], ["API-02"])
        self.assertEqual(c["backToPending"], ["API-03"])
        self.assertTrue(out["pbt"]["green24h"])
        self.assertEqual(out["streak"], {"passedFlat": 3})  # 通過数 2 → 2 で横ばい
        api = next(d for d in out["deliverables"] if d["name"] == "Processor 向け API")
        self.assertEqual((api["passed"], api["total"]), (2, 4))
        self.assertEqual(api["c"], 1.0)  # M1 50%
        self.assertEqual(out["history"]["labels"], ["11/26 07", "09"])
        self.assertEqual(out["history"]["series"][0]["values"], [2, 2])
        self.assertEqual(out["history"]["series"][0]["name"], "通過したケース（全 4）")
        self.assertEqual(out["history"]["series"][3]["values"], [None, 1.5])
        self.assertNotIn("sample", {k for k, v in out.items() if v is True})

    def test_schedule_is_never_changed(self) -> None:
        # マイルストーンの予定（薄い帯）は人の指示なしに変えない（てべすてん、2026-11-26）
        prev = {**self.PREV, "goOffset": 7,
                "milestones": [{"id": "M1", "name": "最小限動く", "guide": 3, "p50": 4.5, "p80": 5.5, "start": 0, "passed": 0, "total": 1}],
                "phases": [{"id": "P1", "name": "検証", "start": 4, "end": 6, "state": "次"}]}
        out = pr.build_report(prev, [case("API-01")], [], NOW, None, [])
        keys = ("id", "name", "guide", "p50", "p80", "start")
        self.assertEqual([{k: m[k] for k in keys} for m in out["milestones"]], [{k: m[k] for k in keys} for m in prev["milestones"]])
        self.assertEqual(out["phases"], prev["phases"])
        self.assertEqual((out["goOffset"], out["projectStart"]), (7, "2026-11-24"))

    def test_report_command_from_runs_dir(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            (d / "runs" / "ci").mkdir(parents=True)
            (d / "runs" / "ci" / "1.json").write_text(json.dumps(run("1", {"GRD-01": "pass"})), encoding="utf-8")
            prev = d / "auto.json"
            prev.write_text(json.dumps(self.PREV), encoding="utf-8")
            out = d / "out.json"
            pr.main(["report", "--prev", str(prev), "--out", str(out), "--runs-dir", str(d / "runs"), "--now", "2026-11-26T09:07"])
            got = json.loads(out.read_text(encoding="utf-8"))
        self.assertEqual(got["generatedBy"], "scripts/progress/progress_report.py")
        self.assertEqual(len(got["deliverables"]), len(pr.DELIVERABLES))
        self.assertEqual(got["cases"]["total"], got["history"]["series"][0]["goal"])


if __name__ == "__main__":
    unittest.main()
