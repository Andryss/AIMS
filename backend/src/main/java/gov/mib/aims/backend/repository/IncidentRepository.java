package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.IncidentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий инцидентов.
 */
public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {

    /**
     * Возвращает страницу инцидентов, отсортированную по дате создания (новые первые).
     */
    Page<IncidentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
