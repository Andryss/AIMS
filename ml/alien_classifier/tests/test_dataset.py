import csv
import shutil

import pytest
from PIL import Image

from prepare_dataset import prepare_dataset, verify_prepared_dataset



def test_preparation_preserves_catalog_order_and_detects_changed_images(dataset):
    root, output, catalog, source_files = dataset
    report = prepare_dataset(root, output, catalog, source_files)

    rows = list(csv.DictReader((output / "manifest.csv").open()))
    assert report["sourceImages"] == 12
    assert report["usableImages"] == 12
    assert report["sceneIndependent"] is False
    assert {row["labelIndex"] for row in rows if row["datasetLabel"] == "zebra"} == {"0"}
    assert {row["labelIndex"] for row in rows if row["datasetLabel"] == "angler"} == {"1"}
    assert rows[0]["path"] == "train/zebra/0.png"
    assert len(verify_prepared_dataset(root, output, catalog)) == 12

    Image.new("RGB", (12, 8), "pink").save(root / "train/zebra/0.png")
    with pytest.raises(ValueError, match="changed"):
        verify_prepared_dataset(root, output, catalog)


@pytest.mark.parametrize("path", ["train/zebra/0.png", "validation/angler/1.png", "test/zebra/1.png"])
def test_partial_download_never_produces_ready_manifest(dataset, path):
    root, output, catalog, source_files = dataset
    (root / path).unlink()
    with pytest.raises(ValueError, match="Missing"):
        prepare_dataset(root, output, catalog, source_files)
    assert not (output / "report.json").exists()


@pytest.mark.parametrize("part", ["train", "validation/angler", "test/zebra"])
def test_missing_split_or_class_is_rejected(dataset, part):
    root, output, catalog, source_files = dataset
    shutil.rmtree(root / part)
    with pytest.raises(ValueError):
        prepare_dataset(root, output, catalog, source_files)


def test_unknown_class_and_incomplete_source_inventory_are_rejected(dataset):
    root, output, catalog, source_files = dataset
    source_files = [row for row in source_files if not row["path"].startswith("test/angler/")]
    with pytest.raises(ValueError, match="inventory"):
        prepare_dataset(root, output, catalog, source_files)

    extra = root / "train/unknown/new.png"
    extra.parent.mkdir()
    Image.new("RGB", (12, 8), "red").save(extra)
    with pytest.raises(ValueError):
        prepare_dataset(root, output, catalog, source_files + [{"path": "train/unknown/new.png", "fileId": "extra"}])


@pytest.mark.parametrize("contents", [b"not an image", b"\x89PNG\r\n\x1a\ntruncated"])
def test_corrupted_image_is_rejected(dataset, contents):
    root, output, catalog, source_files = dataset
    (root / "train/zebra/0.png").write_bytes(contents)
    with pytest.raises(ValueError, match="image"):
        prepare_dataset(root, output, catalog, source_files)


def test_identical_image_with_conflicting_labels_is_rejected(dataset):
    root, output, catalog, source_files = dataset
    shutil.copyfile(root / "train/zebra/0.png", root / "test/angler/0.png")
    with pytest.raises(ValueError, match="Conflicting"):
        prepare_dataset(root, output, catalog, source_files)


def test_cross_split_duplicate_is_removed_from_training_not_test(dataset):
    root, output, catalog, source_files = dataset
    shutil.copyfile(root / "test/zebra/0.png", root / "train/zebra/0.png")
    report = prepare_dataset(root, output, catalog, source_files)
    rows = list(csv.DictReader((output / "manifest.csv").open()))
    assert report["sourceImages"] == 12
    assert report["usableImages"] == 11
    assert len(report["excludedDuplicates"]) == 1
    assert report["excludedDuplicates"][0]["path"] == "train/zebra/0.png"
    assert report["excludedDuplicates"][0]["keptPath"] == "test/zebra/0.png"
    assert "test/zebra/0.png" in {row["path"] for row in rows}
    assert "train/zebra/0.png" not in {row["path"] for row in rows}


def test_counts_can_differ_between_classes(dataset):
    root, output, catalog, source_files = dataset
    path = "train/angler/extra.png"
    Image.new("RGB", (12, 8), "purple").save(root / path)
    source_files.append({"path": path, "fileId": "extra"})
    assert prepare_dataset(root, output, catalog, source_files)["sourceImages"] == 13


@pytest.mark.parametrize("bad_path", ["../outside.png", "/tmp/outside.png", "train/angler/../../escape.png"])
def test_source_paths_cannot_escape_dataset(dataset, bad_path):
    root, output, catalog, source_files = dataset
    with pytest.raises(ValueError, match="path"):
        prepare_dataset(root, output, catalog, source_files + [{"path": bad_path, "fileId": "bad"}])


def test_modified_manifest_is_rejected_before_training(dataset):
    root, output, catalog, source_files = dataset
    prepare_dataset(root, output, catalog, source_files)
    manifest = output / "manifest.csv"
    manifest.write_text(manifest.read_text().replace("train/zebra/0.png", "test/zebra/0.png"))
    with pytest.raises(ValueError, match="manifest"):
        verify_prepared_dataset(root, output, catalog)


def test_failed_repreparation_invalidates_previous_ready_report(dataset):
    root, output, catalog, source_files = dataset
    prepare_dataset(root, output, catalog, source_files)
    (root / "test/angler/1.png").unlink()
    with pytest.raises(ValueError):
        prepare_dataset(root, output, catalog, source_files)
    assert not (output / "report.json").exists()


def test_near_duplicate_audit_also_checks_validation_against_test(dataset):
    root, output, catalog, source_files = dataset
    for split, offset in [("validation", 10), ("test", 20)]:
        image = Image.new("RGB", (12, 8))
        image.putdata([(255 - x * 8 - offset,) * 3 for y in range(8) for x in range(12)])
        image.save(root / split / "zebra/0.png")
    report = prepare_dataset(root, output, catalog, source_files)
    pairs = report["nearDuplicates"]["validationTest"]["examples"]
    assert any(row["path"] == "test/zebra/0.png" and row["validationPath"] == "validation/zebra/0.png" for row in pairs)
