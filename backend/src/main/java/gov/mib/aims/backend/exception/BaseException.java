package gov.mib.aims.backend.exception;

import lombok.Builder;
import lombok.Getter;

/**
 * Базовое исключение для обработки ошибок API.
 */
@Getter
public class BaseException extends RuntimeException {

    private final int code;
    private final String message;
    private final String humanMessage;

    @Builder
    public BaseException(int code, String message, String humanMessage) {
        super(humanMessage);
        this.code = code;
        this.message = message;
        this.humanMessage = humanMessage;
    }
}
