package gov.mib.aims.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Настройки инициализации демо-данных.
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "aims.demo-data")
public class DemoDataProperties {

    /**
     * Включить сид RBAC и демо-пользователей при старте приложения.
     */
    private boolean enabled;
}
