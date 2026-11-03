"""Quality metrics and validation-only rejection calibration; no training side effects."""

import numpy as np


def validated_predictions(targets, probabilities, class_count=None):
    targets = np.asarray(targets)
    scores = np.asarray(probabilities, dtype=np.float64)
    if (targets.ndim != 1 or not len(targets) or not np.issubdtype(targets.dtype, np.integer)
            or scores.ndim != 2 or len(scores) != len(targets) or scores.shape[1] < 2):
        raise ValueError("Expected a complete nonempty set of class targets and probability rows")
    if class_count is not None and scores.shape[1] != class_count:
        raise ValueError("Prediction class count does not match catalog")
    if np.any(targets < 0) or np.any(targets >= scores.shape[1]):
        raise ValueError("Target outside class range")
    if not np.isfinite(scores).all() or np.any(scores < 0) or not np.allclose(scores.sum(axis=1), 1):
        raise ValueError("Predictions must be finite, normalized probabilities")
    return targets, scores


def classification_metrics(targets, probabilities, class_keys):
    targets, scores = validated_predictions(targets, probabilities, len(class_keys))
    predicted = scores.argmax(axis=1)
    matrix = np.zeros((len(class_keys), len(class_keys)), dtype=int)
    np.add.at(matrix, (targets, predicted), 1)
    per_class = []
    for index, key in enumerate(class_keys):
        correct = int(matrix[index, index])
        support, selected = int(matrix[index].sum()), int(matrix[:, index].sum())
        precision = correct / selected if selected else 0.0
        recall = correct / support if support else 0.0
        f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
        per_class.append({"key": key, "support": support, "precision": precision, "recall": recall, "f1": f1})
    top3 = np.argsort(-scores, axis=1, kind="stable")[:, :min(3, len(class_keys))]
    mistakes = [{"index": int(i), "expected": class_keys[targets[i]], "predicted": class_keys[predicted[i]],
                 "score": float(scores[i, predicted[i]])} for i in np.flatnonzero(targets != predicted)]
    return {"samples": len(targets), "top1": float(np.mean(targets == predicted)),
            "top3": float(np.mean(np.any(top3 == targets[:, None], axis=1))),
            "macroF1": float(np.mean([row["f1"] for row in per_class])),
            "perClass": per_class, "confusionMatrix": matrix.tolist(), "mistakes": mistakes}


def rejection_metrics(targets, probabilities, threshold):
    targets, scores = validated_predictions(targets, probabilities)
    if not 0 <= threshold <= 1:
        raise ValueError("Threshold must be between 0 and 1")
    accepted = scores.max(axis=1) >= threshold
    count = int(accepted.sum())
    return {"threshold": float(threshold), "accepted": count, "coverage": count / len(targets),
            "accuracy": float(np.mean(scores[accepted].argmax(axis=1) == targets[accepted])) if count else None}


def select_threshold(targets, probabilities, target_precision=0.9, min_samples=20):
    targets, scores = validated_predictions(targets, probabilities)
    if not 0 < target_precision <= 1 or min_samples < 1:
        raise ValueError("Invalid calibration constraints")
    confidence = scores.max(axis=1)
    order = np.argsort(-confidence, kind="stable")
    correct = np.cumsum(scores.argmax(axis=1)[order] == targets[order])
    boundaries = np.r_[np.flatnonzero(np.diff(confidence[order])) + 1, len(order)]
    eligible = boundaries[(boundaries >= min_samples) & (correct[boundaries - 1] / boundaries >= target_precision)]
    if not len(eligible):
        return None
    count = int(eligible[-1])
    return {"threshold": float(confidence[order[count - 1]]), "accepted": count,
            "coverage": count / len(targets), "accuracy": float(correct[count - 1] / count)}
