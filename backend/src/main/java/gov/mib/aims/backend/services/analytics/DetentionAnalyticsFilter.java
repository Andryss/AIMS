package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;

/**
 * Неизменяемые фильтры исторической аналитики заключений.
 */
public record DetentionAnalyticsFilter(Long alienId, DetentionRequestStatusApi status, Long incidentId) {

    /**
     * Сохраняет отсутствие фильтра при передаче статуса в SQL.
     */
    public String statusName() {
        return status == null ? null : status.name();
    }
}
