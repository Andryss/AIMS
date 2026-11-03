import pytest

from evaluate import classification_metrics, rejection_metrics, select_threshold


def test_metrics_include_every_class_and_actual_misclassifications():
    scores = [[0.8, 0.1, 0.1], [0.2, 0.7, 0.1], [0.1, 0.8, 0.1], [0.1, 0.6, 0.3]]
    result = classification_metrics([0, 0, 1, 2], scores, ["a", "b", "c"])
    assert result["samples"] == 4
    assert result["top1"] == 0.5
    assert result["top3"] == 1.0
    assert result["macroF1"] == pytest.approx(7 / 18)
    assert result["confusionMatrix"] == [[1, 1, 0], [0, 1, 0], [0, 1, 0]]
    assert result["perClass"][0] == {"key": "a", "support": 2, "precision": 1.0, "recall": 0.5, "f1": 2 / 3}
    assert result["perClass"][2]["recall"] == 0.0
    assert result["mistakes"][0]["index"] == 1


@pytest.mark.parametrize("targets,scores", [
    ([], []), ([0, 1], [[0.8, 0.2]]), ([2], [[0.8, 0.2]]),
    ([0], [[float("nan"), 0.2]]), ([0], [[-0.1, 1.1]]), ([0], [[0.9, 0.9]]),
])
def test_incomplete_or_invalid_predictions_do_not_produce_quality_metrics(targets, scores):
    with pytest.raises(ValueError):
        classification_metrics(targets, scores, ["a", "b"])


def test_threshold_uses_validation_ties_together_and_requires_enough_samples():
    scores = [[0.95, 0.05], [0.9, 0.1], [0.8, 0.2], [0.8, 0.2]]
    result = select_threshold([0, 0, 0, 1], scores, target_precision=0.9, min_samples=2)
    assert result == {"threshold": 0.9, "accepted": 2, "coverage": 0.5, "accuracy": 1.0}
    assert select_threshold([0, 0, 0, 1], scores, min_samples=20) is None


def test_rejection_without_accepted_images_does_not_report_perfect_accuracy():
    result = rejection_metrics([0, 1], [[0.6, 0.4], [0.5, 0.5]], 0.9)
    assert result == {"threshold": 0.9, "accepted": 0, "coverage": 0.0, "accuracy": None}
