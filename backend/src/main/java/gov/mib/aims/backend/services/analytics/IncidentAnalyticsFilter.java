package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;

/**
 * Неизменяемые фильтры исторической аналитики инцидентов.
 */
public record IncidentAnalyticsFilter(IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {

    /**
     * Сохраняет отсутствие фильтра при передаче типа события в SQL.
     */
    public String eventTypeName() {
        return eventType == null ? null : eventType.name();
    }
}
