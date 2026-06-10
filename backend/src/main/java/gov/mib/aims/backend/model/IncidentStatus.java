package gov.mib.aims.backend.model;

/**
 * Статус инцидента.
 */
public enum IncidentStatus {

    /** Черновик после создания. */
    DRAFT,
    /** Готов к анализу аналитиком. */
    READY_FOR_ANALYSIS,
    /** Готов к выполнению оперативным агентом. */
    READY_FOR_EXECUTION,
    /** Требуется уточнение у оператора. */
    CLARIFICATION_REQUIRED
}
