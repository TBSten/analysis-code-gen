#!/usr/bin/env python3
"""run_history.py の自己テスト。python3 scripts/ci/test_run_history.py"""
import json
import sys
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import run_history as h  # noqa: E402

RUN = {
    "id": 45925893682,
    "name": "Nightly",
    "run_number": 1,
    "run_attempt": 1,
    "event": "schedule",
    "conclusion": "cancelled",
    "created_at": "2026-11-27T02:01:36Z",
    "run_started_at": "2026-11-27T02:01:40Z",
    "updated_at": "2026-11-27T03:02:16Z",
    "head_sha": "af6e3be66b5b944ee372ec1279d6aba587fd3502",
    "html_url": "https://github.com/x/y/actions/runs/45925893682",
}
JOBS = {
    "jobs": [
        {"name": "plan", "conclusion": "success"},
        {"name": "pending", "conclusion": "cancelled"},
        {"name": "linux (Kotlin 2.4.0)", "conclusion": "success"},
    ]
}


class RecordTest(unittest.TestCase):
    def test_make_record(self):
        r = h.make_record(RUN, JOBS["jobs"], {"pending_timeout": 2})
        self.assertEqual(r["workflow"], "Nightly")
        self.assertEqual(r["event"], "schedule")
        self.assertEqual(r["queue_s"], 4)
        self.assertEqual(r["duration_s"], 3636)
        self.assertEqual(r["jobs"], {"success": 2, "cancelled": 1})
        self.assertEqual(r["bad_jobs"], ["pending"])
        self.assertEqual(r["metrics"], {"pending_timeout": 2})
        self.assertFalse(r["gated"])

    def test_gated_run(self):
        jobs = [{"name": "gate", "conclusion": "success"}, {"name": "pbt", "conclusion": "skipped"}]
        self.assertTrue(h.make_record(RUN, jobs, None)["gated"])

    def test_record_appends_once(self):
        with tempfile.TemporaryDirectory() as d:
            d = Path(d)
            (d / "run.json").write_text(json.dumps(RUN))
            (d / "jobs.json").write_text(json.dumps(JOBS))
            out = d / "history" / "runs.jsonl"
            args = ["record", "--run", str(d / "run.json"), "--jobs", str(d / "jobs.json"), "--out", str(out)]
            h.main(args)
            h.main(args)  # 同じ run は 2 度積まない
            lines = out.read_text().splitlines()
            self.assertEqual(len(lines), 1)
            self.assertEqual(json.loads(lines[0])["run_id"], RUN["id"])


class PbtMetricsTest(unittest.TestCase):
    def test_trials_are_delta_of_samples(self):
        with tempfile.TemporaryDirectory() as d:
            d = Path(d)
            (d / "before").mkdir()
            (d / "after").mkdir()
            (d / "before" / "A.tsv").write_text("PBT-01 a\t1000.0\t100\t1.0\t0\n")
            (d / "after" / "A.tsv").write_text("PBT-01 a\t900.0\t350\t1.0\t1\n")
            (d / "after" / "B.tsv").write_text("XD-11 b\t50.0\t40\t1.0\t1\n")
            (d / "after" / "C.tsv").write_text("PBT-02 c\t50.0\t7\t1.0\t1\n")
            (d / "before" / "C.tsv").write_text("PBT-02 c\t50.0\t7\t1.0\t0\n")  # 今回は回っていない
            res = d / "results"
            res.mkdir()
            (res / "TEST-x.xml").write_text('<testsuite tests="5" failures="1" errors="0" skipped="2" time="61.5"></testsuite>')
            out = d / "m.json"
            h.main(["pbt-metrics", "--before", str(d / "before"), "--after", str(d / "after"), "--results", str(res),
                    "--kotlin", "2.4.21", "--shard", "7", "--out", str(out)])
            m = json.loads(out.read_text())
            self.assertEqual(m["trials_by_property"], {"PBT-01 a": 250, "XD-11 b": 40})
            self.assertEqual(m["trials"], 290)
            self.assertEqual(m["properties"], 2)
            self.assertEqual((m["tests"], m["failures"], m["skipped"], m["test_s"]), (5, 1, 2, 62))


class SummaryTest(unittest.TestCase):
    def test_counts_against_expected(self):
        now = datetime(2026, 11, 28, 0, 0, tzinfo=timezone.utc)
        recs = [
            {"workflow": "PBT hourly", "created_at": "2026-11-27T10:17:00Z", "event": "schedule", "conclusion": "success",
             "duration_s": 1800, "metrics": {"trials": 100}},
            {"workflow": "PBT hourly", "created_at": "2026-11-27T11:40:00Z", "event": "workflow_dispatch", "conclusion": "failure",
             "duration_s": 2000, "metrics": {"trials": 50}},
            {"workflow": "PBT hourly", "created_at": "2026-11-25T11:40:00Z", "event": "schedule", "conclusion": "success"},
            {"workflow": "PBT hourly", "created_at": "2026-11-27T11:50:00Z", "event": "schedule", "conclusion": "success", "gated": True},
        ]
        rows = {r["workflow"]: r for r in h.summarize(recs, now, 24)}
        pbt = rows["PBT hourly"]
        self.assertEqual((pbt["runs"], pbt["expected"], pbt["success"], pbt["trials"]), (2, 24.0, 1, 150))
        self.assertEqual(pbt["events"], {"schedule": 1, "workflow_dispatch": 1})
        self.assertEqual(pbt["gated"], 1)
        self.assertEqual(pbt["median_duration_s"], 1900)
        self.assertEqual(rows["Nightly"]["runs"], 0)


if __name__ == "__main__":
    unittest.main()
