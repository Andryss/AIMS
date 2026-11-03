import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import pytest
from PIL import Image

@pytest.fixture
def dataset(tmp_path):
    root = tmp_path / "raw"
    catalog = [
        {"classificationKey": "starfield_zebra", "datasetLabel": "zebra", "nameRu": "Зебра"},
        {"classificationKey": "starfield_angler", "datasetLabel": "angler", "nameRu": "Удильщик"},
    ]
    source_files = []
    for split_index, split in enumerate(["train", "validation", "test"]):
        for class_index, item in enumerate(catalog):
            for number in range(2):
                path = Path(split) / item["datasetLabel"] / f"{number}.png"
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                Image.new("RGB", (12, 8), (split_index * 60, class_index * 90, number * 80)).save(target)
                source_files.append({"path": path.as_posix(), "fileId": f"file-{len(source_files)}"})
    return root, tmp_path / "prepared", catalog, source_files
