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
    CLARIFICATION_REQUIRED,
    /** Подготовка к выполнению агентом. */
    PREPARATION_FOR_EXECUTION,
    /** Подготовлен к выполнению. */
    PREPARED_FOR_EXECUTION,
    /** Требуется повторный анализ. */
    REANALYSIS_REQUIRED
}
