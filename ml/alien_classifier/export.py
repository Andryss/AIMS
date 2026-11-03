"""Export a completed run's selected checkpoint and verify a portable CPU inference bundle."""

import argparse
import importlib.metadata
import json
import math
import platform
import shutil
import tempfile
import time
from pathlib import Path

import numpy as np
import onnx
import onnxruntime as ort
import torch
from PIL import Image, ImageOps

from dataset import load_catalog, sha256, write_json
from model import PREPROCESSING, WEIGHTS, build_model, image_transform
from prepare_dataset import inspect_image

ABSOLUTE_TOLERANCE = 2e-4
RELATIVE_TOLERANCE = 1e-4
MAX_REFERENCE_IMAGES = 8


def load_selected_model(run_dir):
    run_dir = Path(run_dir)
    run = json.loads((run_dir / "run.json").read_text(encoding="utf-8"))
    metrics = json.loads((run_dir / "metrics.json").read_text(encoding="utf-8"))
    if run["status"] != "completed":
        raise ValueError("Only completed training runs can be exported")
    checkpoint = torch.load(run_dir / "best.pt", map_location="cpu", weights_only=True)
    if (not checkpoint.get("complete") or checkpoint["epoch"] != run["bestEpoch"]
            or checkpoint["epoch"] != metrics["bestEpoch"]
            or checkpoint["featureCacheSignature"] != run["featureCacheSignature"]):
        raise ValueError("Selected checkpoint does not match the completed run")
    labels = json.loads((run_dir / "labels.json").read_text(encoding="utf-8"))
    expected = [(i, item["classificationKey"], item["datasetLabel"]) for i, item in enumerate(load_catalog())]
    actual = [(item["index"], item["classificationKey"], item["datasetLabel"]) for item in labels]
    if actual != expected or checkpoint["labels"] != labels:
        raise ValueError("Checkpoint class order does not match the catalog and labels.json")
    preprocessing = json.loads((run_dir / "preprocessing.json").read_text(encoding="utf-8"))
    if (preprocessing != PREPROCESSING or checkpoint["preprocessing"] != preprocessing
            or checkpoint["weights"] != str(WEIGHTS)):
        raise ValueError("Checkpoint preprocessing or pretrained architecture does not match")
    calibration = metrics.get("validationCalibration")
    threshold = calibration["threshold"] if calibration else None
    if threshold is not None and (isinstance(threshold, bool) or not isinstance(threshold, (int, float))
                                  or not math.isfinite(threshold) or not 0 <= threshold <= 1):
        raise ValueError("Invalid validation rejection threshold")
    model = build_model(len(labels), pretrained=False)
    model.load_state_dict(checkpoint["model"], strict=True)
    model.requires_grad_(False)
    if not all(torch.isfinite(value).all() for value in model.state_dict().values()):
        raise ValueError("Checkpoint contains non-finite values")
    return model.eval(), labels, checkpoint, metrics, threshold


def cpu_session(path):
    ort.disable_telemetry_events()
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    options.inter_op_num_threads = 1
    return ort.InferenceSession(str(path), sess_options=options, providers=["CPUExecutionProvider"])


def reference_images(image_paths, directory):
    directory.mkdir()
    paths = []
    for index, path in enumerate(image_paths, 1):
        path = Path(path)
        inspect_image(path)
        target = directory / f"source-{index:02d}{path.suffix.lower()}"
        shutil.copyfile(path, target)
        paths.append(target)
    with Image.open(paths[0]) as image:
        rgb = ImageOps.exif_transpose(image).convert("RGB")
        portrait = directory / "portrait.png"
        ImageOps.fit(rgb, (181, 301)).save(portrait)
        gray = directory / "grayscale.png"
        rgb.convert("L").save(gray)
        rgba = directory / "rgba.png"
        transparent = rgb.convert("RGBA")
        transparent.putalpha(128)
        transparent.save(rgba)
        oriented = directory / "oriented.jpg"
        exif = Image.Exif()
        exif[274] = 6
        rgb.save(oriented, exif=exif, quality=90)
        paths.extend([portrait, gray, rgba, oriented])
    return paths


def verify_outputs(model, session, values):
    with torch.inference_mode():
        expected = model(torch.from_numpy(values)).numpy()
    actual = session.run(["logits"], {"images": values})[0]
    if actual.shape != expected.shape or not np.isfinite(actual).all():
        raise ValueError("ONNX output shape or values differ from PyTorch")
    if not np.allclose(actual, expected, rtol=RELATIVE_TOLERANCE, atol=ABSOLUTE_TOLERANCE):
        raise ValueError(f"ONNX logits differ from PyTorch: max absolute error {np.max(np.abs(actual - expected))}")
    if not np.array_equal(actual.argmax(axis=1), expected.argmax(axis=1)):
        raise ValueError("ONNX selected different classes than PyTorch")
    return expected, float(np.max(np.abs(actual - expected)))


