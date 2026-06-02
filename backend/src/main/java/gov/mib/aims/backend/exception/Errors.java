package gov.mib.aims.backend.exception;

/**
 * Фабрика ошибок приложения.
 */
public final class Errors {

    private Errors() {
    }

    /**
     * Неверный логин или пароль.
     */
    public static BaseException invalidCredentials() {
        return BaseException.builder()
                .code(401)
                .message("auth.invalid_credentials")
                .humanMessage("Invalid login or password")
                .build();
    }

    /**
     * Пользователь не аутентифицирован.
     */
    public static BaseException unauthorized() {
        return BaseException.builder()
                .code(401)
                .message("auth.unauthorized")
                .humanMessage("Authentication required")
                .build();
    }

    /**
     * Доступ запрещён.
     */
    public static BaseException accessDenied() {
        return BaseException.builder()
                .code(403)
                .message("auth.access_denied")
                .humanMessage("Access denied")
                .build();
    }

    /**
     * Пользователь не найден.
     */
    public static BaseException userNotFound(String login) {
        return BaseException.builder()
                .code(401)
                .message("auth.user_not_found")
                .humanMessage("User not found: " + login)
                .build();
    }

    /**
     * Ошибка валидации.
     */
    public static BaseException validationError(String message) {
        return BaseException.builder()
                .code(400)
                .message("validation.error")
                .humanMessage(message != null ? message : "Validation error")
                .build();
    }

    /**
     * Ресурс не найден.
     */
    public static BaseException notFound() {
        return BaseException.builder()
                .code(404)
                .message("resource.not.found")
                .humanMessage("Resource not found")
                .build();
    }

    /**
     * Файл не найден.
     */
    public static BaseException fileNotFound() {
        return BaseException.builder()
                .code(404)
                .message("file.not_found")
                .humanMessage("File not found")
                .build();
    }

    /**
     * Пустой или отсутствующий файл в запросе.
     */
    public static BaseException fileEmpty() {
        return BaseException.builder()
                .code(400)
                .message("file.empty")
                .humanMessage("File must not be empty")
                .build();
    }

    /**
     * Необработанная ошибка.
     */
    public static BaseException unhandledExceptionError() {
        return BaseException.builder()
                .code(500)
                .message("internal.error")
                .humanMessage("Something went wrong...")
                .build();
    }
}
