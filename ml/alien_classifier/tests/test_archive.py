import zipfile

import pytest

from archive_dataset import import_archives


def make_archive(path, root, source, prefix="Starfield_Fauna_50_240p/"):
    with zipfile.ZipFile(path, "w") as archive:
        for row in source:
            archive.write(root / row["path"], prefix + row["path"])


def test_full_archive_is_verified_then_imported_atomically(dataset, tmp_path):
    raw, _, catalog, source = dataset
    archive = tmp_path / "full.zip"
    make_archive(archive, raw, source)
    target = tmp_path / "imported"
    result = import_archives([archive], target, catalog, source)
    assert result["images"] == 12
    assert len(result["archives"][0]["sha256"]) == 64
    assert len(list(target.rglob("*.png"))) == 12
    assert (target / source[0]["path"]).read_bytes() == (raw / source[0]["path"]).read_bytes()


def test_drive_split_zips_are_combined_as_one_complete_dataset(dataset, tmp_path):
    raw, _, catalog, source = dataset
    parts = []
    for index, split in enumerate(["train", "validation", "test"]):
        part = tmp_path / f"part-{index}.zip"
        make_archive(part, raw, [row for row in source if row["path"].startswith(split + "/")])
        parts.append(part)
    assert import_archives(parts, tmp_path / "imported", catalog, source)["images"] == 12


def test_incomplete_archive_does_not_leave_partial_dataset(dataset, tmp_path):
    raw, _, catalog, source = dataset
    archive = tmp_path / "partial.zip"
    make_archive(archive, raw, source[:-1])
    target = tmp_path / "imported"
    with pytest.raises(ValueError, match="incomplete"):
        import_archives([archive], target, catalog, source)
    assert not target.exists()


@pytest.mark.parametrize("bad_name", ["../escape.png", "/absolute.png", "Starfield_Fauna_50_240p/../../escape.png"])
def test_unsafe_archive_is_rejected_before_extracting(dataset, tmp_path, bad_name):
    raw, _, catalog, source = dataset
    archive = tmp_path / "unsafe.zip"
    make_archive(archive, raw, source)
    with zipfile.ZipFile(archive, "a") as stream:
        stream.writestr(bad_name, b"bad")
    target = tmp_path / "imported"
    with pytest.raises(ValueError, match="path"):
        import_archives([archive], target, catalog, source)
    assert not target.exists()


@pytest.mark.parametrize("suffix", [" (1)", "(1)"])
def test_duplicate_source_names_keep_both_images_without_claiming_individual_drive_ids(dataset, tmp_path, suffix):
    raw, _, catalog, source = dataset
    duplicated = source[0]
    source = [dict(row) for row in source]
    source[0] = {"path": "train/zebra/0--variant-1.png", "sourcePath": duplicated["path"], "fileIds": ["one", "two"]}
    source.append({"path": "train/zebra/0--variant-2.png", "sourcePath": duplicated["path"], "fileIds": ["one", "two"]})
    archive = tmp_path / "duplicate.zip"
    original = [duplicated, *[row for row in source[1:-1]]]
    make_archive(archive, raw, original)
    with zipfile.ZipFile(archive, "a") as stream:
        stream.write(raw / "train/zebra/1.png", f"Starfield_Fauna_50_240p/train/zebra/0{suffix}.png")
    target = tmp_path / "imported"
    assert import_archives([archive], target, catalog, source)["images"] == 13
    assert (target / "train/zebra/0--variant-1.png").exists()
    assert (target / "train/zebra/0--variant-2.png").exists()


def test_existing_data_is_not_overwritten(dataset, tmp_path):
    raw, _, catalog, source = dataset
    archive = tmp_path / "full.zip"
    make_archive(archive, raw, source)
    with pytest.raises(FileExistsError):
        import_archives([archive], raw, catalog, source)