def verify_bundle(model, session, image_paths, output):
    paths = reference_images(image_paths, output / "references")
    cases, inputs, errors = [], [], []
    for path in paths:
        with Image.open(path) as image:
            tensor = image_transform()(ImageOps.exif_transpose(image).convert("RGB"))
        values = tensor.unsqueeze(0).numpy().astype("<f4", copy=False)
        expected, error = verify_outputs(model, session, values)
        inputs.append(values)
        errors.append(error)
        input_path = path.with_suffix(".input.f32")
        output_path = path.with_suffix(".logits.f32")
        values.tofile(input_path)
        expected.astype("<f4", copy=False).tofile(output_path)
        cases.append({"image": path.relative_to(output).as_posix(),
                      "input": input_path.relative_to(output).as_posix(),
                      "logits": output_path.relative_to(output).as_posix(),
                      "inputShape": [1, 3, 224, 224], "outputShape": [1, 50],
                      "top1Index": int(expected.argmax(axis=1)[0])})
    _, batch_error = verify_outputs(model, session, np.concatenate([inputs[0]] * 3, axis=0))
    errors.append(batch_error)
    write_json(output / "references/cases.json", {"dtype": "float32", "byteOrder": "little-endian",
               "layout": "NCHW", "absoluteTolerance": ABSOLUTE_TOLERANCE,
               "relativeTolerance": RELATIVE_TOLERANCE, "cases": cases})
    for _ in range(3):
        session.run(["logits"], {"images": inputs[0]})
    timings = []
    for _ in range(30):
        start = time.perf_counter()
        session.run(["logits"], {"images": inputs[0]})
        timings.append((time.perf_counter() - start) * 1000)
    return {"referenceCases": len(cases), "verifiedBatchSizes": [1, 3],
            "maxAbsoluteError": max(errors), "absoluteTolerance": ABSOLUTE_TOLERANCE,
            "relativeTolerance": RELATIVE_TOLERANCE,
            "cpuInference": {"samples": len(timings), "threads": 2, "batchSize": 1,
                             "platform": platform.platform(), "processor": platform.processor(),
                             "milliseconds": {f"p{p}": float(np.percentile(timings, p)) for p in [50, 90, 95, 99]}}}


def export_run(run_dir, output, image_paths):
    run_dir, output = Path(run_dir), Path(output)
    if output.exists():
        raise FileExistsError(f"Export destination already exists: {output}")
    if not 1 <= len(image_paths) <= MAX_REFERENCE_IMAGES:
        raise ValueError(f"Provide between 1 and {MAX_REFERENCE_IMAGES} verification images")
    torch.set_num_threads(2)
    model, labels, checkpoint, metrics, threshold = load_selected_model(run_dir)
    for path in image_paths:
        inspect_image(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    staging = Path(tempfile.mkdtemp(prefix=".onnx-export-", dir=output.parent))
    try:
        model_path = staging / "model.onnx"
        torch.onnx.export(model, (torch.zeros(2, 3, 224, 224),), str(model_path),
                          input_names=["images"], output_names=["logits"], opset_version=18,
                          dynamo=True, external_data=False,
                          dynamic_shapes=({0: torch.export.Dim("batch", min=1, max=64)},))
        onnx.checker.check_model(str(model_path))
        session = cpu_session(model_path)
        verification = verify_bundle(model, session, image_paths, staging)
        write_json(staging / "labels.json", labels)
        write_json(staging / "preprocessing.json", {**PREPROCESSING, "layout": "NCHW", "dtype": "float32",
                   "resizeRounding": "floor", "centerCropRounding": "ties-to-even",
                   "alpha": "discard", "grayscale": "repeat-to-RGB", "resizeImplementation": "Pillow BILINEAR with antialias"})
        graph = onnx.load(model_path)
        manifest = {"formatVersion": 1, "modelVersion": run_dir.name, "classCount": len(labels),
                    "bestEpoch": checkpoint["epoch"], "sourceCheckpointSha256": sha256(run_dir / "best.pt"),
                    "modelBytes": model_path.stat().st_size, "onnxIrVersion": graph.ir_version,
                    "opset": next(item.version for item in graph.opset_import if item.domain == ""),
                    "input": {"name": "images", "dtype": "float32", "shape": ["batch", 3, 224, 224]},
                    "output": {"name": "logits", "dtype": "float32", "shape": ["batch", len(labels)]},
                    "decision": {"type": "softmax-max", "threshold": threshold, "calibrationSplit": "validation",
                                 "unknownRejectionValidated": metrics.get("unknownRejectionValidated", False)},
                    "sceneIndependent": metrics.get("sceneIndependent", False), "verification": verification,
                    "versions": {name: importlib.metadata.version(name) for name in ["torch", "torchvision", "onnx", "onnxscript", "onnxruntime", "Pillow"]},
                    "files": {path.relative_to(staging).as_posix(): sha256(path)
                              for path in sorted(staging.rglob("*")) if path.is_file()}}
        write_json(staging / "model-manifest.json", manifest)
        staging.rename(output)
        return manifest
    finally:
        if staging.exists():
            shutil.rmtree(staging)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-dir", type=Path, required=True, help="Completed training run containing best.pt and metrics")
    parser.add_argument("--output", type=Path, help="New bundle directory; defaults to <run-dir>/onnx")
    parser.add_argument("--images", type=Path, nargs="+", required=True, help="Up to 8 real PNG/JPEG images for verification")
    args = parser.parse_args()
    output = args.output if args.output is not None else args.run_dir / "onnx"
    manifest = export_run(args.run_dir, output, args.images)
    print(json.dumps({"bundle": str(output), "modelBytes": manifest["modelBytes"],
                      "verification": manifest["verification"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
