package gov.mib.aims.backend.model;

/**
 * Статус заявки на движение оборудования.
 */
public enum EquipmentMovementStatus {

    /** В ожидании обработки. */
    PENDING,
    /** Обрабатывается завхозом. */
    PROCESSING,
    /** Движение выполнено. */
    COMPLETED,
    /** Заявка отменена. */
    CANCELLED,
    /** Заявка отклонена. */
    REJECTED
}
