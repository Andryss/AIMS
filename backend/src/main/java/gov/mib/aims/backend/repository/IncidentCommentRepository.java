package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.IncidentCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий комментариев к инцидентам.
 */
public interface IncidentCommentRepository extends JpaRepository<IncidentCommentEntity, Long> {

    /**
     * Возвращает комментарии инцидента, старые сверху.
     *
     * @param incidentId идентификатор инцидента
     * @param pageable пагинация
     * @return страница комментариев
     */
    Page<IncidentCommentEntity> findByIncidentIdOrderByCreatedAtAscIdAsc(Long incidentId, Pageable pageable);
}
