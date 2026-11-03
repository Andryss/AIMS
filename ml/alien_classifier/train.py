"""Check inputs by default; explicitly run one supervised, time-bounded MobileNet baseline."""

import csv
import importlib.metadata
import json
import os
import signal
import sys
import time
from pathlib import Path

from dataset import MODULE_DIR, json_digest, load_catalog, load_source_files, sha256, write_json
from evaluate import classification_metrics, rejection_metrics, select_threshold
from prepare_dataset import verify_prepared_dataset
from training_control import EarlyStopping, parse_args, run_with_budget


def check_inputs(args):
    catalog = load_catalog()
    source = load_source_files(MODULE_DIR, catalog)
    rows = verify_prepared_dataset(args.root, args.prepared, catalog, source)
    if {(row["split"], int(row["labelIndex"])) for row in rows} != {
            (split, index) for split in ("train", "validation", "test") for index in range(len(catalog))}:
        raise ValueError("Every prepared split must contain every catalog class")
    for row in rows:
        item = catalog[int(row["labelIndex"])]
        if row["classificationKey"] != item["classificationKey"] or row["datasetLabel"] != item["datasetLabel"]:
            raise ValueError("Manifest class ordering differs from catalog")
    return catalog, rows


def save_checkpoint(torch, path, value):
    temporary = path.with_suffix(".tmp")
    torch.save(value, temporary)
    temporary.replace(path)


