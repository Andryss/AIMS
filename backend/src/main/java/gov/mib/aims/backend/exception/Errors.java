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
     * Недостаточно прав для операции.
     */
    public static BaseException insufficientRole() {
        return BaseException.builder()
                .code(403)
                .message("auth.insufficient_role")
                .humanMessage("Insufficient role for this operation")
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
     * Ошибка сохранения загружаемого файла.
     */
    public static BaseException fileUploadFailed() {
        return BaseException.builder()
                .code(500)
                .message("file.upload_failed")
                .humanMessage("Failed to store uploaded file")
                .build();
    }

    /**
     * Уведомление не найдено.
     */
    public static BaseException notificationNotFound() {
        return BaseException.builder()
                .code(404)
                .message("notification.not_found")
                .humanMessage("Notification not found")
                .build();
    }

    /**
     * Некорректная ссылка на связанную сущность.
     *
     * @param ref строка ссылки
     */
    public static BaseException invalidRelatedEntity(String ref) {
        return BaseException.builder()
                .code(400)
                .message("notification.invalid_related_entity")
                .humanMessage("Invalid related entity reference: " + ref)
                .build();
    }

    /**
     * Инцидент не найден.
     */
    public static BaseException incidentNotFound() {
        return BaseException.builder()
                .code(404)
                .message("incident.not_found")
                .humanMessage("Incident not found")
                .build();
    }

    /**
     * Недопустимый переход статуса инцидента.
     */
    public static BaseException invalidStatusTransition() {
        return BaseException.builder()
                .code(400)
                .message("incident.invalid_status_transition")
                .humanMessage("Invalid incident status transition")
                .build();
    }

    /**
     * Запись справочника инопланетян не найдена.
     */
    public static BaseException alienNotFound() {
        return BaseException.builder()
                .code(404)
                .message("alien.not_found")
                .humanMessage("Alien not found")
                .build();
    }

    /**
     * Привязка типа инопланетянина недопустима в текущем состоянии инцидента.
     */
    public static BaseException invalidAlienLink() {
        return BaseException.builder()
                .code(400)
                .message("incident.invalid_alien_link")
                .humanMessage("Cannot link alien type in current incident state")
                .build();
    }

    /**
     * Вложение инцидента не найдено.
     */
    public static BaseException attachmentNotFound() {
        return BaseException.builder()
                .code(404)
                .message("incident.attachment_not_found")
                .humanMessage("Attachment file not found")
                .build();
    }

    /**
     * Недопустимое назначение ответственного или исполнителей.
     */
    public static BaseException invalidAssignment() {
        return BaseException.builder()
                .code(400)
                .message("incident.invalid_assignment")
                .humanMessage("Cannot assign users in current incident state")
                .build();
    }

    /**
     * Пользователь не найден.
     */
    public static BaseException userNotFound() {
        return BaseException.builder()
                .code(404)
                .message("user.not_found")
                .humanMessage("User not found")
                .build();
    }

    /**
     * Пользователь не является агентом.
     */
    public static BaseException userNotAgent() {
        return BaseException.builder()
                .code(400)
                .message("user.not_agent")
                .humanMessage("User must have AGENT role")
                .build();
    }

    /**
     * Отчёт об очистке не найден.
     */
    public static BaseException cleanupReportNotFound() {
        return BaseException.builder()
                .code(404)
                .message("cleanup_report.not_found")
                .humanMessage("Cleanup report not found")
                .build();
    }

    /**
     * Отчёт об очистке уже существует для инцидента.
     */
    public static BaseException cleanupReportAlreadyExists() {
        return BaseException.builder()
                .code(409)
                .message("cleanup_report.already_exists")
                .humanMessage("Cleanup report already exists for this incident")
                .build();
    }

    /**
     * Недопустимый переход статуса очистки.
     */
    public static BaseException invalidCleanupStatusTransition() {
        return BaseException.builder()
                .code(400)
                .message("cleanup.invalid_status_transition")
                .humanMessage("Invalid cleanup status transition")
                .build();
    }

    /**
     * Операция очистки недоступна в текущем статусе инцидента.
     */
    public static BaseException cleanupNotAllowed() {
        return BaseException.builder()
                .code(400)
                .message("cleanup.not_allowed")
                .humanMessage("Cleanup operations are not allowed in current incident state")
                .build();
    }

    /**
     * Алерт мониторинга не найден.
     */
    public static BaseException monitoringAlertNotFound() {
        return BaseException.builder()
                .code(404)
                .message("monitoring_alert.not_found")
                .humanMessage("Monitoring alert not found")
                .build();
    }

    /**
     * Дубликат внешнего идентификатора события мониторинга.
     */
    public static BaseException duplicateMonitoringEvent() {
        return BaseException.builder()
                .code(409)
                .message("monitoring_alert.duplicate_external_event")
                .humanMessage("Monitoring event with this externalEventId already exists")
                .build();
    }

    /**
     * Алерт мониторинга уже связан с инцидентом или недоступен для привязки.
     */
    public static BaseException monitoringAlertNotLinkable() {
        return BaseException.builder()
                .code(400)
                .message("monitoring_alert.not_linkable")
                .humanMessage("Monitoring alert cannot be linked to a new incident")
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
