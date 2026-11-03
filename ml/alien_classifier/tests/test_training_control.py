import sys
import time
import os
import signal

import pytest

from training_control import EarlyStopping, parse_args, run_with_budget


def test_training_requires_an_explicit_run_flag():
    assert parse_args([]).run is False
    assert parse_args(["--run"]).run is True


@pytest.mark.parametrize("args", [
    ["--epochs", "101"], ["--epochs", "0"], ["--workers", "3"],
    ["--threads", "3"], ["--batch-size", "65"], ["--max-minutes", "31"],
    ["--patience", "0"], ["--patience", "21"],
])
def test_resource_limits_reject_accidental_large_runs(args):
    with pytest.raises(SystemExit):
        parse_args(args)


def test_longer_training_accepts_more_epochs_and_patience_with_the_same_time_budget():
    args = parse_args(["--epochs", "100", "--patience", "10"])
    assert args.epochs == 100
    assert args.patience == 10
    assert args.max_minutes == 30


def test_early_stopping_keeps_the_best_validation_loss():
    stopping = EarlyStopping(patience=3)
    assert stopping.update(1.0) is True
    assert stopping.update(0.8) is True
    assert stopping.update(0.9) is False
    assert stopping.update(0.8) is False
    assert stopping.should_stop is False
    assert stopping.update(0.85) is False
    assert stopping.should_stop is True
    assert stopping.best_loss == 0.8


def test_time_budget_terminates_a_real_process_without_training(tmp_path):
    started = time.monotonic()
    result = run_with_budget([sys.executable, "-c", "import time; time.sleep(60)"], 0.1)
    assert result == 124
    assert time.monotonic() - started < 5


def test_successful_process_preserves_exit_status():
    assert run_with_budget([sys.executable, "-c", "raise SystemExit(7)"], 5) == 7


def test_timeout_kills_a_descendant_even_when_the_group_leader_exits(tmp_path):
    pid_file = tmp_path / "child.pid"
    child = "import signal,time; signal.signal(signal.SIGTERM, signal.SIG_IGN); time.sleep(60)"
    parent = ("import subprocess,sys,time; from pathlib import Path; "
              f"p=subprocess.Popen([sys.executable,'-c',{child!r}]); "
              f"Path({str(pid_file)!r}).write_text(str(p.pid)); time.sleep(60)")
    try:
        assert run_with_budget([sys.executable, "-c", parent], 0.5) == 124
        pid = int(pid_file.read_text())
        time.sleep(0.1)
        with pytest.raises(ProcessLookupError):
            os.kill(pid, 0)
    finally:
        if pid_file.exists():
            try:
                os.kill(int(pid_file.read_text()), signal.SIGKILL)
            except ProcessLookupError:
                pass
