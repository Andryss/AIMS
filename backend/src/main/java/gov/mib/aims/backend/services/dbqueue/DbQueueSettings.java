package gov.mib.aims.backend.services.dbqueue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ru.yoomoney.tech.dbqueue.settings.FailRetryType;
import ru.yoomoney.tech.dbqueue.settings.ProcessingMode;
import ru.yoomoney.tech.dbqueue.settings.ReenqueueRetryType;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbQueueSettings {

    /**
     * Название очереди (UPPER_CASE).
     */
    String value();

    ProcessingMode processingMode() default ProcessingMode.SEPARATE_TRANSACTIONS;

    int threadCount() default 1;

    int betweenTasksTimeout() default 0;

    int noTaskTimeout() default 10;

    int fatalCrashTimeout() default 1;

    FailRetryType failRetryType() default FailRetryType.GEOMETRIC_BACKOFF;

    int failInitialDelay() default 60;

    ReenqueueRetryType retryType() default ReenqueueRetryType.MANUAL;

    int[] sequentialPlan() default {60};

    int fixedDelay() default 60;

    int initialDelay() default 60;

    int arithmeticStep() default 60;

    long geometricRatio() default 2;
}
