"""Resource limits and process supervision, independent of PyTorch and datasets."""

import argparse
import math
import os
import signal
import subprocess
from pathlib import Path

from dataset import MODULE_DIR


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Validate prepared data by default; --run explicitly starts one bounded training run")
    parser.add_argument("--run", action="store_true", help="Actually train; omitted means data/config checks only")
    parser.add_argument("--root", type=Path, default=MODULE_DIR / "data/raw")
    parser.add_argument("--prepared", type=Path, default=MODULE_DIR / "data/prepared")
    parser.add_argument("--output", type=Path, default=MODULE_DIR / "artifacts/baseline")
    parser.add_argument("--epochs", type=int, choices=range(1, 101), default=10)
    parser.add_argument("--patience", type=int, choices=range(1, 21), default=3,
                        help="Stop after this many epochs without validation-loss improvement")
    parser.add_argument("--batch-size", type=int, choices=range(1, 65), default=64)
    parser.add_argument("--workers", type=int, choices=range(0, 3), default=2)
    parser.add_argument("--threads", type=int, choices=range(1, 3), default=2)
    parser.add_argument("--max-minutes", type=int, choices=range(1, 31), default=30)
    parser.add_argument("--device", choices=["auto", "cuda", "mps", "cpu"], default="auto")
    parser.add_argument("--seed", type=int, default=42)
    return parser.parse_args(argv)


class EarlyStopping:
    def __init__(self, patience=3):
        self.patience = patience
        self.best_loss = math.inf
        self.bad_epochs = 0

    def update(self, loss):
        if not math.isfinite(loss):
            raise ValueError("Validation loss is not finite")
        if loss < self.best_loss:
            self.best_loss, self.bad_epochs = loss, 0
            return True
        self.bad_epochs += 1
        return False

    @property
    def should_stop(self):
        return self.bad_epochs >= self.patience


def run_with_budget(command, seconds, env=None):
    if seconds <= 0:
        raise ValueError("Time budget must be positive")
    process = subprocess.Popen(command, env=env, start_new_session=True)
    try:
        return process.wait(timeout=seconds)
    except subprocess.TimeoutExpired:
        return 124
    finally:
        if process.poll() is None:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                pass
        # A completed leader does not imply that its data-loader children exited.
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.wait()
