package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ErrorObject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Глобальный обработчик исключений REST API.
 */
@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {

    /**
     * Обрабатывает BaseException и возвращает ErrorObject.
     *
     * @param ex исключение
     * @param response HTTP-ответ
     * @return тело ошибки
     */
    @ExceptionHandler(BaseException.class)
    public ErrorObject handleBaseException(BaseException ex, HttpServletResponse response) {
        log.warn("BaseException: code={}, message={}", ex.getCode(), ex.getMessage());
        response.setStatus(ex.getCode());
        return createErrorObject(ex);
    }

    /**
     * Обрабатывает отказ в доступе (@PreAuthorize и method security).
     *
     * @param ex исключение
     * @return тело ошибки
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(FORBIDDEN)
    public ErrorObject handleAccessDenied(RuntimeException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return createErrorObject(Errors.accessDenied());
    }

    /**
     * Обрабатывает ошибки валидации тела запроса.
     *
     * @param ex исключение
     * @return тело ошибки
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation error";
        log.warn("Validation error: {}", message);
        return createErrorObject(Errors.validationError(message));
    }

    /**
     * Обрабатывает ошибки валидации параметров.
     *
     * @param ex исключение
     * @return тело ошибки
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return createErrorObject(Errors.validationError(ex.getMessage()));
    }

    /**
     * Обрабатывает отсутствие ресурса.
     *
     * @param ex исключение
     * @return тело ошибки
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorObject handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getResourcePath());
        return createErrorObject(Errors.notFound());
    }

    /**
     * Обрабатывает необработанные исключения.
     *
     * @param ex исключение
     * @return тело ошибки
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorObject handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return createErrorObject(Errors.unhandledExceptionError());
    }

    private static ErrorObject createErrorObject(BaseException ex) {
        return new ErrorObject()
                .code(ex.getCode())
                .message(ex.getMessage())
                .humanMessage(ex.getHumanMessage());
    }
}
