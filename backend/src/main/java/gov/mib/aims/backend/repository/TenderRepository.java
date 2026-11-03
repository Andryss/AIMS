package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.TenderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Поиск тендеров на закупку оборудования.
 */
public interface TenderRepository extends JpaRepository<TenderEntity, Long> {

    /**
     * Ищет тендеры по условиям закупки и точным фильтрам.
     */
    @Query(value = """
            SELECT * FROM tender
            WHERE strpos(lower(coalesce(conditions, '')), :q) > 0
              AND (CAST(:status AS text) IS NULL OR status = :status)
              AND (CAST(:type AS text) IS NULL OR equipment_type = :type)
              AND (CAST(:incident AS bigint) IS NULL OR incident_id = :incident)
            ORDER BY created_at DESC, id DESC
            """, nativeQuery = true)
    Page<TenderEntity> search(@Param("q") String q, @Param("status") String status,
                             @Param("type") String type, @Param("incident") Long incidentId, Pageable pageable);
}
