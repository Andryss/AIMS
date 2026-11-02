package gov.mib.aims.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Общие настройки повторного выполнения методов.
 */
@Component
@ConfigurationProperties(prefix = "aims.retry")
@Getter
@Setter
public class RetryProperties {

    private int maxRetries;
    private List<Class<? extends Throwable>> retryableExceptions = List.of();
}
