package gov.mib.aims.backend.model;

import java.util.List;

/**
 * Рассчитанная угроза и вклады критериев.
 */
public record IncidentThreatAssessment(int calculatedThreatLevel, List<Contribution> contributions) {

    /**
     * Вклад отдельного критерия в сумму до ограничения шкалой.
     */
    public record Contribution(Criterion criterion, int adjustment) {
    }

    /**
     * Источник вклада в оценку угрозы.
     */
    public enum Criterion {

        /** Угроза привязанного вида. */
        ALIEN_THREAT,
        /** Базовая угроза при отсутствии вида. */
        DEFAULT_THREAT,
        /** Поправка типа события. */
        EVENT_TYPE,
        /** Поправка количества пострадавших. */
        VICTIMS
    }
}
