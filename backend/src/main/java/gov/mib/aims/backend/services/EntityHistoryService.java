package gov.mib.aims.backend.services;

import gov.mib.aims.backend.model.EntityHistoryRecord;
import gov.mib.aims.backend.model.EntityType;

import java.util.List;

/**
 * Сервис записи и чтения истории изменений сущностей.
 */
public interface EntityHistoryService {

    /**
     * Фиксирует новое состояние сущности (текущий пользователь из SecurityContext).
     *
     * @param entityType тип сущности
     * @param entityId идентификатор сущности
     * @param newState объект нового состояния для JSON-снимка
     */
    void recordChange(EntityType entityType, Long entityId, Object newState);

    /**
     * Фиксирует новое состояние сущности.
     *
     * @param entityType тип сущности
     * @param entityId идентификатор сущности
     * @param newState объект нового состояния для JSON-снимка
     * @param changedByUserId идентификатор пользователя, выполнившего изменение
     */
    void recordChange(EntityType entityType, Long entityId, Object newState, Long changedByUserId);

    /**
     * Возвращает историю изменений сущности от новых к старым.
     *
     * @param entityType тип сущности
     * @param entityId идентификатор сущности
     * @return записи истории
     */
    List<EntityHistoryRecord> getHistory(EntityType entityType, Long entityId);
}
