package gov.mib.aims.backend.model;

/**
 * Тип сущности для записи в истории изменений.
 */
public enum EntityType {

    /** Инцидент. */
    INCIDENT,

    /** Экземпляр оборудования. */
    EQUIPMENT,

    /** Заявка на движение оборудования. */
    EQUIPMENT_MOVEMENT,

    /** Алерт внешнего мониторинга. */
    MONITORING_ALERT
}
