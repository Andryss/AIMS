"""Shared catalog and source-manifest validation; no model imports or network calls."""

import hashlib
import json
from collections import Counter
from pathlib import Path, PurePosixPath

MODULE_DIR = Path(__file__).resolve().parent
SPLITS = ("train", "validation", "test")


def sha256(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def json_digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True).encode()).hexdigest()


def write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def load_catalog(path=MODULE_DIR / "catalog.json"):
    items = json.loads(Path(path).read_text(encoding="utf-8"))
    if len(items) != 50:
        raise ValueError("Catalog must contain all 50 species")
    for field in ("classificationKey", "datasetLabel", "nameRu"):
        values = [item[field] for item in items]
        if any(not value.strip() for value in values) or len(set(values)) != 50:
            raise ValueError(f"Catalog {field} must be nonempty and unique")
    if any(not 1 <= item["threatLevel"] <= 10 for item in items):
        raise ValueError("Invalid demo threat level")
    return items


def validate_source_files(files, catalog):
    labels = {item["datasetLabel"] for item in catalog}
    paths = set()
    pairs = set()
    for item in files:
        raw_path = item["path"]
        path = PurePosixPath(raw_path)
        if (len(path.parts) != 3 or path.is_absolute() or ".." in path.parts
                or path.as_posix() != raw_path or "\\" in raw_path):
            raise ValueError(f"Invalid source path: {raw_path}")
        split, label, name = path.parts
        if split not in SPLITS or label not in labels or Path(name).suffix.lower() not in {".png", ".jpg", ".jpeg"}:
            raise ValueError(f"Unknown source path: {raw_path}")
        if raw_path in paths:
            raise ValueError(f"Duplicate source path: {raw_path}")
        paths.add(raw_path)
        pairs.add((split, label))

    if pairs != {(split, label) for split in SPLITS for label in labels}:
        raise ValueError("Incomplete source inventory: every split must contain every class")
    return paths


def load_source_files(directory=MODULE_DIR, catalog=None):
    directory = Path(directory)
    metadata = json.loads((directory / "sources.json").read_text(encoding="utf-8"))
    path = directory / metadata["sourceInventoryFile"]
    if sha256(path) != metadata["sourceInventorySha256"]:
        raise ValueError("Pinned source inventory checksum mismatch")
    files = json.loads(path.read_text(encoding="utf-8"))
    counts = Counter(item["path"].split("/")[0] for item in files)
    if len(files) != metadata["sourceFileCount"] or dict(counts) != metadata["sourceSplitCounts"]:
        raise ValueError("Incomplete pinned source inventory")
    validate_source_files(files, catalog if catalog is not None else load_catalog(directory / "catalog.json"))
    return files
