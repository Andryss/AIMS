package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.EntityHistoryEntity;
import gov.mib.aims.backend.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий истории изменений сущностей.
 */
public interface EntityHistoryRepository extends JpaRepository<EntityHistoryEntity, Long> {

    /**
     * Возвращает историю изменений сущности в хронологическом порядке.
     *
     * @param entityType тип сущности
     * @param entityId идентификатор сущности
     * @param pageable пагинация
     * @return страница записей истории
     */
    Page<EntityHistoryEntity> findByEntityTypeAndEntityIdOrderByChangedAtAsc(
            EntityType entityType,
            Long entityId,
            Pageable pageable
    );
}
