package gov.mib.aims.backend.model;

/**
 * Внутренний результат модели, не предназначенный для публичного API.
 */
public record AlienPrediction(String classificationKey, double score, boolean matched) {
}
