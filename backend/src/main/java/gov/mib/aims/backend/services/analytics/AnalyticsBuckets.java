package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.AnalyticsBucketUnitApi;
import gov.mib.aims.backend.services.ChronoService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Автоматический масштаб графика с ограниченным числом точек.
 */
public record AnalyticsBuckets(AnalyticsBucketUnitApi unit, int step, Instant start, int months, int seconds) {

    private static final int MAX_BUCKETS = 120;

    /**
     * Выбирает часовые, дневные или календарные месячные границы по UTC.
     */
    public static AnalyticsBuckets forPeriod(AnalyticsPeriod period, ChronoService chronoService) {
        if (chronoService.isDurationAtMost(period.from(), period.to(), 48, ChronoUnit.HOURS)) {
            return new AnalyticsBuckets(AnalyticsBucketUnitApi.HOUR, 1,
                    chronoService.truncate(period.from(), ChronoUnit.HOURS), 0, 3600);
        }
        if (chronoService.isDurationAtMost(period.from(), period.to(), 90, ChronoUnit.DAYS)) {
            return new AnalyticsBuckets(AnalyticsBucketUnitApi.DAY, 1,
                    chronoService.truncate(period.from(), ChronoUnit.DAYS), 0, 86400);
        }

        Instant first = chronoService.startOfMonth(period.from());
        Instant last = chronoService.minus(period.to(), 1, ChronoUnit.NANOS);
        long count = chronoService.monthsBetween(first, last) + 1;
        int step = Math.toIntExact((count + MAX_BUCKETS - 1) / MAX_BUCKETS);

        return new AnalyticsBuckets(AnalyticsBucketUnitApi.MONTH, step,
                first, step, 0);
    }
}
