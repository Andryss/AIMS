package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.services.ChronoService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Выполнение SQL сохраняет исходный результат и проверяет период до обращения к хранилищу.
 */
class AnalyticsReportExecutorTest {

    private final ChronoService chronoService = new ChronoService(Clock.fixed(
            Instant.parse("2025-06-01T12:00:00.123456789Z"), ZoneId.of("Europe/Kirov")));
    private final AnalyticsReportExecutor executor = new AnalyticsReportExecutor(chronoService);

    @Test
    void returnsRawSqlResultAlongsideValidatedUtcContext() {
        String json = "opaque SQL result, decoded only by the response mapper";
        AnalyticsReport report = executor.execute(
                OffsetDateTime.parse("2025-05-01T03:00:00+03:00"),
                OffsetDateTime.parse("2025-06-01T03:00:00+03:00"), query -> {
                    assertThat(query.from()).isEqualTo(Instant.parse("2025-05-01T00:00:00Z"));
                    assertThat(query.to()).isEqualTo(Instant.parse("2025-06-01T00:00:00Z"));
                    assertThat(query.buckets().start()).isEqualTo(Instant.parse("2025-05-01T00:00:00Z"));

                    return json;
                });

        assertThat(report.json()).isEqualTo(json);
        assertThat(report.context().from()).isEqualTo(Instant.parse("2025-05-01T00:00:00Z"));
        assertThat(report.context().to()).isEqualTo(Instant.parse("2025-06-01T00:00:00Z"));
    }

    @Test
    void rejectsInvalidPeriodBeforeRunningQuery() {
        OffsetDateTime boundary = OffsetDateTime.parse("2025-05-01T00:00:00Z");

        assertThatThrownBy(() -> executor.execute(boundary, boundary, query -> {
            throw new AssertionError("Invalid periods must not execute a query");
        })).isInstanceOf(BaseException.class);
    }
}
