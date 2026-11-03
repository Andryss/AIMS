package gov.mib.aims.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/**
 * Системные часы для приложения и локальной разработки.
 */
@Configuration
@Profile("!test")
public class ChronoConfig {

    /**
     * Предоставляет системные часы UTC для сервиса времени.
     *
     * @return системные часы UTC
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
