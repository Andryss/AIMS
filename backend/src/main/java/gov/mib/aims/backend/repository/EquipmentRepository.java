package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.EquipmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Складской учет отдельных экземпляров.
 */
public interface EquipmentRepository extends JpaRepository<EquipmentEntity, Long> {

    /**
     * Возвращает принятые по тендеру экземпляры для снимка истории.
     */
    @Query(value = "SELECT id FROM equipment_item WHERE source_tender_id = :tender ORDER BY id", nativeQuery = true)
    List<Long> findIdsBySourceTenderId(@Param("tender") Long tenderId);

    /**
     * Проверяет невозвращенные экземпляры по текущей выдаче для инцидента.
     */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM equipment_item e
                JOIN equipment_movement m ON m.id = e.current_issue_movement_id
                WHERE m.incident_id = :incident AND e.status = 'ISSUED')
            """, nativeQuery = true)
    boolean existsIssuedByIncidentId(@Param("incident") Long incidentId);

    /**
     * Ищет экземпляры по реквизитам и состоянию.
     */
    @Query(value = """
            SELECT * FROM equipment_item
            WHERE (strpos(lower(model), :q) > 0 OR strpos(lower(serial_number), :q) > 0)
              AND (CAST(:type AS text) IS NULL OR type = :type)
              AND (CAST(:status AS text) IS NULL OR status = :status)
              AND (CAST(:holder AS bigint) IS NULL OR holder_user_id = :holder)
            ORDER BY lower(model), lower(serial_number), id
            """, nativeQuery = true)
    Page<EquipmentEntity> search(@Param("q") String q, @Param("type") String type,
                                @Param("status") String status, @Param("holder") Long holder,
                                Pageable pageable);
}
