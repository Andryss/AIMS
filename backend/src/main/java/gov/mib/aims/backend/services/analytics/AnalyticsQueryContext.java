package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.services.ChronoService;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Неизменяемые границы и масштаб одного аналитического запроса.
 */
public record AnalyticsQueryContext(AnalyticsPeriod period, AnalyticsBuckets buckets) {

    /**
     * Проверяет период и один раз выбирает масштаб отчёта.
     */
    public static AnalyticsQueryContext of(OffsetDateTime from, OffsetDateTime to, ChronoService chronoService) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);

        return new AnalyticsQueryContext(period, AnalyticsBuckets.forPeriod(period, chronoService));
    }

    /**
     * Включительная нижняя граница запроса в UTC.
     */
    public Instant from() {
        return period.from();
    }

    /**
     * Исключительная верхняя граница запроса в UTC.
     */
    public Instant to() {
        return period.to();
    }
}
