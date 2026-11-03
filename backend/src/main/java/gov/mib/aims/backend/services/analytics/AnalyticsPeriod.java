package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.AnalyticsMeta;
import gov.mib.aims.backend.services.ChronoService;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Полуоткрытый интервал аналитики в UTC.
 */
public record AnalyticsPeriod(Instant from, Instant to) {

    /**
     * Проверяет обязательные границы и запрещает прогноз будущего состояния.
     */
    public static AnalyticsPeriod of(OffsetDateTime from, OffsetDateTime to, ChronoService chronoService) {
        if (from == null || to == null
                || !chronoService.isBefore(chronoService.toInstant(from), chronoService.toInstant(to))) {
            throw Errors.validationError("Укажите начало периода раньше его окончания");
        }
        if (!chronoService.hasMicrosecondPrecision(from) || !chronoService.hasMicrosecondPrecision(to)) {
            throw Errors.validationError("Точность границ периода не должна превышать микросекунды");
        }
        if (chronoService.isAfter(chronoService.toInstant(to), chronoService.now())) {
            throw Errors.validationError("Окончание периода не может быть в будущем");
        }

        return new AnalyticsPeriod(chronoService.toInstant(from), chronoService.toInstant(to));
    }

    /**
     * Заполняет общую информацию отчёта, сохраняя показатель полноты истории.
     */
    public AnalyticsMeta meta(AnalyticsBuckets buckets, Long missing, ChronoService chronoService) {
        return new AnalyticsMeta().from(chronoService.toOffsetDateTime(from)).to(chronoService.toOffsetDateTime(to))
                .generatedAt(chronoService.offsetDateTimeNow())
                .bucketUnit(buckets.unit()).bucketStep(buckets.step()).recordsWithoutSnapshot(missing);
    }
}
