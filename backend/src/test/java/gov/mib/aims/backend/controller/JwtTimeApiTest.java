package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.config.JwtProperties;
import gov.mib.aims.backend.config.TestChronoConfig;
import gov.mib.aims.backend.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Выпуск и проверка JWT используют то же фиксированное время, что и приложение.
 */
class JwtTimeApiTest extends BaseApiTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties properties;

    @Test
    void issuesTokenAtApplicationTimeAndValidatesItWithTheSameClock() {
        String token = jwtService.generateToken("operator");
        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(TestChronoConfig.FIXED_INSTANT)).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(TestChronoConfig.FIXED_INSTANT);
        assertThat(claims.getExpiration().toInstant())
                .isEqualTo(TestChronoConfig.FIXED_INSTANT.plusMillis(properties.getExpirationMs()));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }
}
