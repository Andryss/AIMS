package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.services.ChronoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Пороговые значения масштаба и календарные границы остаются общими для отчётов.
 */
class AnalyticsQueryContextTest {

    private final ChronoService chronoService = new ChronoService(Clock.fixed(
            Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC));

    @ParameterizedTest
    @CsvSource({
            "2025-01-03T00:00:00Z,HOUR,3600",
            "2025-01-03T00:00:00.000001Z,DAY,86400",
            "2025-04-01T00:00:00Z,DAY,86400",
            "2025-04-01T00:00:00.000001Z,MONTH,0"
    })
    void selectsBucketsAtInclusiveDurationLimits(String to, AnalyticsBuckets.Unit unit, int seconds) {
        AnalyticsQueryContext query = AnalyticsQueryContext.of(OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                OffsetDateTime.parse(to), chronoService);

        assertThat(query.buckets().unit()).isEqualTo(unit);
        assertThat(query.buckets().seconds()).isEqualTo(seconds);
        assertThat(query.buckets().step()).isEqualTo(1);
    }

    @Test
    void capsLongReportsUsingWholeUtcMonths() {
        AnalyticsQueryContext query = AnalyticsQueryContext.of(OffsetDateTime.parse("2000-01-15T03:00:00+03:00"),
                OffsetDateTime.parse("2025-01-01T03:00:00+03:00"), chronoService);

        assertThat(query.buckets().start()).isEqualTo(Instant.parse("2000-01-01T00:00:00Z"));
        assertThat(query.buckets().unit()).isEqualTo(AnalyticsBuckets.Unit.MONTH);
        assertThat(query.buckets().step()).isEqualTo(3);
        assertThat(query.buckets().months()).isEqualTo(3);
        assertThat(query.buckets().seconds()).isZero();
    }
}
