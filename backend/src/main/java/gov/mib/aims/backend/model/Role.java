package gov.mib.aims.backend.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Роли RBAC: код совпадает с {@code role.name} в БД и значением в API.
 */
public enum Role {

    /** Оператор. */
    OPERATOR(
            "Оператор",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_CREATE,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.USER_READ,
            Permission.EQUIPMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_CREATE,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ,
            Permission.MONITORING_ALERT_READ
    ),
    /** Аналитик. */
    ANALYST(
            "Аналитик",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.INCIDENT_THREAT_CALCULATE,
            Permission.INCIDENT_ALIEN_CLASSIFY,
            Permission.INCIDENT_ALIEN_LINK,
            Permission.ALIEN_READ,
            Permission.ALIEN_WRITE,
            Permission.USER_READ,
            Permission.EQUIPMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_CREATE,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    ),
    /** Администратор. */
    ADMIN(
            "Администратор",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_CREATE,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.INCIDENT_THREAT_CALCULATE,
            Permission.INCIDENT_ALIEN_CLASSIFY,
            Permission.INCIDENT_ALIEN_LINK,
            Permission.ALIEN_READ,
            Permission.ALIEN_WRITE,
            Permission.USER_READ,
            Permission.INCIDENT_ASSIGN,
            Permission.EQUIPMENT_READ,
            Permission.EQUIPMENT_WRITE,
            Permission.EQUIPMENT_MOVEMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_CREATE,
            Permission.EQUIPMENT_MOVEMENT_PROCESS,
            Permission.TENDER_READ,
            Permission.TENDER_CREATE,
            Permission.TENDER_EDIT,
            Permission.TENDER_MANAGE,
            Permission.TENDER_RECEIVE,
            Permission.ANALYTICS_READ,
            Permission.DETENTION_READ,
            Permission.DETENTION_CREATE,
            Permission.DETENTION_EDIT,
            Permission.DETENTION_PROCESS,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    ),
    /** Оперативный агент. */
    AGENT(
            "Оперативный агент",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_STATUS_CHANGE,
            Permission.INCIDENT_COMMENT,
            Permission.ALIEN_READ,
            Permission.USER_READ,
            Permission.INCIDENT_ASSIGN,
            Permission.DETENTION_READ,
            Permission.DETENTION_CREATE,
            Permission.DETENTION_EDIT,
            Permission.EQUIPMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_CREATE,
            Permission.CLEANUP_REPORT_READ,
            Permission.FILE_UPLOAD,
            Permission.FILE_READ,
            Permission.NOTIFICATION_READ
    ),
    /** Завхоз. */
    STOREKEEPER(
            "Завхоз",
            Permission.INCIDENT_READ,
            Permission.USER_READ,
            Permission.EQUIPMENT_READ,
            Permission.EQUIPMENT_WRITE,
            Permission.EQUIPMENT_MOVEMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_CREATE,
            Permission.EQUIPMENT_MOVEMENT_PROCESS,
            Permission.TENDER_READ,
            Permission.TENDER_CREATE,
            Permission.TENDER_EDIT,
            Permission.TENDER_RECEIVE
    ),
    /** Менеджер закупок. */
    TENDER_MANAGER(
            "Менеджер закупок",
            Permission.INCIDENT_READ,
            Permission.USER_READ,
            Permission.EQUIPMENT_READ,
            Permission.TENDER_READ,
            Permission.TENDER_CREATE,
            Permission.TENDER_EDIT,
            Permission.TENDER_MANAGE,
            Permission.TENDER_RECEIVE
    ),
    /** Руководство. */
    MANAGEMENT(
            "Руководство",
            Permission.ANALYTICS_READ,
            Permission.INCIDENT_READ,
            Permission.EQUIPMENT_READ,
            Permission.EQUIPMENT_MOVEMENT_READ,
            Permission.TENDER_READ,
            Permission.DETENTION_READ,
            Permission.ALIEN_READ,
            Permission.USER_READ,
            Permission.FILE_READ
    ),
    /** Тюремный надзиратель. */
    PRISON_GUARD(
            "Тюремный надзиратель",
            Permission.INCIDENT_READ,
            Permission.ALIEN_READ,
            Permission.USER_READ,
            Permission.DETENTION_READ,
            Permission.DETENTION_EDIT,
            Permission.DETENTION_PROCESS
    ),
    /** Специалист по прикрытию. */
    CLEANER(
            "Специалист по прикрытию",
            Permission.INCIDENT_READ,
            Permission.INCIDENT_COMMENT,
            Permission.ALIEN_READ,
            Permission.USER_READ,
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