def train_worker(args, progress):
    started = time.monotonic()
    catalog, rows = check_inputs(args)
    import numpy as np
    import torch
    from torch.utils.data import DataLoader
    from model import ImageRowsDataset, PREPROCESSING, WEIGHTS, build_model

    torch.set_num_threads(args.threads)
    torch.manual_seed(args.seed)
    np.random.seed(args.seed)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    device_name = args.device
    if device_name == "auto":
        device_name = "cuda" if torch.cuda.is_available() else "mps" if torch.backends.mps.is_available() else None
        if device_name is None:
            raise ValueError("No GPU found. Choose --device cpu explicitly to train on CPU")
    device = torch.device(device_name)
    torch.hub.set_dir(str(MODULE_DIR / "data/torch"))
    model = build_model(len(catalog)).to(device)
    head = model[1]
    state = {"phase": "initializing", "epoch": 0, "completedBatches": 0, "completedEpoch": False}
    deadline = started + args.max_minutes * 60
    training_deadline = deadline - min(180, args.max_minutes * 12)

    def check_deadline(training=False):
        if time.monotonic() >= (training_deadline if training else deadline):
            raise TimeoutError("Time budget exhausted; saved artifacts remain incomplete")

    labels = [{"index": index, **item} for index, item in enumerate(catalog)]
    write_json(args.output / "labels.json", labels)
    write_json(args.output / "preprocessing.json", PREPROCESSING)
    versions = {name: importlib.metadata.version(name) for name in ("torch", "torchvision", "numpy", "Pillow")}
    signature = json_digest({"manifest": sha256(args.prepared / "manifest.csv"), "catalog": json_digest(catalog),
                             "weights": str(WEIGHTS), "preprocessing": PREPROCESSING, "versions": versions,
                             "modelCodeSha256": sha256(MODULE_DIR / "model.py")})
    cache_dir = MODULE_DIR / "data/cache" / signature
    cache_dir.mkdir(parents=True, exist_ok=True)
    metadata = json.loads((args.output / "run.json").read_text())
    metadata.update({"versions": versions, "device": str(device), "weights": str(WEIGHTS),
                     "featureCacheSignature": signature, "sceneIndependent": False,
                     "augmentation": "none: frozen deterministic features for the first baseline"})
    write_json(args.output / "run.json", metadata)

    def features(split, training=False):
        state["phase"] = f"features_{split}"
        check_deadline(training)
        selected = [row for row in rows if row["split"] == split]
        cache = cache_dir / f"{split}.pt"
        if cache.exists():
            value = torch.load(cache, map_location="cpu", weights_only=True)
            if value["signature"] == signature and value["paths"] == [row["path"] for row in selected]:
                if value["features"].shape == (len(selected), model[1].in_features):
                    return value["features"], value["targets"], selected
            raise ValueError(f"Invalid feature cache: {cache}")
        loader = DataLoader(ImageRowsDataset(args.root, selected), batch_size=args.batch_size,
                            num_workers=args.workers, shuffle=False)
        extracted, targets = [], []
        model[0].eval()
        with torch.no_grad():
            for number, (images, target) in enumerate(loader, 1):
                check_deadline(training)
                extracted.append(model[0](images.to(device)).cpu())
                targets.append(target)
                if number % 25 == 0:
                    print(f"{split}: extracted {min(number * args.batch_size, len(selected))}/{len(selected)}", flush=True)
        value = {"signature": signature, "features": torch.cat(extracted), "targets": torch.cat(targets),
                 "paths": [row["path"] for row in selected]}
        save_checkpoint(torch, cache, value)
        return value["features"], value["targets"], selected

    optimizer = torch.optim.AdamW(head.parameters(), lr=0.001)

    def save_progress():
        save_checkpoint(torch, args.output / "last.pt", {
            "model": model.state_dict(), "optimizer": optimizer.state_dict(), "labels": labels,
            "weights": str(WEIGHTS), "preprocessing": PREPROCESSING,
            "featureCacheSignature": signature, "progress": dict(state), "complete": False,
        })

    progress["save"] = save_progress
    save_progress()
    train_x, train_y, _ = features("train", training=True)
    validation_x, validation_y, _ = features("validation", training=True)
    loss_function = torch.nn.CrossEntropyLoss()
    stopping = EarlyStopping(patience=args.patience)
    generator = torch.Generator().manual_seed(args.seed)

    def predict(x, y, training=False):
        head.eval()
        loss, result = 0.0, []
        with torch.no_grad():
            for offset in range(0, len(x), args.batch_size):
                check_deadline(training)
                logits = head(x[offset:offset + args.batch_size].to(device))
                target = y[offset:offset + args.batch_size].to(device)
                loss += loss_function(logits, target).item() * len(target)
                result.append(logits.softmax(dim=1).cpu())
        return loss / len(x), torch.cat(result).numpy()

    with (args.output / "history.csv").open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["epoch", "trainLoss", "validationLoss"])
        writer.writeheader()
        for epoch in range(args.epochs):
            check_deadline(training=True)
            state.update({"phase": "training", "epoch": epoch + 1, "completedBatches": 0, "completedEpoch": False})
            head.train()
            order = torch.randperm(len(train_x), generator=generator)
            train_loss = 0.0
            for offset in range(0, len(order), args.batch_size):
                check_deadline(training=True)
                indices = order[offset:offset + args.batch_size]
                logits = head(train_x[indices].to(device))
                loss = loss_function(logits, train_y[indices].to(device))
                optimizer.zero_grad(set_to_none=True)
                loss.backward()
                optimizer.step()
                state["completedBatches"] += 1
                train_loss += loss.item() * len(indices)
            state["phase"] = "validation"
            validation_loss, _ = predict(validation_x, validation_y, training=True)
            state["completedEpoch"] = True
            record = {"epoch": epoch + 1, "trainLoss": train_loss / len(train_x), "validationLoss": validation_loss}
            writer.writerow(record)
            stream.flush()
            checkpoint = {"model": model.state_dict(), "optimizer": optimizer.state_dict(), "epoch": epoch + 1,
                          "labels": labels, "weights": str(WEIGHTS), "preprocessing": PREPROCESSING,
                          "featureCacheSignature": signature, "validationLoss": validation_loss,
                          "progress": dict(state), "complete": True}
            save_checkpoint(torch, args.output / "last.pt", checkpoint)
            if stopping.update(validation_loss):
                save_checkpoint(torch, args.output / "best.pt", checkpoint)
            print(json.dumps(record), flush=True)
            if stopping.should_stop:
                break

    best = torch.load(args.output / "best.pt", map_location=device, weights_only=True)
    model.load_state_dict(best["model"])
    _, validation_scores = predict(validation_x, validation_y)
    calibration = select_threshold(validation_y.numpy(), validation_scores)
    test_x, test_y, test_rows = features("test")
    _, test_scores = predict(test_x, test_y)
    keys = [item["classificationKey"] for item in catalog]
    metrics = classification_metrics(test_y.numpy(), test_scores, keys)
    for mistake in metrics["mistakes"]:
        mistake["path"] = test_rows[mistake["index"]]["path"]
    metrics.update({"bestEpoch": best["epoch"], "sceneIndependent": False,
                    "validationCalibration": calibration, "unknownRejectionValidated": False,
                    "rejection": rejection_metrics(test_y.numpy(), test_scores, calibration["threshold"]) if calibration else None})
    write_json(args.output / "metrics.json", metrics)
    with (args.output / "confusion_matrix.csv").open("w", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(["expected/predicted", *keys])
        writer.writerows([key, *row] for key, row in zip(keys, metrics["confusionMatrix"]))
    write_json(args.output / "mistakes.json", metrics["mistakes"])
    write_json(args.output / "validation_metrics.json", classification_metrics(validation_y.numpy(), validation_scores, keys))
    metadata.update({"status": "completed", "elapsedSeconds": time.monotonic() - started, "bestEpoch": best["epoch"]})
    write_json(args.output / "run.json", metadata)
    print(json.dumps({key: metrics[key] for key in ("samples", "top1", "top3", "macroF1")}), flush=True)


def worker(args):
    progress = {}
    try:
        train_worker(args, progress)
    except (TimeoutError, KeyboardInterrupt) as error:
        if "save" in progress:
            progress["save"]()
        code = 124 if isinstance(error, TimeoutError) else 130
        metadata = json.loads((args.output / "run.json").read_text())
        metadata.update({"status": "timed_out" if code == 124 else "interrupted", "exitCode": code})
        write_json(args.output / "run.json", metadata)
        raise SystemExit(code) from error


def main():
    args = parse_args()
    if not args.run:
        catalog, rows = check_inputs(args)
        print(f"Inputs verified: {len(rows)} images, {len(catalog)} classes. No training started; use --run explicitly.")
        return
    if os.environ.get("AIMS_ALIEN_TRAIN_WORKER") == "1":
        def on_termination(signum, frame):
            raise TimeoutError("Supervisor stopped the run")
        signal.signal(signal.SIGTERM, on_termination)
        worker(args)
        return
    args.output.mkdir(parents=True, exist_ok=False)
    config = {key: str(value) if isinstance(value, Path) else value for key, value in vars(args).items()}
    write_json(args.output / "run.json", {"status": "running", "config": config, "python": sys.version,
                                        "trainingScriptSha256": sha256(Path(__file__))})
    env = {**os.environ, "AIMS_ALIEN_TRAIN_WORKER": "1", "CUBLAS_WORKSPACE_CONFIG": ":4096:8"}
    started = time.monotonic()
    try:
        code = run_with_budget([sys.executable, str(Path(__file__).resolve()), *sys.argv[1:]], args.max_minutes * 60, env)
    except KeyboardInterrupt:
        code = 130
    metadata = json.loads((args.output / "run.json").read_text())
    if code != 0:
        metadata.update({"status": "timed_out" if code == 124 else "interrupted" if code == 130 else "failed",
                         "elapsedSeconds": time.monotonic() - started, "exitCode": code})
        write_json(args.output / "run.json", metadata)
    raise SystemExit(code)


if __name__ == "__main__":
    main()
