package gov.mib.aims.backend.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Запрещает запуск без {@code JWT_SECRET} вне dev/test профилей.
 */
@Component
public class JwtSecretValidator {

    private final Environment environment;
    private final JwtProperties jwtProperties;

    public JwtSecretValidator(Environment environment, JwtProperties jwtProperties) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Проверяет наличие JWT-секрета при старте приложения.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtSecret() {
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            return;
        }
        if (jwtProperties.getSecret() == null || jwtProperties.getSecret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be set for non-dev profiles");
        }
    }
}
