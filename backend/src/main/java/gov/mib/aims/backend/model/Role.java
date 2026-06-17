package gov.mib.aims.backend.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Роли RBAC: код совпадает с {@code role.name} в БД и значением в API.
 */
public enum Role {

    OPERATOR(
            "Оператор",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_CREATE,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.USER_READ,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    ),
    ANALYST(
            "Аналитик",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.INCIDENT_ALIEN_LINK,
            Permission.ALIEN_READ,
            Permission.USER_READ,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    ),
    ADMIN(
            "Администратор",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_CREATE,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.INCIDENT_ALIEN_LINK,
            Permission.ALIEN_READ,
            Permission.USER_READ,
            Permission.INCIDENT_ASSIGN,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    ),
    AGENT(
            "Оперативный агент",
            Permission.INCIDENT_READ,
            Permission.USER_READ,
            Permission.INCIDENT_ASSIGN,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.ALIEN_READ,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    ),
    CLEANER(
            "Специалист по прикрытию",
            Permission.INCIDENT_READ,
            Permission.USER_READ,
            Permission.INCIDENT_COMMENT,
            Permission.ALIEN_READ,
            Permission.CLEANUP_REPORT_READ,
            Permission.CLEANUP_REPORT_CREATE,
            Permission.CLEANUP_STATUS_CHANGE,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    );

    private final String description;
    private final Set<Permission> permissions;

    Role(String description, Permission... permissions) {
        this.description = description;
        this.permissions = EnumSet.copyOf(Arrays.asList(permissions));
    }

    public String getCode() {
        return name();
    }

    public String getDescription() {
        return description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
