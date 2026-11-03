package gov.mib.aims.backend.model;

/**
 * Этап закупки оборудования.
 */
public enum TenderStatus {

    /** Потребность. */
    NEED,

    /** Сбор предложений. */
    COLLECTING_OFFERS,

    /** Ожидание поставки. */
    AWAITING_DELIVERY,

    /** Завершён. */
    COMPLETED,

    /** Отменён. */
    CANCELLED
}
