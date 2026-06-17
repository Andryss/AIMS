package gov.mib.aims.backend.model;

/**
 * Статус алерта внешней системы мониторинга.
 */
public enum MonitoringAlertStatus {

    /** Получен, инцидент ещё не создан. */
    NEW,

    /** По алерту зарегистрирован инцидент. */
    INCIDENT_CREATED
}
