package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.model.IncidentStatus;

/**
 * Ключ перехода статуса (from → to).
 */
public record StatusTransitionKey(IncidentStatus from, IncidentStatus to) {

    /**
     * Создаёт ключ перехода.
     *
     * @param from исходный статус
     * @param to целевой статус
     * @return ключ
     */
    public static StatusTransitionKey of(IncidentStatus from, IncidentStatus to) {
        return new StatusTransitionKey(from, to);
    }
}
