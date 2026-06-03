package gov.mib.aims.backend.model;

/**
 * Тип события инцидента (строковое значение в API и БД).
 */
public enum IncidentEventType {

    /** Неопознанное появление в небе. */
    UNIDENTIFIED_SIGHTING,
    /** Подозрение на контакт. */
    CONTACT_SUSPECT,
    /** Нелегальная высадка НЛО. */
    ILLEGAL_UFO_LANDING,
    /** Аномалия памяти у свидетелей. */
    MEMORY_ANOMALY,
    /** Обнаружение инопланетного артефакта. */
    ALIEN_ARTIFACT,
    /** Захват или транспортировка инопланетянина. */
    ALIEN_CAPTURE
}
