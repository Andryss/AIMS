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

    /** Тендер на закупку оборудования. */
    TENDER,

    /** Алерт внешнего мониторинга. */
    MONITORING_ALERT
}
