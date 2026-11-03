package gov.mib.aims.backend.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Единая точность хранения и UTC независимо от часового пояса часов или входных данных.
 */
class ChronoServiceTest {

    private static final Instant NOW = Instant.parse("2025-06-01T12:00:00.123456789Z");

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Europe/Kirov", "America/New_York"})
    void usesUtcAndMicrosecondsRegardlessOfClockZone(String zone) {
        var chrono = new ChronoService(Clock.fixed(NOW, ZoneId.of(zone)));

        assertThat(chrono.now()).isEqualTo(Instant.parse("2025-06-01T12:00:00.123456Z"));
        assertThat(chrono.localDateTimeNow()).isEqualTo(LocalDateTime.parse("2025-06-01T12:00:00.123456"));
        assertThat(chrono.offsetDateTimeNow()).isEqualTo(OffsetDateTime.parse("2025-06-01T12:00:00.123456Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-06-01T15:00:00+03:00", "2025-06-01T08:00:00-04:00"})
    void normalizesOffsetsBeforeStoringLocalDateTime(String timestamp) {
        var chrono = new ChronoService(Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(chrono.toLocalDateTime(OffsetDateTime.parse(timestamp)))
                .isEqualTo(LocalDateTime.parse("2025-06-01T12:00:00"));
    }

    @Test
    void addsExactlyTwentyFourHoursAcrossDst() {
        var chrono = new ChronoService(Clock.fixed(NOW, ZoneId.of("Europe/Kirov")));

        assertThat(chrono.plus(Instant.parse("2000-03-25T12:00:00Z"), 1, ChronoUnit.DAYS))
                .isEqualTo(Instant.parse("2000-03-26T12:00:00Z"));
    }

    @Test
    void preservesMissingOptionalDates() {
        var chrono = new ChronoService(Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(chrono.toOffsetDateTime((Instant) null)).isNull();
        assertThat(chrono.toOffsetDateTime((LocalDateTime) null)).isNull();
        assertThat(chrono.toLocalDateTime(null)).isNull();
        assertThat(chrono.plus(null, 30, ChronoUnit.DAYS)).isNull();
    }
}
