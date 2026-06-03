package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.EntityHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий истории изменений сущностей.
 */
public interface EntityHistoryRepository extends JpaRepository<EntityHistoryEntity, Long> {
}
