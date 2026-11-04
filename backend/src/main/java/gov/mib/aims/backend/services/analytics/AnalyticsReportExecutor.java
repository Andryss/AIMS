package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.function.Function;

/**
 * Проверка периода и выполнение аналитического запроса без сборки API-представления.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsReportExecutor {

    private final ChronoService chronoService;

    /**
     * Выполняет SQL в транзакции вызывающего сервиса и возвращает исходный результат с контекстом.
     */
    public AnalyticsReport execute(OffsetDateTime from, OffsetDateTime to,
            Function<AnalyticsQueryContext, String> query) {
        AnalyticsQueryContext context = AnalyticsQueryContext.of(from, to, chronoService);

        return new AnalyticsReport(query.apply(context), context);
    }
}
