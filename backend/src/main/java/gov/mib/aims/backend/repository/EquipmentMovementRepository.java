package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.EquipmentMovementEntity;
import gov.mib.aims.backend.model.EquipmentMovementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * Заявки на движение оборудования.
 */
public interface EquipmentMovementRepository extends JpaRepository<EquipmentMovementEntity, Long> {

    /**
     * Проверяет наличие незавершенных заявок по инциденту.
     */
    boolean existsByIncidentIdAndStatusIn(Long incidentId, Collection<EquipmentMovementStatus> statuses);

    /**
     * Возвращает страницу заявок с фильтрами.
     */
    @Query(value = """
            SELECT m.* FROM equipment_movement m
            WHERE (CAST(:incident AS bigint) IS NULL OR m.incident_id = :incident)
              AND (CAST(:responsible AS bigint) IS NULL OR m.responsible_user_id = :responsible)
              AND (CAST(:status AS text) IS NULL OR m.status = :status)
              AND (CAST(:operation AS text) IS NULL OR m.operation = :operation)
              AND (CAST(:equipment AS bigint) IS NULL OR EXISTS (
                  SELECT 1 FROM equipment_movement_item i
                  WHERE i.movement_id = m.id AND i.equipment_item_id = :equipment))
            ORDER BY m.created_at DESC, m.id DESC
            """, nativeQuery = true)
    Page<EquipmentMovementEntity> search(@Param("incident") Long incidentId,
                                         @Param("responsible") Long responsibleUserId,
                                         @Param("status") String status, @Param("operation") String operation,
                                         @Param("equipment") Long equipmentId, Pageable pageable);
}
