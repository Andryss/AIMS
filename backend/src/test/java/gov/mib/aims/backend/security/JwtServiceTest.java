package gov.mib.aims.backend.security;

import gov.mib.aims.backend.config.JwtProperties;
import gov.mib.aims.backend.services.ChronoService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Срок действия JWT проверяется по часам приложения.
 */
class JwtServiceTest {

    @ParameterizedTest
    @CsvSource({"59,true", "61,false"})
    void validatesExpirationUsingTheConfiguredClock(long elapsedSeconds, boolean valid) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-that-is-long-enough-for-hs256");
        properties.setExpirationMs(60_000);
        Instant issuedAt = Instant.parse("2025-06-01T12:00:00Z");
        var issuer = new JwtService(properties, new ChronoService(Clock.fixed(issuedAt, ZoneOffset.UTC)));
        var verifier = new JwtService(properties,
                new ChronoService(Clock.fixed(issuedAt.plusSeconds(elapsedSeconds), ZoneOffset.UTC)));
        String token = issuer.generateToken("operator");

        assertThat(verifier.isTokenValid(token)).isEqualTo(valid);
    }
}
