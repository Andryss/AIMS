package gov.mib.aims.backend.model;

/**
 * Статус инцидента.
 */
public enum IncidentStatus {

    /** Черновик после создания. */
    DRAFT,
    /** Готов к анализу аналитиком. */
    READY_FOR_ANALYSIS
}
