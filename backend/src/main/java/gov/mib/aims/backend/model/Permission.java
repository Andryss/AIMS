package gov.mib.aims.backend.model;

/**
 * Разрешения RBAC: код совпадает с {@code permission.code} в БД и значением в JWT authorities.
 */
public enum Permission {

    INCIDENT_READ("Просмотр карточек и списка инцидентов"),
    INCIDENT_CREATE("Создание инцидентов"),
    INCIDENT_STATUS_CHANGE("Смена статуса инцидента"),
    INCIDENT_COMMENT("Добавление комментариев к инциденту"),
    INCIDENT_ALIEN_LINK("Привязка типа инопланетянина к инциденту"),
    ALIEN_READ("Просмотр и поиск в справочнике инопланетян"),
    USER_READ("Поиск пользователей и пакетное чтение профилей"),
    INCIDENT_ASSIGN("Назначение ответственного и исполнителей на инцидент"),
    CLEANUP_REPORT_READ("Просмотр отчёта об очистке"),
    CLEANUP_REPORT_CREATE("Создание отчёта об очистке"),
    CLEANUP_STATUS_CHANGE("Смена статуса очистки"),
    FILE_UPLOAD("Загрузка файлов"),
    FILE_READ("Скачивание файлов"),
    NOTIFICATION_READ("Просмотр уведомлений"),
    MONITORING_ALERT_READ("Просмотр алертов внешнего мониторинга");

    private final String description;

    Permission(String description) {
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }
}
