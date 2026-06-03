package gov.mib.aims.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Фиксированные часы для интеграционных и unit-тестов.
 */
@Configuration
@Profile("test")
public class TestClockConfig {

    /**
     * Фиксированный момент времени в UTC (совпадает с типичными датами в тестах API).
     */
    public static final Instant FIXED_INSTANT = Instant.parse("2025-06-01T12:00:00Z");

    /**
     * {@link #FIXED_INSTANT} как {@link LocalDateTime} в UTC.
     */
    public static final LocalDateTime FIXED_LOCAL_DATE_TIME = LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

    /**
     * Часы, зафиксированные на {@link #FIXED_INSTANT}.
     *
     * @return фиксированные часы UTC
     */
    @Bean
    Clock clock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
