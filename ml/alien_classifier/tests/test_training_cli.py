import json
import os
import subprocess
import sys
from pathlib import Path

from PIL import Image

import train
import pytest
from dataset import load_catalog, sha256
from prepare_dataset import prepare_dataset
from training_control import parse_args


def test_input_checks_accept_full_class_order_without_loading_a_model(tmp_path, monkeypatch):
    catalog = load_catalog()
    root, prepared = tmp_path / "raw", tmp_path / "prepared"
    source = []
    for split_index, split in enumerate(["train", "validation", "test"]):
        for index, item in enumerate(catalog):
            path = f"{split}/{item['datasetLabel']}/image.png"
            target = root / path
            target.parent.mkdir(parents=True)
            Image.new("RGB", (12, 8), (index * 4, split_index * 80, 40)).save(target)
            source.append({"path": path, "fileId": str(len(source))})
    prepare_dataset(root, prepared, catalog, source)
    source_file = tmp_path / "source_files.json"
    source_file.write_text(json.dumps(source))
    (tmp_path / "sources.json").write_text(json.dumps({
        "sourceInventoryFile": "source_files.json", "sourceInventorySha256": sha256(source_file),
        "sourceFileCount": 150, "sourceSplitCounts": {"train": 50, "validation": 50, "test": 50},
    }))
    args = parse_args(["--root", str(root), "--prepared", str(prepared)])
    with pytest.raises(ValueError, match="inventory"):
        train.check_inputs(args)
    monkeypatch.setattr(train, "MODULE_DIR", tmp_path)
    checked_catalog, rows = train.check_inputs(args)
    assert len(checked_catalog) == 50
    assert len(rows) == 150
    assert not (tmp_path / "artifacts").exists()


def test_default_cli_fails_closed_on_missing_data_and_never_downloads_weights(tmp_path):
    root = Path(__file__).resolve().parents[1]
    output = tmp_path / "artifacts"
    weights = tmp_path / "torch-cache"
    result = subprocess.run([sys.executable, str(root / "train.py"), "--root", str(tmp_path / "raw"),
                             "--prepared", str(tmp_path / "prepared"), "--output", str(output)],
                            env={**os.environ, "TORCH_HOME": str(weights)}, capture_output=True, text=True)
    assert result.returncode != 0
    assert not output.exists()
    assert not weights.exists()


@pytest.mark.parametrize("error,exit_code,status", [(TimeoutError, 124, "timed_out"), (KeyboardInterrupt, 130, "interrupted")])
def test_cooperative_stop_preserves_first_epoch_progress_without_training(tmp_path, monkeypatch, error, exit_code, status):
    args = parse_args(["--output", str(tmp_path)])
    (tmp_path / "run.json").write_text('{"status":"running"}')

    def interrupted_work(args, progress):
        progress["save"] = lambda: (tmp_path / "last.pt").write_bytes(b"checkpoint: first epoch, three batches")
        raise error("stopped")

    monkeypatch.setattr(train, "train_worker", interrupted_work)
    with pytest.raises(SystemExit) as stopped:
        train.worker(args)
    assert stopped.value.code == exit_code
    assert (tmp_path / "last.pt").read_bytes() == b"checkpoint: first epoch, three batches"
    assert json.loads((tmp_path / "run.json").read_text())["status"] == status
