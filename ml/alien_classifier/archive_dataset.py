"""Import a whole Drive folder ZIP (or all ZIP parts) without partial extraction or overwrites."""

import re
import shutil
import stat
import tempfile
import zipfile
from collections import defaultdict
from contextlib import ExitStack
from pathlib import Path, PurePosixPath

from dataset import sha256, validate_source_files, write_json

MAX_UNCOMPRESSED_BYTES = 8 * 1024 ** 3
MAX_IMAGE_BYTES = 10 * 1024 ** 2


def member_path(name):
    path = PurePosixPath(name)
    if path.is_absolute() or ".." in path.parts or "\\" in name:
        raise ValueError(f"Unsafe archive path: {name}")
    parts = path.parts
    if parts and parts[0] == "Starfield_Fauna_50_240p":
        parts = parts[1:]
    if len(parts) != 3:
        raise ValueError(f"Unexpected archive path: {name}")
    return "/".join(parts)


def import_archives(archives, root, catalog, source_files):
    root = Path(root)
    if root.exists():
        raise FileExistsError(f"Destination already exists: {root}; choose an empty destination")
    validate_source_files(source_files, catalog)
    expected_groups = defaultdict(list)
    for row in source_files:
        expected_groups[row.get("sourcePath", row["path"])].append(row["path"])
    groups = defaultdict(list)
    provenance = []
    total_size = 0
    with ExitStack() as stack:
        for archive_path in archives:
            archive_path = Path(archive_path)
            archive = stack.enter_context(zipfile.ZipFile(archive_path))
            provenance.append({"name": archive_path.name, "sha256": sha256(archive_path)})
            for info in archive.infolist():
                if info.is_dir() or "__MACOSX" in PurePosixPath(info.filename).parts or Path(info.filename).name == ".DS_Store":
                    continue
                if stat.S_ISLNK(info.external_attr >> 16):
                    raise ValueError(f"Symlink archive path: {info.filename}")
                relative = member_path(info.filename)
                source_path = relative
                if source_path not in expected_groups:
                    candidate = re.sub(r" ?\(\d+\)(?=\.[^.]+$)", "", relative)
                    if len(expected_groups.get(candidate, [])) > 1:
                        source_path = candidate
                if source_path not in expected_groups:
                    raise ValueError(f"Unknown archive path: {relative}")
                total_size += info.file_size
                if info.file_size > MAX_IMAGE_BYTES or total_size > MAX_UNCOMPRESSED_BYTES:
                    raise ValueError("Archive exceeds the dataset size limit")
                groups[source_path].append((archive, archive_path.name, info))
        if set(groups) != set(expected_groups) or any(len(groups[path]) != len(paths) for path, paths in expected_groups.items()):
            raise ValueError("Dataset archive incomplete or contains repeated/unexpected images; provide all ZIP parts")
        root.parent.mkdir(parents=True, exist_ok=True)
        staging = Path(tempfile.mkdtemp(prefix=".dataset-import-", dir=root.parent))
        mappings = []
        try:
            for source_path, paths in expected_groups.items():
                members = sorted(groups[source_path], key=lambda item: (item[2].CRC, item[2].file_size, item[2].filename))
                for destination, (archive, archive_name, info) in zip(sorted(paths), members):
                    target = staging / destination
                    target.parent.mkdir(parents=True, exist_ok=True)
                    with archive.open(info) as content, target.open("wb") as output:
                        shutil.copyfileobj(content, output)
                    mappings.append({"path": destination, "archive": archive_name, "member": info.filename,
                                     "crc32": f"{info.CRC:08x}", "headerOffset": info.header_offset})
            staging.rename(root)
        finally:
            if staging.exists():
                shutil.rmtree(staging)
    report = {"images": len(source_files), "archives": provenance, "members": mappings,
              "duplicateNamePolicy": "Order ambiguous same-name source images by ZIP CRC; individual Drive IDs are not inferred"}
    write_json(root.parent / f"{root.name}-archives.json", report)
    return report
