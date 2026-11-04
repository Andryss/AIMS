package gov.mib.aims.backend.services.transaction;

import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Сериализуемая команда с общей политикой повторов при конфликте.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Retryable(interceptor = "retryInterceptor")
@Transactional(isolation = Isolation.SERIALIZABLE)
public @interface RetryableSerializable {
}
