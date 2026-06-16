package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.model.CleanupStatus;

/**
 * Ключ перехода статуса очистки (from → to); from может быть null.
 */
public record CleanupStatusTransitionKey(CleanupStatus from, CleanupStatus to) {

    /**
     * Создаёт ключ перехода.
     *
     * @param from исходный статус (null для первого перехода)
     * @param to целевой статус
     * @return ключ
     */
    public static CleanupStatusTransitionKey of(CleanupStatus from, CleanupStatus to) {
        return new CleanupStatusTransitionKey(from, to);
    }
}
