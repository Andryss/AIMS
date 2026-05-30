package gov.mib.aims.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Базовое исключение для обработки ошибок API.
 */
@Getter
@Builder
@AllArgsConstructor
public class BaseException extends RuntimeException {

    private final int code;
    private final String message;
    private final String humanMessage;
}
