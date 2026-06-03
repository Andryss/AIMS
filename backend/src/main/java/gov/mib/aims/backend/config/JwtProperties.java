package gov.mib.aims.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Настройки JWT.
 */
@Configuration
@Data
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Секрет для подписи токенов (HS256, не короче 32 символов).
     */
    @NotBlank
    @Size(min = 32)
    private String secret;

    /**
     * Время жизни access token в миллисекундах.
     */
    @Positive
    private long expirationMs;
}
