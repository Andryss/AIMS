package gov.mib.aims.backend.model;

import java.time.Instant;

/**
 * Запись истории изменения сущности (read-model).
 *
 * @param id идентификатор записи истории
 * @param entityType тип сущности
 * @param entityId идентификатор сущности
 * @param snapshot JSON нового состояния
 * @param changedByUserId кто изменил
 * @param changedAt когда изменил
 */
public record EntityHistoryRecord(
        Long id,
        EntityType entityType,
        Long entityId,
        String snapshot,
        Long changedByUserId,
        Instant changedAt
) {
}
