package gov.mib.aims.backend.services.validation;

import gov.mib.aims.backend.exception.Errors;

/**
 * Подготовка обязательного текста и необязательных комментариев.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Обрезает обязательное поле и запрещает пустое значение.
     */
    public static String required(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw Errors.validationError(message);
        }

        return value.trim();
    }

    /**
     * Обрезает необязательное поле, сохраняя null.
     */
    public static String optional(String value) {
        return value == null ? null : value.trim();
    }
}
