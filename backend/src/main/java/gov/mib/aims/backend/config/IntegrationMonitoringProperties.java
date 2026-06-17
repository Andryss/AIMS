package gov.mib.aims.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки inbound-интеграции с внешней системой мониторинга.
 */
@Component
@ConfigurationProperties(prefix = "aims.integration.monitoring")
@Getter
@Setter
public class IntegrationMonitoringProperties {

    private String apiKey;
}
