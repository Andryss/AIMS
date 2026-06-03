package gov.mib.aims.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/**
 * Системные часы для production и локальной разработки.
 */
@Configuration
@Profile("!test")
public class ClockConfig {

    /**
     * Часы по UTC (для {@link java.time.LocalDateTime#now(Clock)} и {@link java.time.Instant#now(Clock)}).
     *
     * @return системные часы UTC
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
