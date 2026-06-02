package gov.mib.aims.backend.model;

import java.time.Instant;
import java.util.List;

/**
 * Уведомление (read-model).
 *
 * @param id идентификатор уведомления
 * @param message текст
 * @param relatedEntities ссылки на связанные сущности
 * @param read прочитано
 * @param readAt время прочтения
 * @param createdAt время создания
 */
public record NotificationRecord(
        Long id,
        String message,
        List<String> relatedEntities,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
