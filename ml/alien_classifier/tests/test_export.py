import hashlib
import json
import shutil
from types import SimpleNamespace

import numpy as np
import onnxruntime as ort
import pytest
import torch
from PIL import Image

import export as export_module
from dataset import load_catalog, write_json
from export import export_run, verify_outputs
from model import PREPROCESSING, WEIGHTS, build_model


@pytest.fixture(scope="module")
def export_source(tmp_path_factory):
    root = tmp_path_factory.mktemp("export-source")
    run = root / "run"
    run.mkdir()
    torch.set_num_threads(2)
    torch.manual_seed(7)
    model = build_model(50, pretrained=False)
    labels = [{"index": index, **item} for index, item in enumerate(load_catalog())]
    torch.save({"model": model.state_dict(), "labels": labels, "preprocessing": PREPROCESSING,
                "weights": str(WEIGHTS), "epoch": 7, "validationLoss": 0.5,
                "featureCacheSignature": "test-source", "complete": True}, run / "best.pt")
    write_json(run / "run.json", {"status": "completed", "bestEpoch": 7, "featureCacheSignature": "test-source"})
    write_json(run / "metrics.json", {"bestEpoch": 7, "validationCalibration": {"threshold": 0.4},
                                     "sceneIndependent": False, "unknownRejectionValidated": False})
    write_json(run / "labels.json", labels)
    write_json(run / "preprocessing.json", PREPROCESSING)
    image = root / "red.png"
    Image.new("RGB", (301, 181), "red").save(image)
    pattern = np.zeros((181, 301, 3), dtype=np.uint8)
    pattern[:, :, 0] = np.arange(301) % 256
    pattern[:, :, 1] = np.arange(181)[:, None]
    pattern[:, :, 2] = 127
    Image.fromarray(pattern).save(root / "pattern.png")
    return run, image, model


def test_export_preserves_all_class_indices_dynamic_batches_and_java_reference_bytes(export_source, tmp_path):
    run, image, model = export_source
    output = tmp_path / "bundle"
    manifest = export_run(run, output, [image, image.with_name("pattern.png")])
    assert manifest["classCount"] == 50
    assert manifest["bestEpoch"] == 7
    assert manifest["decision"]["threshold"] == 0.4
    assert manifest["decision"]["unknownRejectionValidated"] is False
    assert manifest["input"] == {"name": "images", "dtype": "float32", "shape": ["batch", 3, 224, 224]}
    labels = json.loads((output / "labels.json").read_text())
    assert labels[0]["classificationKey"] == "starfield_angler"
    assert labels[-1]["classificationKey"] == "starfield_windbag"
    assert [row["index"] for row in labels] == list(range(50))
    preprocessing = json.loads((output / "preprocessing.json").read_text())
    assert preprocessing["resizeRounding"] == "floor"
    assert preprocessing["centerCropRounding"] == "ties-to-even"
    for name, digest in manifest["files"].items():
        with (output / name).open("rb") as stream:
            assert hashlib.file_digest(stream, "sha256").hexdigest() == digest

    references = json.loads((output / "references/cases.json").read_text())
    first = references["cases"][0]
    values = np.fromfile(output / first["input"], dtype="<f4").reshape(1, 3, 224, 224)
    assert values[0, :, 0, 0] == pytest.approx([(1 - 0.485) / 0.229, -0.456 / 0.224, -0.406 / 0.225])
    pattern = np.fromfile(output / references["cases"][1]["input"], dtype="<f4").reshape(1, 3, 224, 224)
    # Resize 301x181 -> 425x256, crop left100/top16 (half-pixel offset rounds to even).
    assert pattern[0, 0, 0, :3] == pytest.approx([(value / 255 - 0.485) / 0.229 for value in [71, 71, 72]])
    assert pattern[0, 1, 0, 0] == pytest.approx((11 / 255 - 0.456) / 0.224)
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    session = ort.InferenceSession(str(output / "model.onnx"), sess_options=options, providers=["CPUExecutionProvider"])
    for size in [1, 3]:
        batch = np.repeat(values, size, axis=0)
        with torch.inference_mode():
            expected = model(torch.from_numpy(batch)).numpy()
        actual = session.run(["logits"], {"images": batch})[0]
        assert actual.shape == (size, 50)
        np.testing.assert_allclose(actual, expected, atol=1e-5, rtol=1e-4)
    assert not (output / "model.onnx.data").exists()


@pytest.mark.parametrize("file,mutation", [
    ("run.json", lambda value: {**value, "status": "timed_out"}),
    ("run.json", lambda value: {**value, "bestEpoch": 99}),
    ("labels.json", lambda value: list(reversed(value))),
    ("preprocessing.json", lambda value: {**value, "mean": [0, 0, 0]}),
    ("metrics.json", lambda value: {**value, "validationCalibration": {"threshold": 2}}),
])
def test_invalid_source_is_rejected_before_publishing_a_bundle(export_source, tmp_path, file, mutation):
    run, image, _ = export_source
    source = tmp_path / "bad-run"
    shutil.copytree(run, source)
    write_json(source / file, mutation(json.loads((source / file).read_text())))
    output = tmp_path / "output"
    with pytest.raises(ValueError):
        export_run(source, output, [image])
    assert not output.exists()


def test_existing_bundle_is_not_overwritten(export_source, tmp_path):
    run, image, _ = export_source
    with pytest.raises(FileExistsError):
        export_run(run, tmp_path, [image])


def test_failed_image_verification_leaves_no_partial_bundle(export_source, tmp_path):
    run, _, _ = export_source
    broken = tmp_path / "broken.png"
    broken.write_bytes(b"not an image")
    output = tmp_path / "bundle"
    with pytest.raises(ValueError):
        export_run(run, output, [broken])
    assert not output.exists()


def test_numerical_failure_after_graph_creation_removes_staging_files(export_source, tmp_path, monkeypatch):
    run, image, _ = export_source

    def reject_logits(model, session, images, staging):
        assert (staging / "model.onnx").is_file()
        raise ValueError("Numerical mismatch")

    monkeypatch.setattr(export_module, "verify_bundle", reject_logits)
    output = tmp_path / "bundle"
    with pytest.raises(ValueError, match="Numerical mismatch"):
        export_run(run, output, [image])
    assert not output.exists()
    assert list(tmp_path.glob(".onnx-export-*")) == []


@pytest.mark.parametrize("bias,actual,expected_error", [
    ([0.001, 2.0], [0.00105, 2.00005], 0.00005),
    ([0.3536013, 2.0], [0.3537397, 2.0], 0.0001384),
    ([0.001, 2.0], [0.011, 2.0], None),
    ([1.0, 1.00001], [1.00001, 1.0], None),
])
def test_verification_accepts_fp32_rounding_but_rejects_large_errors_and_changed_classes(bias, actual, expected_error):
    model = torch.nn.Linear(1, 2)
    with torch.no_grad():
        model.weight.zero_()
        model.bias.copy_(torch.tensor(bias))
    session = SimpleNamespace(run=lambda names, inputs: [np.array([actual], dtype=np.float32)])
    if expected_error is not None:
        expected, error = verify_outputs(model, session, np.zeros((1, 1), dtype=np.float32))
        assert expected.argmax(axis=1).tolist() == [1]
        assert error == pytest.approx(expected_error, abs=1e-7)
    else:
        with pytest.raises(ValueError):
            verify_outputs(model, session, np.zeros((1, 1), dtype=np.float32))
