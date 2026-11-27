#!/usr/bin/env python3
"""schedule_gate.py の自己テスト。python3 scripts/ci/test_schedule_gate.py"""
import sys
import unittest
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import schedule_gate as g  # noqa: E402

NOW = datetime(2026, 11, 28, 1, 0, tzinfo=timezone.utc)


def run(i, event, created, conclusion=None):
    return {"id": i, "event": event, "created_at": created, "conclusion": conclusion}


class GateTest(unittest.TestCase):
    def test_skips_when_dispatched_recently(self):
        runs = [run(9, "schedule", "2026-11-28T00:59:00Z"), run(8, "workflow_dispatch", "2026-11-28T00:40:00Z", "success")]
        self.assertEqual(g.should_skip(runs, 9, NOW, 0.5)["id"], 8)

    def test_runs_when_nothing_recent(self):
        runs = [run(9, "schedule", "2026-11-28T00:59:00Z"), run(8, "workflow_dispatch", "2026-11-27T23:00:00Z", "success")]
        self.assertIsNone(g.should_skip(runs, 9, NOW, 0.5))

    def test_ignores_cancelled_and_other_events(self):
        runs = [
            run(9, "schedule", "2026-11-28T00:59:00Z"),
            run(8, "workflow_dispatch", "2026-11-28T00:40:00Z", "cancelled"),
            run(7, "push", "2026-11-28T00:50:00Z", "success"),
            run(6, "pull_request", "2026-11-28T00:50:00Z", "success"),
        ]
        self.assertIsNone(g.should_skip(runs, 9, NOW, 0.5))

    def test_in_progress_counts(self):
        runs = [run(9, "schedule", "2026-11-28T00:59:00Z"), run(8, "workflow_dispatch", "2026-11-28T00:45:00Z", None)]
        self.assertEqual(g.should_skip(runs, 9, NOW, 0.5)["id"], 8)


if __name__ == "__main__":
    unittest.main()
