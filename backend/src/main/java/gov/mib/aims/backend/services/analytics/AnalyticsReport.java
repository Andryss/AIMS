package gov.mib.aims.backend.services.analytics;

/**
 * Исходный JSON аналитического запроса и проверенные параметры его выполнения.
 */
public record AnalyticsReport(String json, AnalyticsQueryContext context) {
}
