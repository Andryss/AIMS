"""Verify the complete source dataset, remove exact leakage and record a baseline manifest."""

import argparse
import csv
import hashlib
import json
import warnings
from collections import Counter, defaultdict
from pathlib import Path

from PIL import Image, ImageOps

from dataset import MODULE_DIR, SPLITS, json_digest, load_catalog, load_source_files, sha256, validate_source_files, write_json

FIELDS = ("path", "labelIndex", "classificationKey", "datasetLabel", "split", "sha256", "pixelSha256", "dhash", "width", "height")
MAX_PIXELS = 25_000_000


def inspect_image(path):
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(path) as image:
                if image.format not in {"PNG", "JPEG"} or image.width * image.height > MAX_PIXELS:
                    raise ValueError("Unsupported image format or dimensions")
                image.verify()
            with Image.open(path) as image:
                rgb = ImageOps.exif_transpose(image).convert("RGB")
                pixel_hash = hashlib.sha256(f"{rgb.size}:".encode() + rgb.tobytes()).hexdigest()
                gray = list(rgb.convert("L").resize((9, 8), Image.Resampling.LANCZOS).get_flattened_data())
                bits = sum(int(gray[y * 9 + x] > gray[y * 9 + x + 1]) << (y * 8 + x)
                           for y in range(8) for x in range(8))
                return pixel_hash, f"{bits:016x}", rgb.width, rgb.height
    except (OSError, ValueError, Image.DecompressionBombWarning, Image.DecompressionBombError) as error:
        raise ValueError(f"Invalid image {path}: {error}") from error


def near_duplicate_audit(rows):
    training = defaultdict(list)
    validation = defaultdict(list)
    for row in rows:
        if row["split"] == "train":
            training[row["datasetLabel"]].append((int(row["dhash"], 16), row["path"]))
        elif row["split"] == "validation":
            validation[row["datasetLabel"]].append((int(row["dhash"], 16), row["path"]))
    counts = Counter()
    examples = []
    validation_test = {"count": 0, "examples": []}
    for row in rows:
        if row["split"] == "train":
            continue
        value = int(row["dhash"], 16)
        candidates = training[row["datasetLabel"]]
        distance, path = min(((value ^ other).bit_count(), path) for other, path in candidates)
        if distance <= 4:
            counts[row["split"]] += 1
            if len(examples) < 50:
                examples.append({"path": row["path"], "trainingPath": path, "hammingDistance": distance})
        if row["split"] == "test":
            distance, path = min(((value ^ other).bit_count(), path) for other, path in validation[row["datasetLabel"]])
            if distance <= 4:
                validation_test["count"] += 1
                if len(validation_test["examples"]) < 50:
                    validation_test["examples"].append({"path": row["path"], "validationPath": path, "hammingDistance": distance})
    return {"method": "dHash64, Hamming <= 4 within a class; heuristic, not scene IDs",
            "countsAgainstTrain": dict(counts), "examplesAgainstTrain": examples, "validationTest": validation_test}


def prepare_dataset(root, output, catalog, source_files):
    root, output = Path(root).resolve(), Path(output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    (output / "report.json").unlink(missing_ok=True)
    expected = validate_source_files(source_files, catalog)
    actual = {path.relative_to(root).as_posix() for path in root.rglob("*")
              if path.is_file() and not any(part.startswith(".") for part in path.relative_to(root).parts)}
    missing, unexpected = sorted(expected - actual), sorted(actual - expected)
    if missing or unexpected:
        raise ValueError(f"Missing {len(missing)} files {missing[:3]}; unexpected {len(unexpected)} files {unexpected[:3]}")

    rows = []
    duplicates = defaultdict(list)
    for split in SPLITS:
        for index, item in enumerate(catalog):
            prefix = f"{split}/{item['datasetLabel']}/"
            for name in sorted(path for path in expected if path.startswith(prefix)):
                path = root / name
                if path.is_symlink() or not path.resolve().is_relative_to(root):
                    raise ValueError(f"Invalid image path: {name}")
                pixel_hash, dhash, width, height = inspect_image(path)
                row = {"path": name, "labelIndex": index, "classificationKey": item["classificationKey"],
                       "datasetLabel": item["datasetLabel"], "split": split, "sha256": sha256(path),
                       "pixelSha256": pixel_hash, "dhash": dhash, "width": width, "height": height}
                rows.append(row)
                duplicates[pixel_hash].append(row)

    excluded = []
    discarded_paths = set()
    for group in duplicates.values():
        if len({row["datasetLabel"] for row in group}) > 1:
            raise ValueError(f"Conflicting labels for identical image: {[row['path'] for row in group]}")
        kept, *rest = sorted(group, key=lambda row: (-SPLITS.index(row["split"]), row["path"]))
        for row in rest:
            discarded_paths.add(row["path"])
            excluded.append({"path": row["path"], "keptPath": kept["path"]})
    usable = [row for row in rows if row["path"] not in discarded_paths]
    counts = Counter((row["split"], row["datasetLabel"]) for row in usable)
    if len(counts) != len(SPLITS) * len(catalog):
        raise ValueError("Duplicate removal left an empty class/split; review source data")

    with (output / "manifest.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(usable)
    write_json(output / "labels.json", [{"index": i, **item} for i, item in enumerate(catalog)])
    report = {"status": "ready", "sourceImages": len(rows), "usableImages": len(usable),
              "catalogSha256": json_digest(catalog), "sourceInventorySha256": json_digest(source_files),
              "manifestSha256": sha256(output / "manifest.csv"), "sceneIndependent": False,
              "splitMethod": "upstream random video frames; exact duplicates removed, test preferred",
              "counts": {split: {item["datasetLabel"]: counts[split, item["datasetLabel"]] for item in catalog} for split in SPLITS},
              "excludedDuplicates": excluded, "nearDuplicates": near_duplicate_audit(usable)}
    write_json(output / "report.json", report)
    return report


def verify_prepared_dataset(root, output, catalog, source_files=None):
    root, output = Path(root).resolve(), Path(output)
    report = json.loads((output / "report.json").read_text(encoding="utf-8"))
    if report.get("status") != "ready" or report["manifestSha256"] != sha256(output / "manifest.csv"):
        raise ValueError("Prepared manifest is not ready or has changed")
    if report["catalogSha256"] != json_digest(catalog):
        raise ValueError("Catalog changed after dataset preparation")
    if source_files is not None:
        if report["sourceInventorySha256"] != json_digest(source_files) or report["sourceImages"] != len(source_files):
            raise ValueError("Prepared dataset does not match the pinned source inventory")
    with (output / "manifest.csv").open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream))
    for row in rows:
        path = root / row["path"]
        if not path.resolve().is_relative_to(root) or not path.is_file() or sha256(path) != row["sha256"]:
            raise ValueError(f"Dataset image changed: {row['path']}")
    if len(rows) != report["usableImages"]:
        raise ValueError("Prepared manifest count changed")
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=MODULE_DIR / "data/raw")
    parser.add_argument("--output", type=Path, default=MODULE_DIR / "data/prepared")
    parser.add_argument("--archives", type=Path, nargs="+", help="Complete Drive folder ZIP, or all its ZIP parts; destination must not exist")
    args = parser.parse_args()
    catalog = load_catalog()
    source = load_source_files(catalog=catalog)
    if args.archives:
        from archive_dataset import import_archives
        import_archives(args.archives, args.root, catalog, source)
    report = prepare_dataset(args.root, args.output, catalog, source)
    print(f"Prepared {report['usableImages']} / {report['sourceImages']} images; report: {args.output / 'report.json'}")


if __name__ == "__main__":
    main()
