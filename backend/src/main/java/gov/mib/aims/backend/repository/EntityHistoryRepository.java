package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.entity.EntityHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий истории изменений сущностей.
 */
public interface EntityHistoryRepository extends JpaRepository<EntityHistoryEntity, Long> {

    /**
     * История изменений сущности, от новых к старым.
     *
     * @param entityType тип сущности
     * @param entityId идентификатор сущности
     * @return записи истории
     */
    List<EntityHistoryEntity> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            EntityType entityType,
            Long entityId
    );
}
