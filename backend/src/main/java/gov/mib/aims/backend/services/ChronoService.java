package gov.mib.aims.backend.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;

/**
 * Единые часы, точность временных меток и преобразования времени по UTC.
 */
@Service
@RequiredArgsConstructor
public class ChronoService {

    private final Clock clock;

    /**
     * Текущий момент с точностью хранения PostgreSQL.
     */
    public Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Текущее время UTC для колонок без часового пояса.
     */
    public LocalDateTime localDateTimeNow() {
        return LocalDateTime.ofInstant(now(), ZoneOffset.UTC);
    }

    /**
     * Текущее время с явным смещением UTC.
     */
    public OffsetDateTime offsetDateTimeNow() {
        return toOffsetDateTime(now());
    }

    /**
     * Представляет момент в UTC, сохраняя отсутствующее значение.
     */
    public OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    /**
     * Добавляет смещение к времени, сохранённому как UTC.
     */
    public OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC);
    }

    /**
     * Преобразует время с любым смещением в момент.
     */
    public Instant toInstant(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant();
    }

    /**
     * Нормализует входное время в UTC до удаления смещения.
     */
    public LocalDateTime toLocalDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? null : LocalDateTime.ofInstant(dateTime.toInstant(), ZoneOffset.UTC);
    }

    /**
     * Адаптирует момент для библиотек, использующих Date.
     */
    public Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    /**
     * Добавляет длительность, считая сутки как 24 часа.
     */
    public Instant plus(Instant instant, long amount, TemporalUnit unit) {
        return instant == null ? null : instant.plus(amount, unit);
    }

    /**
     * Вычитает длительность из момента.
     */
    public Instant minus(Instant instant, long amount, TemporalUnit unit) {
        return instant == null ? null : instant.minus(amount, unit);
    }

    /**
     * Обрезает момент до выбранной точности по UTC.
     */
    public Instant truncate(Instant instant, TemporalUnit unit) {
        return instant.truncatedTo(unit);
    }

    /**
     * Проверяет включительную верхнюю границу длительности между моментами.
     */
    public boolean isDurationAtMost(Instant from, Instant to, long amount, TemporalUnit unit) {
        return Duration.between(from, to).compareTo(Duration.of(amount, unit)) <= 0;
    }

    /**
     * Создаёт длительность в секундах.
     */
    public Duration seconds(long amount) {
        return Duration.ofSeconds(amount);
    }

    /**
     * Проверяет строгий порядок моментов.
     */
    public boolean isBefore(Instant first, Instant second) {
        return first.isBefore(second);
    }

    /**
     * Проверяет, что первый момент наступил позже второго.
     */
    public boolean isAfter(Instant first, Instant second) {
        return first.isAfter(second);
    }

    /**
     * Проверяет совместимость входной точности с хранением PostgreSQL.
     */
    public boolean hasMicrosecondPrecision(OffsetDateTime dateTime) {
        return dateTime.getNano() % 1000 == 0;
    }

    /**
     * Начало календарного месяца по UTC.
     */
    public Instant startOfMonth(Instant instant) {
        return YearMonth.from(toOffsetDateTime(instant)).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    /**
     * Количество календарных границ месяцев между моментами по UTC.
     */
    public long monthsBetween(Instant from, Instant to) {
        return ChronoUnit.MONTHS.between(YearMonth.from(toOffsetDateTime(from)), YearMonth.from(toOffsetDateTime(to)));
    }

    /**
     * Форматирует сохранённое время UTC по шаблону представления.
     */
    public String formatUtc(LocalDateTime dateTime, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(toOffsetDateTime(dateTime));
    }
}
