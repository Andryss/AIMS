package gov.mib.aims.backend.model;

/**
 * Состояние экземпляра оборудования.
 */
public enum EquipmentStatus {

    /** На складе, доступно для выдачи. */
    AVAILABLE,
    /** Выдано ответственному сотруднику. */
    ISSUED
}
