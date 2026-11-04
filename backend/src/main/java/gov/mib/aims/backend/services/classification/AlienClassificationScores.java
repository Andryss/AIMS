package gov.mib.aims.backend.services.classification;

import gov.mib.aims.backend.model.AlienPrediction;

import java.util.List;

/**
 * Усредняет распределения с весом, равным уверенности лучшего класса каждого снимка.
 */
final class AlienClassificationScores {

    private final double[] weightedScores;
    private double totalWeight;

    AlienClassificationScores(int classCount) {
        weightedScores = new double[classCount];
    }

    void add(float[] logits) {
        if (logits.length != weightedScores.length) {
            throw new IllegalStateException("Invalid classifier output size");
        }
        float maximum = Float.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (!Float.isFinite(logit)) {
                throw new IllegalStateException("Nonfinite classifier output");
            }
            maximum = Math.max(maximum, logit);
        }
        double sum = 0;
        for (float logit : logits) {
            sum += Math.exp((double) logit - maximum);
        }
        double weight = 1 / sum;
        for (int index = 0; index < logits.length; index++) {
            double probability = Math.exp((double) logits[index] - maximum) / sum;
            weightedScores[index] += probability * weight;
        }
        totalWeight += weight;
    }

    boolean isEmpty() {
        return totalWeight == 0;
    }

    AlienPrediction prediction(List<String> keys, double threshold) {
        if (isEmpty() || keys.size() != weightedScores.length) {
            throw new IllegalStateException("No classifier scores");
        }
        int best = 0;
        for (int index = 1; index < weightedScores.length; index++) {
            if (weightedScores[index] > weightedScores[best]) {
                best = index;
            }
        }
        double score = weightedScores[best] / totalWeight;
        return new AlienPrediction(keys.get(best), score, score >= threshold);
    }
}
