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
    ALIEN_CAPTURE,
    /** Нападение на людей. */
    ATTACK_ON_PEOPLE,
    /** Повреждение имущества. */
    PROPERTY_DAMAGE,
    /** Биологическое заражение. */
    BIOLOGICAL_CONTAMINATION,
    /** Нарушение работы критической инфраструктуры. */
    CRITICAL_INFRASTRUCTURE_DISRUPTION
}
