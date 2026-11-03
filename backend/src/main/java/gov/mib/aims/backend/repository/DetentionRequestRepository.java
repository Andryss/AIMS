package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.DetentionRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Поиск заявок на заключение по данным субъекта и основанию.
 */
public interface DetentionRequestRepository extends JpaRepository<DetentionRequestEntity, Long> {

    /**
     * Ищет заявки по буквальной подстроке, инциденту и статусу.
     */
    @Query(value = """
            SELECT * FROM detention_request
            WHERE (strpos(lower(subject_name), :q) > 0 OR strpos(lower(subject_description), :q) > 0)
              AND (CAST(:incident AS bigint) IS NULL OR incident_id = :incident)
              AND (CAST(:status AS text) IS NULL OR status = :status)
            ORDER BY created_at DESC, id DESC
            """, nativeQuery = true)
    Page<DetentionRequestEntity> search(@Param("q") String q, @Param("incident") Long incidentId,
                                        @Param("status") String status, Pageable pageable);
}
