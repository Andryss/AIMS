package gov.mib.aims.backend.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Тип события инцидента (числовой код для API и БД).
 */
public enum IncidentEventType {

    // Неопознанное появление в небе
    UNIDENTIFIED_SIGHTING(1),
    // Подозрение на контакт
    CONTACT_SUSPECT(2),
    // Нелегальная высадка НЛО
    ILLEGAL_UFO_LANDING(3),
    // Аномалия памяти у свидетелей
    MEMORY_ANOMALY(4),
    // Обнаружение инопланетного артефакта
    ALIEN_ARTIFACT(5),
    // Захват или транспортировка инопланетянина
    ALIEN_CAPTURE(6);

    private final int code;

    IncidentEventType(int code) {
        this.code = code;
    }

    /**
     * Числовой код типа события.
     *
     * @return код
     */
    public int getCode() {
        return code;
    }

    /**
     * Находит тип события по коду.
     *
     * @param code числовой код
     * @return тип события, если найден
     */
    public static Optional<IncidentEventType> fromCode(int code) {
        return Arrays.stream(values())
                .filter(type -> type.code == code)
                .findFirst();
    }
}
