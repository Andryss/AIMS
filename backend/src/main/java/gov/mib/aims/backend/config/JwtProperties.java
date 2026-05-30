package gov.mib.aims.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки JWT.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs) {
}
