package gov.mib.aims.backend.model;

import java.time.Instant;

/**
 * Прочитанная запись аудита до преобразования в публичный ответ.
 *
 * @param <S> тип сохранённого снимка
 */
public record HistoryRecord<S>(Long id, Instant changedAt, Long changedByUserId, S snapshot) {
}
