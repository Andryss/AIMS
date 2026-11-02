package gov.mib.aims.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;

/**
 * Повторное выполнение методов при настроенных типах ошибок.
 */
@Configuration
@EnableRetry
public class RetryConfig {

    /**
     * Создает общую политику повторов для методов с @Retryable.
     *
     * @param properties настройки повторов и типов ошибок
     * @return interceptor повторного выполнения
     */
    @Bean
    public RetryOperationsInterceptor retryInterceptor(RetryProperties properties) {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(properties.getMaxRetries() + 1)
                .retryOn(properties.getRetryableExceptions())
                .traversingCauses()
                .build();

        return RetryInterceptorBuilder.stateless().retryOperations(retryTemplate).build();
    }
}
